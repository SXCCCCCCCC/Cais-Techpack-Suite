package com.sxcccccccc.fluidunifyapi.ic2.mixin;

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
 * IC2 高炉：压缩空气输入（模板 ic2_120:compressed_air）。
 * 门禁点：$airTankInternal$1.canInsert（ModFluids.isCompressedAir）+
 * 空气单元入槽 isCompressedAirFluidCell（isCompressedAirFluidCellCached 委托它，
 * 宽限叶子即可）。air 槽判定 else 分支走 isCompressedAirFluidCell，覆盖完整。
 */
public final class Ic2BlastFurnaceMixin {

    private static final String MACHINE = "ic2_120:blast_furnace";

    @Mixin(targets = "ic2_120.content.block.machines.BlastFurnaceBlockEntity$airTankInternal$1", remap = false)
    public abstract static class TankGate {

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
            boolean nativeResult = fluid == Ic2Support.ic2Fluid("compressed_air")
                    || fluid == Ic2Support.ic2Fluid("flowing_compressed_air");
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("compressed_air"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }
    }

    @Mixin(targets = "ic2_120.content.block.machines.BlastFurnaceBlockEntity", remap = false)
    public abstract static class ContainerGate {

        @Inject(
                method = "isCompressedAirFluidCell(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$airCell(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            if (UnifiedFluidRegistry.decide(MACHINE, Ic2Support.ic2Fluid("compressed_air"), fluid)
                    == UnifiedFluidRegistry.Decision.ACCEPT) {
                cir.setReturnValue(true);
            }
        }
    }
}
