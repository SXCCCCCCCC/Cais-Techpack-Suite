package com.sxcccccccc.iufix.mixin;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.InventoryRecipes;
import com.denfop.api.recipe.MachineRecipe;
import com.denfop.blockentity.base.BlockEntityMolecularTransformer;
import com.denfop.recipe.IInputItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

/**
 * 1.9.2：分子重组机 GUI 渲染 NPE（crash-2026-09-05_09.49.29-client.txt）。
 *
 * <p>ScreenMolecularTransformer 的 queue（x64）渲染分支裸调
 * {@code BlockEntityMolecularTransformer.getOutput(i).getRecipe()}：
 * getOutput 内部是实时配方查表（{@code output[i] = inputSlot[i].process()}），
 * 无配方命中时合法返回 null（BE 其他调用点如 setOverclockRates 都有判空），
 * 唯独 GUI 两处（m_7286_ 渲染体 ×1、drawForegroundLayer 工具提示 ×4）没有。
 * 触发窗口：机器 queue 模式运行中（缓存 output[i] 非空 → 分支守卫
 * continue_proccess 通过），但实时查表失败（输入物品 NBT 与配方注册栈
 * 不一致，或本槽 recipe_list 快照与全局配方表不一致）→ null.getRecipe()
 * 渲染线程 NPE，游戏紧急存退档。
 *
 * <p>修复：@Redirect 全部 5 处 getOutput 调用点。handler 先读缓存
 * （getRecipeOutput(i)，即分支守卫 continue_proccess 判过的同一份缓存——
 * 必须先读，getOutput 内部会把它覆写为 null），再执行原调用；原调用
 * 返回非空直接放行，返回 null 则打诊断日志（输入槽物品/NBT、缓存配方、
 * 本槽 recipe_list 快照、全局 "molecular" 配方表）并回退缓存值——缓存值
 * 由守卫保证非空，从而渲染不崩且显示与机器实际运行的配方一致。
 */
