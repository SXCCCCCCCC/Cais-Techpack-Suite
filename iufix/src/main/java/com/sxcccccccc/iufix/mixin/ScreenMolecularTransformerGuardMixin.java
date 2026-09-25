package com.sxcccccccc.iufix.mixin;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.Input;
import com.denfop.api.recipe.InventoryRecipes;
import com.denfop.api.recipe.MachineRecipe;
import com.denfop.api.recipe.RecipeOutput;
import com.denfop.blockentity.base.BlockEntityMolecularTransformer;
import com.denfop.recipe.IInputHandler;
import com.denfop.recipe.IInputItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 1.9.6（终版，纯 @Redirect 无 @Overwrite）：分子重组机 GUI 渲染 NPE 防线。
 *
 * <p>根因（1.9.4 插桩栈实锤）：单人档客户端容器 base 直指**服务端 BE**，
 * 渲染线程（容器同步包 ClientboundContainerSetContentPacket → SlotInvSlot.set →
 * InventoryRecipes.set → setRecipeOutput(null)）与 Server 线程（tick）无锁
 * 读写同一个 {@code output[]} 字段。GUI 渲染分支守卫（continue_proccess）
 * 读缓存，绘制却解引用实时查表结果——任何时刻缓存/查表都可能被另一线程
 * 写空，两处渲染方法（m_7286_ 渲染体、drawForegroundLayer 工具提示）共
 * 6 个裸解引用点（字段 output[i] 读取 ×2、getOutput(i) 调用 ×4）。
 *
 * <p>方案：原方法体一行不动（背景底图/工具提示/超类调用全部原样），
 * 两组 @Redirect 堵死 6 个解引用点，保证被解引用值永不为 null：
 * <ul>
 * <li>GETFIELD {@code output} → 返回"null 元素换成假配方"的安全副本；</li>
 * <li>INVOKE {@code getOutput} → 实时命中直通；null→缓存（命中打节流
 * 诊断日志）；缓存也 null→假配方兜底。</li>
 * </ul>
 * 1.9.2 的教训（handler 曾返回 null 被上层解引用）：兜底链末端是
 * 懒加载的假配方（energy=1、空输入输出），绝不返回 null，也绝不参与
 * 任何配方匹配（只喂显示代码）。
 */
