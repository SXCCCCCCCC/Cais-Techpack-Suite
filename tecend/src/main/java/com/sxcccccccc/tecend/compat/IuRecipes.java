package com.sxcccccccc.tecend.compat;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.Input;
import com.denfop.api.recipe.RecipeOutput;
import com.denfop.blockentity.mechanism.BlockEntityFluidIntegrator;
import com.denfop.blockentity.mechanism.BlockEntityGeneticStabilize;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * IU 机器配方注入 —— ② 粉碎（IU 磨粉机）。
 *
 * <p><b>为什么这里不用 mixin</b>：IU 虽然把自己的配方写死在代码里（jar 内
 * data/industrialupgrade/recipes/ 只有原版 blasting/furnace/crafting 三类，数据包塞不进去），
 * 但它同时开放了一套公开的运行时配方 API：
 * <ul>
 *   <li>{@code Recipes.recipes} —— public static 的 {@code IRecipes} 实例，在 IU 的构造函数里
 *       由 {@code Recipes.registerRecipes()} 创建（{@code IUCore.java}）；</li>
 *   <li>{@code IRecipes.addRecipe(String name, BaseMachineRecipe)} —— 按【具名管理器】追加；</li>
 *   <li>{@code Recipes.inputFactory} —— {@code IInputHandler}，负责把 ItemStack/字符串包成输入。</li>
 * </ul>
 * 各机器的管理器名同样定义在 {@code RecipesCore} 的构造里，本类用到的两个：
 * {@code "macerator"}（IU 磨粉机）、{@code "genetic_stabilizer"}（遗传稳定器）。
 * 写法直接照抄 IU 自己的 {@code com.denfop.recipes.MaceratorRecipe#addmacerator}。</p>
 *
 * <p><b>调用时机</b>：必须在 IU 的 {@code Recipes.registerRecipes()} 之后，且等 IU 自己把
 * 内建配方灌完（它随后会 {@code optimize()} 重建索引）。所以挂在 {@code FMLLoadCompleteEvent}，
 * 那是所有 mod 初始化完毕的最后一个阶段。</p>
 */
public final class IuRecipes {
    private IuRecipes() {}

    /** IU 磨粉机的具名管理器（RecipesCore: addRecipeManager("macerator", 1, true, true)） */
    private static final String MACERATOR = "macerator";

    /** IU 切割机的具名管理器（RecipesCore: addRecipeManager("cutting", 1, true, true)） */
    private static final String CUTTING = "cutting";

    /** 奖杯各阶段的名字键，与 mod 的 TrophyBlockEntity.STAGE_NAME_KEYS 逐项对应。 */
    private static final Map<Integer, String> TROPHY_NAME_KEYS = Map.of(
            1, "tecend.trophy.uncharged",
            2, "tecend.trophy.rough_trimmed",
            3, "tecend.trophy.rough_cast",
            4, "tecend.trophy.ancient",
            5, "tecend.trophy.uncarved",
            6, "tecend.trophy.sprued");

    /**
     * 奖杯的 ItemStack —— NBT 与 KubeJS 侧 {@code trophySnbt(type)} 逐字段一致
     * （{@code display.Name} + {@code BlockEntityTag{id,type,charge}}），
     * 这样 IU 侧的匹配和 kjs 侧的配方认的是同一个东西。
     */
    private static ItemStack trophyStack(int type) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("proof_of_honor", "championplatform"));
        if (item == null) {
            return null;
        }
        ItemStack stack = new ItemStack(item);

        CompoundTag be = new CompoundTag();
        be.putString("id", "tecend:trophy");
        be.putInt("type", type);
        be.putLong("charge", 0L);

        CompoundTag root = new CompoundTag();
        root.put("BlockEntityTag", be);

        String nameKey = TROPHY_NAME_KEYS.get(type);
        if (nameKey != null) {
            // 可翻译组件，跟随客户端语言（与 mod 的 trophyStack() 行为一致）
            CompoundTag display = new CompoundTag();
            display.putString("Name", "{\"translate\":\"" + nameKey + "\"}");
            root.put("display", display);
        }

        stack.setTag(root);
        return stack;
    }

    public static void register() {
        // ② 粉碎：远古奖杯残片 → 粉碎的残片。
        // 这两个 id 由 GTCEu 的材质系统在运行时生成（材料 tecend:trophy_fragment），
        // 不是我们的注册对象，所以只能按 id 从注册表取，取不到就静默跳过。
        addMacerator("gtceu:trophy_fragment_clump", "gtceu:trophy_fragment_shard");

        // ⑨ 金猪：IU 遗传稳定器
        addGoldPig();

        // ⑫ 炸洗：IU 流式反应釜
        addFluidIntegrator();

        // ⑬ 剪水口：IU 切割机
        addCutting();
    }

    // ==================== ⑫ 炸洗 / ⑬ 剪水口：IU 两台 ====================

    /**
     * IU 流式反应釜（manager {@code "fluid_integrator"}）：⑫ 炸洗
     * —— 未处理(3) + 1B 硫酸 → 没有剪水口(6) + 1B 废硫酸。
     *
     * <p>IU 自己同样没调用过 {@code BlockEntityFluidIntegrator.addRecipe}
     * （源码全树零调用），这台机器出厂配方表是空的。</p>
     *
     * <p>有输出流体（IU 的废硫酸）就能走 IU 自己那个 4 参方法，
     * 它会成对写物品侧 + 流体侧两张表（IU 的约定），比只写物品侧稳妥。</p>
     */
    private static void addFluidIntegrator() {
        ItemStack in = trophyStack(3);
        ItemStack out = trophyStack(6);
        FluidStack sulfuric = fluidOf("gtceu:sulfuric_acid", 1000);
        // 炸洗的副产：硫酸洗过之后变成废液（IU 自己的流体）
        FluidStack wasteSulfuric = fluidOf("industrialupgrade:iufluidwastesulfuricacid", 1000);
        if (in == null || out == null || sulfuric == null || wasteSulfuric == null) {
            return;
        }
        BlockEntityFluidIntegrator.addRecipe(in, out, sulfuric, wasteSulfuric);
    }

    /**
     * IU 切割机（manager {@code "cutting"}）：⑬ 剪水口
     * —— 没有剪水口(6) → 未喷砂(2)。纯物品配方，没有流体侧。
     *
     * <p>IU 的 {@code MetalFormerRecipe.addcutting(String, String, int)} 这类辅助只收字符串 id、
     * 带不了 NBT，所以这里直接调底层 {@code Recipes.recipes.addRecipe}。</p>
     */
    private static void addCutting() {
        ItemStack in = trophyStack(6);
        ItemStack out = trophyStack(2);
        if (in == null || out == null) {
            return;
        }
        Recipes.recipes.addRecipe(CUTTING, new BaseMachineRecipe(
                new Input(Recipes.inputFactory.getInput(in)),
                new RecipeOutput(null, out)));
    }

    // ==================== ⑨ 金猪：IU 遗传稳定器 ====================

    // ==================== ⑨ 金猪：IU 遗传稳定器 ====================

    /**
     * 给遗传稳定器直接写金猪配方 —— 绕开 fluidunifyapi 那套"模板克隆"，手写展开。
     *
     * <p><b>为什么必须手写</b>：IU 定义了 public static 的
     * {@code BlockEntityGeneticStabilize.addRecipe(container, in, out)}，
     * 但整个 IU 源码树里没有任何地方调用它，这台机器出厂配方表是空的。
     * fluidunifyapi 的补丁要求"机器上已存在一条以模板流体为输入的配方"当模板，
     * 空表必然抛 {@code template fluid does not exist on this machine}。
     * 与其迁就那套克隆机制，不如把这 6 条一次写全。</p>
     *
     * <p>每条 {@code addRecipe} 会成对写进"物品侧 + 流体侧"两张表（IU 的约定，
     * 见它自己的实现）：
     * <pre>
     *   物品侧：Input(输入流体, 输入物品) → RecipeOutput(null, 同一个物品)
     *   流体侧：InputFluid(输入物品, 输入流体) → [输出流体]
     * </pre></p>
     *
     * <p>两种熔融金 × 三种猪载体 = 6 条：
     * <ul>
     *   <li>{@code industrialupgrade:iufluidmoltengold}（IU 的熔融金，点名"IU 只要这个"）</li>
     *   <li>{@code gtceu:gold}（GT 的熔融金）</li>
     * </ul>
     * 三种载体是设计里的"限定性卡口"，玩家手上是哪一种就走哪一条。</p>
     *
     * <p><b>时机</b>：本方法挂 {@code FMLLoadCompleteEvent}，早于
     * {@code TagsUpdatedEvent}（世界加载，fluidunifyapi 在那里打补丁）。</p>
     */
    private static void addGoldPig() {
        FluidStack iuGold = fluidOf("industrialupgrade:iufluidmoltengold", 1000);
        FluidStack gtGold = fluidOf("gtceu:gold", 1000);
        FluidStack goldPig = fluidOf("tecend:gold_pig", 1000);
        if (iuGold == null || gtGold == null || goldPig == null) {
            return;   // 缺哪个就整体跳过（上游没装时不该炸）
        }
        for (ItemStack container : pigContainers()) {
            BlockEntityGeneticStabilize.addRecipe(container, iuGold, goldPig);
            BlockEntityGeneticStabilize.addRecipe(container, gtGold, goldPig);
        }
    }

    /**
     * 三种"猪"载体的 ItemStack，NBT 判据与 KubeJS 侧第 ⑨ 步的配方逐字一致
     * （IF 用部分匹配的那部分键；PNC / pylons 用它们稳定的完整小结构）。
     */
    private static List<ItemStack> pigContainers() {
        List<ItemStack> list = new ArrayList<>();
        addIfPresent(list, "industrialforegoing:mob_imprisonment_tool",
                "{entity:\"minecraft:pig\"}");
        addIfPresent(list, "pneumaticcraft:spawner_core",
                "{\"pneumaticcraft:SpawnerCoreStats\":{\"minecraft:pig\":100}}");
        addIfPresent(list, "pylons:mob_filter",
                "{pylons:{name:\"entity.minecraft.pig\",registry:\"minecraft:pig\"}}");
        return list;
    }

    private static void addIfPresent(List<ItemStack> out, String itemId, String snbt) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null) {
            return;
        }
        ItemStack stack = new ItemStack(item);
        try {
            CompoundTag tag = TagParser.parseTag(snbt);
            stack.setTag(tag);
        } catch (Exception e) {
            return;   // SNBT 写错就不塞这个载体，别把整个注册拖崩
        }
        out.add(stack);
    }

    /** 按 id 取流体并包成 FluidStack；缺失返回 null。 */
    private static FluidStack fluidOf(String fluidId, int amount) {
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(new ResourceLocation(fluidId));
        return fluid == null ? null : new FluidStack(fluid, amount);
    }

    private static void addMacerator(String inputId, String outputId) {
        ItemStack in = stackOf(inputId);
        ItemStack out = stackOf(outputId);
        if (in == null || out == null) {
            return;
        }
        Recipes.recipes.addRecipe(MACERATOR, new BaseMachineRecipe(
                new Input(Recipes.inputFactory.getInput(in)),
                new RecipeOutput(null, out)));
    }

    /** 按 id 取物品；缺失返回 null（上游没装或材质没生成时不该炸）。 */
    private static ItemStack stackOf(String id) {
        Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
        return item == null ? null : new ItemStack(item);
    }
}
