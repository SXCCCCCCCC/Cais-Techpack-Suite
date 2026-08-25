package com.sxcccccccc.uubridge.mixin;

import com.sxcccccccc.uubridge.compat.CrystalMemoryBridge;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 断点①：扫描机磁盘槽过滤（crystal_memory_compat_study §5.3 M1）。
 *
 * <p>目标：{@code BlockEntityScanner$1}（构造器匿名类，磁盘槽），jar 实证
 * 类名/方法名：{@code m_7013_}（canPlaceItem，Forge reobf 把原版接口方法
 * 实现重命名为 SRG 名）。原逻辑 {@code stack.getItem() ==
 * IUItem.crystalMemory.getItem()} 按 item 实例过滤 → IC2 水晶放不进去
 * （UU 链第一步即断）。
 *
 * <p>修复：HEAD cancellable，{@code CrystalMemoryBridge.isCrystalMemory}
 * （IU 水晶 || IC2 水晶）→ true，其余物品走原逻辑。
 */
@Mixin(targets = "com.denfop.blockentity.base.BlockEntityScanner$1", remap = false)
public abstract class ScannerDiskSlotMixin {

    @Inject(method = "m_7013_", at = @At("HEAD"), cancellable = true, require = 1)
    private void uubridge$allowIc2Crystal(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (CrystalMemoryBridge.isCrystalMemory(stack)) {
            cir.setReturnValue(true);
        }
    }
}