@Mixin(targets = "com.denfop.screen.ScreenMolecularTransformer", remap = false)
public abstract class ScreenMolecularTransformerGuardMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    /** 节流：相同签名 5 秒内不重复打。 */
    private static long lastLogNanos;
    private static String lastLogSignature;

    /** 兜底假配方：仅在实时与缓存都 null 的竞态帧返回给显示代码，永不参与匹配。 */
    private static MachineRecipe dummyRecipe;

    // ===== m_7286_（渲染体）=====

    @Redirect(
            method = "m_7286_",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/denfop/blockentity/base/BlockEntityMolecularTransformer;output:[Lcom/denfop/api/recipe/MachineRecipe;"
            ),
            require = 1
    )
    private MachineRecipe[] iufix$safeOutputRender(BlockEntityMolecularTransformer te) {
        return iufix$safeOutput(te);
    }

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

    // ===== drawForegroundLayer（工具提示层）=====

    @Redirect(
            method = "drawForegroundLayer",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/denfop/blockentity/base/BlockEntityMolecularTransformer;output:[Lcom/denfop/api/recipe/MachineRecipe;"
            ),
            require = 1
    )
    private MachineRecipe[] iufix$safeOutputForeground(BlockEntityMolecularTransformer te) {
        return iufix$safeOutput(te);
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

    // ===== 兜底逻辑 =====

    /** 字段读兜底：null 元素换假配方的安全副本，随后 output[i].getRecipe() 不再 NPE。 */
    private static MachineRecipe[] iufix$safeOutput(BlockEntityMolecularTransformer te) {
        MachineRecipe[] arr = te.output;
        boolean hasNull = false;
        for (MachineRecipe r : arr) {
            if (r == null) {
                hasNull = true;
                break;
            }
        }
        if (!hasNull) {
            return arr;
        }
        MachineRecipe[] safe = Arrays.copyOf(arr, arr.length);
        for (int j = 0; j < safe.length; j++) {
            if (safe[j] == null) {
                safe[j] = iufix$dummyRecipe();
            }
        }
        return safe;
    }

    /** 调用兜底：实时命中→直通；null→缓存（打诊断）；缓存也 null→假配方。绝不返回 null。 */
    private static MachineRecipe iufix$guardGetOutput(BlockEntityMolecularTransformer te, int i, String site) {
        MachineRecipe fresh = te.getOutput(i);
        if (fresh != null) {
            return fresh;
        }
        MachineRecipe cached = te.getRecipeOutput(i);
        iufix$logNullDiagnostics(te, i, site, cached);
        return cached != null ? cached : iufix$dummyRecipe();
    }

    /** 兜底假配方（懒加载）：energy=1、空输入输出，仅供显示代码解引用。 */
    private static MachineRecipe iufix$dummyRecipe() {
        MachineRecipe d = dummyRecipe;
        if (d == null) {
            synchronized (ScreenMolecularTransformerGuardMixin.class) {
                if (dummyRecipe == null) {
                    CompoundTag nbt = new CompoundTag();
                    nbt.putDouble("energy", 1.0);
                    IInputHandler factory = Recipes.inputFactory;
                    dummyRecipe = new MachineRecipe(
                            new BaseMachineRecipe(
                                    new Input(factory.getInput(new ItemStack(Items.AIR, 1))),
                                    new RecipeOutput(nbt, new ItemStack(Items.AIR, 1))
                            ),
                            Collections.singletonList(0)
                    );
                }
                d = dummyRecipe;
            }
        }
        return d;
    }

    // ===== 诊断日志 =====

    private static void iufix$logNullDiagnostics(BlockEntityMolecularTransformer te, int i, String site, MachineRecipe cached) {
        String sig = i + "|null|" + iufix$recipeDesc(cached);
        long now = System.nanoTime();
        if (sig.equals(lastLogSignature) && now - lastLogNanos < 5_000_000_000L) {
            return;
        }
        lastLogSignature = sig;
        lastLogNanos = now;
        StringBuilder sb = new StringBuilder();
        sb.append("[iufix] ScreenMolecularTransformer getOutput(").append(i)
                .append(") 实时查表返回 null（site=").append(site)
                .append(cached != null ? "，回退缓存渲染防崩）" : "，缓存也为 null，回退假配方渲染防崩）").append('\n');
        sb.append(iufix$dumpMachineState(te, i));
        LOGGER.warn(sb.toString());
    }

    /** 竞态现场快照：输入槽内容 / 缓存配方 / 本槽快照 / 全局配方表。自带 try-catch。 */
    private static String iufix$dumpMachineState(BlockEntityMolecularTransformer te, int i) {
        StringBuilder sb = new StringBuilder();
        try {
            InventoryRecipes slot = te.inputSlot[i];
            ItemStack stack = slot.get(0);
            sb.append("  BE: queue=").append(te.queue)
                    .append(", maxAmount=").append(te.maxAmount)
                    .append(", 位置=").append(te.getBlockPos()).append('\n');
            sb.append("  当前线程: ").append(Thread.currentThread().getName()).append('\n');
            sb.append("  输入槽[").append(i).append("]: ").append(iufix$stackDesc(stack)).append('\n');
            sb.append("  缓存配方[").append(i).append("]: ").append(iufix$recipeDesc(te.getRecipeOutput(i))).append('\n');
            sb.append("  本槽 recipe_list 快照 (").append(slot.getRecipe_list() == null ? "null" : slot.getRecipe_list().size())
                    .append(" 条): ").append(iufix$recipeListDesc(slot.getRecipe_list())).append('\n');
            List<BaseMachineRecipe> global = Recipes.recipes.getRecipeList("molecular");
            sb.append("  全局 molecular 配方表 (").append(global == null ? "null" : global.size())
                    .append(" 条): ").append(iufix$recipeListDesc(global));
        } catch (Exception e) {
            sb.append("  （现场快照采集异常: ").append(e.getClass().getSimpleName()).append(':').append(e.getMessage()).append(')');
        }
        return sb.toString();
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
