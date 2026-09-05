package com.sxcccccccc.iufix.mixin;

import com.denfop.api.recipe.MachineRecipe;
import com.denfop.blockentity.base.BlockEntityMolecularTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.9.4 诊断插桩：捕获"缓存 output[i] 被置 null"的完整调用栈。
 *
 * <p>11:01 崩溃证明缓存存在被并发 setRecipeOutput(null) 清空的竞态窗口，
 * 但不知道是哪个线程/哪条路径在清（客户端同步包？网络线程？服务端逻辑？）。
 * 此 mixin 在 setRecipeOutput(int, MachineRecipe) HEAD 处对 null 写入打
 * 全栈日志（含 side/线程名/BE 位置，5 秒节流）。两种 NULL 形态都会经过这里：
 * ①空槽 → set() → process()=null → setRecipeOutput(null)（正常清空路径，
 * 服务端也会发生，节流防刷屏）；②渲染帧中途的并发清空（竞态实锤路径）。
 * 只打日志不改变任何行为。
 */
@Mixin(value = BlockEntityMolecularTransformer.class, remap = false)
public abstract class MolecularTransformerCacheTraceMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    private static long lastLogNanos;
    private static String lastSig;

    @Inject(
            method = "setRecipeOutput(ILcom/denfop/api/recipe/MachineRecipe;)V",
            at = @At("HEAD"),
            require = 1
    )
    private void iufix$traceNullCacheWrite(int i, MachineRecipe output, CallbackInfo ci) {
        if (output != null) {
            return;
        }
        try {
            BlockEntityMolecularTransformer te = (BlockEntityMolecularTransformer) (Object) this;
            String thread = Thread.currentThread().getName();
            String side = te.getLevel() == null ? "level-null" : (te.getLevel().isClientSide() ? "client" : "server");
            String sig = side + '|' + thread + '|' + i;
            long now = System.nanoTime();
            if (sig.equals(lastSig) && now - lastLogNanos < 5_000_000_000L) {
                return;
            }
            lastSig = sig;
            lastLogNanos = now;

            StackTraceElement[] st = Thread.currentThread().getStackTrace();
            // 已知的正常路径（1.9.4 栈追踪实锤）：客户端容器同步包
            // ClientPacketListener → AbstractContainerMenu.initializeContents → SlotInvSlot.set
            // → InventoryRecipes.set → setRecipeOutput(null)——渲染线程上写服务端 BE，
            // 每次槽被同步为空都会发生，属于 IU 单人档的正常同步行为，不再记录。
            for (StackTraceElement e : st) {
                if (e.getClassName().contains("ClientPacketListener")) {
                    return;
                }
            }
            StringBuilder sb = new StringBuilder();
            sb.append("[iufix] 缓存 output[").append(i).append("] 被置 null（side=").append(side)
                    .append(", thread=").append(thread)
                    .append(", 位置=").append(te.getBlockPos()).append("），写入调用栈：\n");
            for (int j = 2; j < st.length && j < 42; j++) {
                sb.append("    at ").append(st[j]).append('\n');
            }
            LOGGER.warn(sb.toString());
        } catch (Exception ignored) {
            // 插桩代码绝不抛异常
        }
    }
}
