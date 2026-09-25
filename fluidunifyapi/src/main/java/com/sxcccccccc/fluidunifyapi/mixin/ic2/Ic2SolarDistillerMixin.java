package com.sxcccccccc.fluidunifyapi.mixin.ic2;

import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.ic2.Ic2Support;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IC2 太阳能蒸馏机：水输入（模板 minecraft:water）。蒸馏水输出侧不碰。
 * 门禁点：$inputTankInternal$1.canInsert（WATER||FLOWING_WATER）、
 * ioStorage.insert（WATER）、水容器入槽 isWaterInputStack。
 */
public final class Ic2SolarDistillerMixin {

    private static final String MACHINE = "ic2_120:solar_distiller";

    @Mixin(targets = "ic2_120.content.block.machines.SolarDistillerBlockEntity$inputTankInternal$1", remap = false)
    public abstract static class InputTankGate {

        @Inject(
                method = "canInsert(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;)Z",
                at = @At("HEAD"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$canInsert(@Coerce Object variant, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOf(variant));
            if (fluid == null) {
                return;
            }
            boolean nativeResult = fluid == Ic2Support.water() || fluid == Ic2Support.flowingWater();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.water(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }
    }

    @Mixin(targets = "ic2_120.content.block.machines.SolarDistillerBlockEntity$ioStorage$1", remap = false)
    public abstract static class IoInsertGate {

        @Inject(
                method = "insert(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;JLnet/fabricmc/fabric/api/transfer/v1/transaction/TransactionContext;)J",
                at = @At("HEAD"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$insert(@Coerce Object variant, long maxAmount, @Coerce Object tx,
                                          CallbackInfoReturnable<Long> cir) {
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOf(variant));
            if (fluid == null) {
                return;
            }
            boolean nativeResult = fluid == Ic2Support.water() || fluid == Ic2Support.flowingWater();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.water(), fluid, nativeResult);
            if (widened && !nativeResult) {
                // 原生 ioStorage 内联判定 + 归一化为 WATER 再入罐——ACCEPT 时绕过，
                // 身份保持直插输入罐（罐 canInsert 已被加宽）
                long r = Ic2Support.directInsert(this, "inputTankInternal", variant, maxAmount, tx);
                if (r >= 0L) {
                    cir.setReturnValue(r);
                    cir.cancel();
                }
            } else if (!widened) {
                cir.setReturnValue(0L);
                cir.cancel();
            }
        }
    }

    /** 水容器入槽（SLOT_INPUT_WATER）：桶/单元容器反查流体后咨询。 */
    @Mixin(targets = "ic2_120.content.block.machines.SolarDistillerBlockEntity", remap = false)
    public abstract static class ContainerGate {

        @Inject(
                method = "isWaterInputStack(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$waterContainer(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            if (UnifiedFluidRegistry.decide(MACHINE, Ic2Support.water(), fluid)
                    == UnifiedFluidRegistry.Decision.ACCEPT) {
                cir.setReturnValue(true);
            }
        }
    }
}
