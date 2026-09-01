package com.sxcccccccc.fluidunifyapi.ic2.mixin;

import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.ic2.Ic2Support;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IC2 压缩机：水输入（模板 minecraft:water）。
 * 门禁点：isWater(FluidVariant) 私有谓词（罐判定汇聚点）。
 */
public final class Ic2CompressorMixin {

    private static final String MACHINE = "ic2_120:compressor";

    @Mixin(targets = "ic2_120.content.block.machines.CompressorBlockEntity", remap = false)
    public abstract static class Predicates {

        @Inject(
                method = "isWater(Lnet/fabricmc/fabric/api/transfer/v1/fluid/FluidVariant;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$water(@Coerce Object variant, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOf(variant));
            if (fluid == null) {
                return;
            }
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.water(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }
    }
}
