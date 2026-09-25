package com.sxcccccccc.uubridge.mixin;

import com.sxcccccccc.uubridge.compat.CrystalMemoryBridge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 断点②：模式存储机磁盘槽过滤（crystal_memory_compat_study §5.3 M2）。
 *
 * <p>目标：{@code BlockEntityPatternStorage$1}（构造器匿名类，磁盘槽），jar
 * 实证 {@code m_7013_}（canPlaceItem）。原逻辑按 item 实例过滤（同扫描机）
 * → IC2 水晶放不进去，模式存储机按钮 2/3（写/读水晶）不可用。
 *
 * <p>修复：同 M1。
 */
@Mixin(targets = "com.denfop.blockentity.mechanism.BlockEntityPatternStorage$1", remap = false)
public abstract class PatternStorageDiskSlotMixin {

    @Inject(method = "m_7013_", at = @At("HEAD"), cancellable = true, require = 1)
    private void uubridge$allowIc2Crystal(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CrystalMemoryBridge.isCrystalMemory(stack)) {
            cir.setReturnValue(true);
        }
    }
}