@Mixin(targets = "com.denfop.screen.ScreenMolecularTransformer", remap = false)
public abstract class ScreenMolecularTransformerGuardMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    /** 节流：相同签名 5 秒内不重复打，防止 GUI 常驻期间逐帧刷屏。 */
    private static long lastLogNanos;
    private static String lastLogSignature;

    @Redirect(
            method = "m_7286_",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/denfop/blockentity/base/BlockEntityMolecularTransformer;getOutput(I)Lcom/denfop/api/recipe/MachineRecipe;"
            ),
            require = 1
    )
    private MachineRecipe iufix$guardGetOutputRender(BlockEntityMolecularTransformer te, int i) {
        return iufix$guardGetOutput(te, i, "m_7286_");
    }

    @Redirect(
            method = "drawForegroundLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/denfop/blockentity/base/BlockEntityMolecularTransformer;getOutput(I)Lcom/denfop/api/recipe/MachineRecipe;"
            ),
            require = 1
    )
    private MachineRecipe iufix$guardGetOutputForeground(BlockEntityMolecularTransformer te, int i) {
        return iufix$guardGetOutput(te, i, "drawForegroundLayer");
    }

    private MachineRecipe iufix$guardGetOutput(BlockEntityMolecularTransformer te, int i, String site) {
        // 必须先取缓存：getOutput(i) 内部执行 output[i] = process()，会把缓存覆写掉。
        MachineRecipe cached = te.getRecipeOutput(i);
        MachineRecipe fresh = te.getOutput(i);
        if (fresh != null) {
            return fresh;
        }
        iufix$logNullDiagnostics(te, i, site, cached);
        // 分支守卫（inputSlot[i].continue_proccess → getRecipeOutput(i)）刚判过缓存非空，
        // 故此处 cached 必非空；回退缓存值渲染，显示与机器实际运行的配方一致。
        return cached;
    }

    private static void iufix$logNullDiagnostics(BlockEntityMolecularTransformer te, int i, String site, MachineRecipe cached) {
        StringBuilder sig = new StringBuilder();
        try {
            InventoryRecipes slot = te.inputSlot[i];
            ItemStack stack = slot.get(0);
            sig.append(i).append('|').append(iufix$stackDesc(stack)).append('|').append(iufix$recipeDesc(cached));

            String signature = sig.toString();
            long now = System.nanoTime();
            if (signature.equals(lastLogSignature) && now - lastLogNanos < 5_000_000_000L) {
                return;
            }
            lastLogSignature = signature;
            lastLogNanos = now;

            StringBuilder sb = new StringBuilder();
            sb.append("[iufix] ScreenMolecularTransformer getOutput(").append(i)
                    .append(") 实时查表返回 null（site=").append(site)
                    .append("，已回退缓存渲染防崩）\n");
            sb.append("  BE: queue=").append(te.queue)
                    .append(", maxAmount=").append(te.maxAmount)
                    .append(", 位置=").append(te.getBlockPos()).append('\n');
            sb.append("  输入槽[").append(i).append("]: ").append(iufix$stackDesc(stack)).append('\n');
            sb.append("  缓存配方[").append(i).append("]: ").append(iufix$recipeDesc(cached)).append('\n');
            sb.append("  本槽 recipe_list 快照 (").append(slot.getRecipe_list() == null ? "null" : slot.getRecipe_list().size())
                    .append(" 条): ").append(iufix$recipeListDesc(slot.getRecipe_list())).append('\n');
            List<BaseMachineRecipe> global = Recipes.recipes.getRecipeList("molecular");
            sb.append("  全局 molecular 配方表 (").append(global == null ? "null" : global.size())
                    .append(" 条): ").append(iufix$recipeListDesc(global)).append('\n');
            sb.append("  （实时查表 null 的常见原因：输入物品 NBT 与配方注册栈不一致，")
                    .append("或本槽快照与全局配方表不一致；对比上面 4 行即可定位）");
            LOGGER.warn(sb.toString());
        } catch (Exception e) {
            // 诊断代码本身绝不允许再炸渲染线程：任何异常静默降级为普通 null 日志。
            LOGGER.warn("[iufix] ScreenMolecularTransformer getOutput({}) null（诊断代码异常 {}: {}）",
                    i, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static String iufix$stackDesc(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(ForgeRegistries.ITEMS.getKey(stack.getItem()));
        sb.append(" x").append(stack.getCount());
        if (stack.getTag() != null) {
            String nbt = stack.getTag().toString();
            sb.append(" nbt=").append(iufix$truncate(nbt, 200));
        } else {
            sb.append(" nbt=null");
        }
        return sb.toString();
    }

    private static String iufix$recipeDesc(MachineRecipe recipe) {
        if (recipe == null || recipe.getRecipe() == null) {
            return "<null>";
        }
        BaseMachineRecipe base = recipe.getRecipe();
        StringBuilder sb = new StringBuilder();
        sb.append("input=[");
        List<IInputItemStack> inputs = base.input.getInputs();
        for (int j = 0; j < inputs.size(); j++) {
            if (j > 0) {
                sb.append(" | ");
            }
            IInputItemStack in = inputs.get(j);
            if (in.hasTag()) {
                sb.append("tag:").append(in.getTag().location());
            } else {
                List<ItemStack> stacks = in.getInputs();
                sb.append(stacks.isEmpty() ? "<?>" : iufix$stackDesc(stacks.get(0)));
            }
        }
        sb.append("] output=[");
        List<ItemStack> outs = base.output.items;
        for (int j = 0; j < outs.size(); j++) {
            if (j > 0) {
                sb.append(" | ");
            }
            sb.append(iufix$stackDesc(outs.get(j)));
        }
        sb.append(']');
        return sb.toString();
    }

    private static String iufix$recipeListDesc(List<BaseMachineRecipe> list) {
        if (list == null) {
            return "<null>";
        }
        if (list.isEmpty()) {
            return "<empty>";
        }
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(list.size(), 16);
        for (int j = 0; j < shown; j++) {
            if (j > 0) {
                sb.append(" || ");
            }
            BaseMachineRecipe base = list.get(j);
            List<IInputItemStack> inputs = base.input.getInputs();
            sb.append('[');
            for (int k = 0; k < inputs.size(); k++) {
                if (k > 0) {
                    sb.append(" | ");
                }
                IInputItemStack in = inputs.get(k);
                if (in.hasTag()) {
                    sb.append("tag:").append(in.getTag().location());
                } else {
                    List<ItemStack> stacks = in.getInputs();
                    sb.append(stacks.isEmpty() ? "<?>" : iufix$stackDesc(stacks.get(0)));
                }
            }
            sb.append(" -> ");
            List<ItemStack> outs = base.output.items;
            sb.append(outs.isEmpty() ? "<?>" : iufix$stackDesc(outs.get(0)));
            sb.append(']');
        }
        if (list.size() > shown) {
            sb.append(" ...(共 ").append(list.size()).append(" 条)");
        }
        return sb.toString();
    }

    private static String iufix$truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
