package com.sxcccccccc.iudebug.recipecheck;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseFluidMachineRecipe;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.IInput;
import com.denfop.api.recipe.IInputFluid;
import com.denfop.api.recipe.IRecipes;
import com.denfop.api.recipe.RecipeOutput;
import com.denfop.api.recipe.RecipesFluidCore;
import com.denfop.recipe.IInputItemStack;
import com.denfop.recipe.InputItemStack;
import com.sxcccccccc.iudebug.config.IudebugConfig;
import dev.latvian.mods.kubejs.server.KubeJSReloadListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 功能2：配方不完整检查崩溃（规格 iudebug_spec.md + v1.2 用户裁决）。
 *
 * <p><b>触发时机（v1.2，照抄 kjsexportcmd-1.0.0 触发机制）</b>：
 * {@code @SubscribeEvent onTagsUpdated(TagsUpdatedEvent)} ——
 * {@code KubeJSReloadListener.resources} 拿 {@link ReloadableServerResources} →
 * {@code getRecipeManager()} → 反射读 {@code RecipeManager} 内部 recipes 字段
 * （SRG 名 {@code f_44007_}）→ 检查 map 类型：仍是<b>可变 HashMap</b> = 服务端数据
 * 加载时机（执行）；已冻结为 ImmutableMap = 客户端同步事件（跳过，只打日志）。
 * 服务端时机到达就<b>一次性执行检查</b>：发现不完整配方立即在服务器线程抛
 * {@link IllegalStateException}（消息 = 完整清单）崩掉游戏；无任何预检/复检/兜底。
 *
 * <p>防误炸说明（v1.2 已移除连续一致设计）：包内 KubeJS 异步 tag 竞态已被
 * ModernFix 修好（2026-08-19 实测生效），kjsexportcmd 的 map 类型判断已天然区分
 * 服务端/客户端时机，故不再需要预检+复检+tick 兜底那套自创逻辑（用户裁决删除）。
 * /reload 会再次触发 SERVER_DATA_LOAD 时机 → 每次服务端数据加载都检查，
 * reload 后再次崩溃是期望行为（自动化调试主循环）。
 *
 * <p>三条检查线：
 * <ul>
 *   <li>线 A 物品机：{@link IRecipes#getMap_recipe_managers()} 遍历机器名，
 *       {@link IRecipes#getRecipeList(String)} 取配方，按 D1-D6 判坏；</li>
 *   <li>线 A 流体机：{@link RecipesFluidCore#map_recipes_fluid} 按 D7-D9/D5 判坏；</li>
 *   <li>线 B 原生：{@link RecipeManager#getRecipes()} 遍历全部原生配方，按 V1-V3 判坏。</li>
 * </ul>
 *
 * <p>默认全关（{@code recipecheck.enabled=false}），任何不确定的判定默认不报，
 * 宁可漏报不可误炸。崩溃方式为服务器线程抛异常：单机集成服务器 = 整局退出，
 * crash 报告 "Exception Message" 原样包含完整清单（先 LOGGER.error 双保险，日志全量）。
 */
@Mod.EventBusSubscriber(modid = "iudebug", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RecipeCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger("iudebug");

    private RecipeCheck() {
    }

    /**
     * 触发（照抄 kjsexportcmd-1.0.0 的 onTagsUpdated）：TagsUpdatedEvent 每次触发都进来，
     * 反射查 RecipeManager 内部 recipes map 类型 —— 可变 HashMap = 服务端数据加载时机
     * （启动 1 次 + 每次 /reload 1 次），此时执行一次检查；冻结的 ImmutableMap = 客户端
     * tag 同步事件，跳过。整个方法 try/catch Throwable 包住（照抄 kjsexportcmd），
     * 检查自身异常只打日志不崩。
     */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (!IudebugConfig.RECIPE_CHECK_ENABLED.get()) {
            return;
        }
        try {
            ReloadableServerResources rsrc = KubeJSReloadListener.resources;
            if (rsrc == null) {
                return;
            }
            RecipeManager rm = rsrc.getRecipeManager();
            Field f = RecipeManager.class.getDeclaredField("f_44007_");
            f.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) f.get(rm);
            String mapType = map.getClass().getName();
            if (!mapType.contains("HashMap")) {
                LOGGER.info("[iudebug][recipecheck] recipes map 已冻结（{}），跳过（客户端同步事件）", mapType);
                return;
            }
            // 服务端数据加载时机：一次性检查，发现不完整配方立即崩
            List<String[]> broken = runAllChecks(rm, event.getRegistryAccess(),
                    IudebugConfig.RECIPE_CHECK_INCLUDE_WARN.get());
            if (!broken.isEmpty()) {
                crashWithList(broken);
            }
        } catch (Throwable t) {
            LOGGER.error("[iudebug][recipecheck] 检查过程异常", t);
        }
    }

    /** 全部检查线汇总 + ignoredRecipeIds 前缀过滤。 */
    static List<String[]> runAllChecks(RecipeManager rm, RegistryAccess ra, boolean includeWarn) {
        List<String[]> broken = new ArrayList<>();
        if (IudebugConfig.RECIPE_CHECK_INCLUDE_DENFOP.get()) {
            broken.addAll(scanDenfop(includeWarn));
        }
        if (IudebugConfig.RECIPE_CHECK_INCLUDE_VANILLA.get()) {
            broken.addAll(scanVanilla(rm, ra, IudebugConfig.RECIPE_CHECK_INCLUDE_VANILLA_SPECIAL.get()));
        }
        List<? extends String> ignores = IudebugConfig.RECIPE_CHECK_IGNORED_RECIPE_IDS.get();
        if (ignores != null && !ignores.isEmpty()) {
            List<String[]> filtered = new ArrayList<>();
            for (String[] row : broken) {
                String id = row[0];
                String machine = id.startsWith("denfop-fluid/") ? id.substring("denfop-fluid/".length())
                        : id.startsWith("denfop/") ? id.substring("denfop/".length())
                        : id; // 原生线：id 即配方 id
                boolean ignored = false;
                for (String p : ignores) {
                    if (p != null && !p.isEmpty() && machine.startsWith(p)) {
                        ignored = true;
                        break;
                    }
                }
                if (!ignored) {
                    filtered.add(row);
                }
            }
            broken = filtered;
        }
        return broken;
    }

    // ------------------------------------------------------------------
    // 崩溃
    // ------------------------------------------------------------------

    static void crashWithList(List<String[]> broken) {
        StringBuilder sb = new StringBuilder("iudebug: 检测到 ")
                .append(broken.size()).append(" 条不完整配方：\n");
        for (String[] b : broken) {
            sb.append("  - [").append(b[0]).append("] ").append(b[1]).append(" :: ").append(b[2]).append('\n');
        }
        LOGGER.error(sb.toString()); // latest.log 双保险（崩溃报告消息若被截断，日志是全量）
        throw new IllegalStateException(sb.toString());
    }

    // ------------------------------------------------------------------
    // 线 A：Denfop 物品机 + 流体机（规格 §3.1 照抄）
    // ------------------------------------------------------------------

    /**
     * 返回 [机器名, 配方描述, 问题描述] 的坏配方列表。
     * 判定标准：D1 输入空气 / D2 tag 当前为空 / D3 输入槽列表为空 / D4 伴生流体为空 /
     * D5 输出含空气 / D6 输出列表空（WARN，由 includeWarn 控制）/ D7 流体输出空 /
     * D8 流体输入空 / D9 流体配方的物品输入空。
     */
    public static List<String[]> scanDenfop(boolean includeWarn) {
        List<String[]> broken = new ArrayList<>();
        IRecipes recipes = Recipes.recipes; // static 字段；IU 加载失败时为 null（判空）
        if (recipes == null) {
            return broken;
        }

        // ---- 物品机 ----
        for (String machine : recipes.getMap_recipe_managers()) { // key = map_recipe_managers.keySet()
            for (BaseMachineRecipe r : recipes.getRecipeList(machine)) { // map_recipes.getOrDefault(name, empty)
                List<String> probs = new ArrayList<>();
                IInput in = r.input;
                if (in != null) {
                    List<IInputItemStack> ins = in.getInputs();
                    if (ins == null || ins.isEmpty()) {
                        probs.add("输入槽列表为空 (D3)");
                    } else {
                        for (int i = 0; i < ins.size(); i++) {
                            IInputItemStack ii = ins.get(i);
                            if (ii instanceof InputItemStack iis && iis.input.isEmpty()) {
                                probs.add("输入槽" + i + " 引用未注册物品=空气 (D1)");
                            } else if (ii.hasTag() && (ii.getInputs() == null || ii.getInputs().isEmpty())) {
                                probs.add("输入槽" + i + " tag 当前为空 (D2): " + ii.getTag().location());
                            }
                        }
                    }
                    if (in.hasFluids() && (in.getFluid() == null || in.getFluid().isEmpty())) {
                        probs.add("伴生流体输入为空 (D4)");
                    }
                }
                RecipeOutput out = r.output;
                if (out != null && out.items != null) {
                    for (ItemStack s : out.items) {
                        if (s == null || s.isEmpty()) {
                            probs.add("物品输出含空气 (D5)");
                        }
                    }
                } else if (out == null || out.items == null || out.items.isEmpty()) {
                    if (includeWarn) {
                        probs.add("物品输出列表为空 (D6, WARN)");
                    }
                }
                if (!probs.isEmpty()) {
                    broken.add(new String[]{"denfop/" + machine, describe(r), String.join("; ", probs)});
                }
            }
        }

        // ---- 流体机 ----
        RecipesFluidCore fc = recipes.getRecipeFluid(); // IRecipes.getRecipeFluid()
        for (String machine : fc.map_recipes_fluid.keySet()) {
            for (BaseFluidMachineRecipe r : fc.getRecipeList(machine)) {
                List<String> probs = new ArrayList<>();
                IInputFluid in = r.getInput();
                if (in != null) {
                    for (FluidStack fs : in.getInputs()) {
                        if (fs == null || fs.isEmpty()) {
                            probs.add("流体输入为空 (D8)");
                        }
                    }
                    IInputItemStack st = in.getStack();
                    if (st != null && (st.getInputs() == null || st.getInputs().isEmpty())) {
                        probs.add("流体配方的物品输入空 (D9)");
                    }
                }
                for (FluidStack fs : r.getOutput_fluid()) {
                    if (fs == null || fs.isEmpty()) {
                        probs.add("流体输出含空流体 (D7)");
                    }
                }
                RecipeOutput out = r.getOutput();
                if (out != null && out.items != null) {
                    for (ItemStack s : out.items) {
                        if (s == null || s.isEmpty()) {
                            probs.add("物品输出含空气 (D5)");
                        }
                    }
                } else if (out != null && (out.items == null || out.items.isEmpty())) {
                    if (includeWarn) {
                        probs.add("物品输出列表为空 (D6, WARN)");
                    }
                }
                if (!probs.isEmpty()) {
                    broken.add(new String[]{"denfop-fluid/" + machine, describeFluid(r), String.join("; ", probs)});
                }
            }
        }
        return broken;
    }

    /** 配方无 id，用输入输出描述（规格 §3.1）。 */
    static String describe(BaseMachineRecipe r) {
        StringBuilder sb = new StringBuilder("in[");
        IInput in = r.input;
        if (in != null) {
            for (IInputItemStack ii : in.getInputs()) {
                for (ItemStack s : ii.getInputs()) {
                    sb.append(key(s)).append('x').append(ii.getAmount()).append(' ');
                }
            }
            if (in.hasFluids()) {
                sb.append("+fluid:").append(ForgeRegistries.FLUIDS.getKey(in.getFluid().getFluid())).append(' ');
            }
        }
        sb.append("] out[");
        if (r.output != null && r.output.items != null) {
            for (ItemStack s : r.output.items) {
                sb.append(key(s)).append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /** 流体配方描述（in[流体+物品] out[流体+物品]）。 */
    static String describeFluid(BaseFluidMachineRecipe r) {
        StringBuilder sb = new StringBuilder("in[");
        IInputFluid in = r.getInput();
        if (in != null) {
            for (FluidStack fs : in.getInputs()) {
                if (fs != null) {
                    sb.append("fluid:").append(ForgeRegistries.FLUIDS.getKey(fs.getFluid())).append(' ');
                }
            }
            IInputItemStack st = in.getStack();
            if (st != null) {
                for (ItemStack s : st.getInputs()) {
                    sb.append(key(s)).append(' ');
                }
            }
        }
        sb.append("] out[");
        for (FluidStack fs : r.getOutput_fluid()) {
            if (fs != null) {
                sb.append("fluid:").append(ForgeRegistries.FLUIDS.getKey(fs.getFluid())).append(' ');
            }
        }
        RecipeOutput out = r.getOutput();
        if (out != null && out.items != null) {
            for (ItemStack s : out.items) {
                sb.append(key(s)).append(' ');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /** 物品描述：空气显示 AIR，否则注册名 x 数量。 */
    static String key(ItemStack s) {
        return s.isEmpty() ? "AIR"
                : ForgeRegistries.ITEMS.getKey(s.getItem()) + " x" + s.getCount();
    }

    // ------------------------------------------------------------------
    // 线 B：原生 RecipeManager（规格 §3.2 照抄；入参改为 RecipeManager+RegistryAccess，
    // 不再依赖 MinecraftServer —— 与 kjsexportcmd 的触发方式一致）
    // ------------------------------------------------------------------

    public static List<String[]> scanVanilla(RecipeManager rm, RegistryAccess ra, boolean includeSpecial) {
        List<String[]> broken = new ArrayList<>();
        for (Recipe<?> rec : rm.getRecipes()) { // Collection<Recipe<?>>，reload 后为 ImmutableMap 只读
            if (!includeSpecial && rec.isSpecial()) {
                continue;
            }
            if (rec.getSerializer() != null
                    && rec.getSerializer().toString().contains("universal_recipe")) {
                continue; // IU 数据包壳，走线 A
            }
            List<String> probs = new ArrayList<>();
            for (Ingredient ing : rec.getIngredients()) {
                ItemStack[] items = ing.getItems(); // 空 tag/未知物品 → 空数组
                if (items.length == 0) {
                    probs.add("空配料 (V1): " + ing.toJson());
                } else {
                    for (ItemStack s : items) {
                        if (s.isEmpty()) {
                            probs.add("配料含空气 (V2): " + ing.toJson());
                            break;
                        }
                    }
                }
            }
            if (rec.getResultItem(ra).isEmpty()) {
                probs.add("输出空气 (V3)");
            }
            if (!probs.isEmpty()) {
                broken.add(new String[]{rec.getId().toString(), "vanilla", String.join("; ", probs)});
            }
        }
        return broken;
    }
}
