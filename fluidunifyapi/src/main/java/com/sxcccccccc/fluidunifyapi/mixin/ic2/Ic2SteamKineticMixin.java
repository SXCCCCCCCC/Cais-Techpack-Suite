package com.sxcccccccc.fluidunifyapi.mixin.ic2;

import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.ic2.Ic2Support;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IC2 蒸汽动能发电机：蒸汽输入（模板 ic2_120:steam）。
 * 门禁点：$steamTank$1.canInsert（ModFluids.isSteam）。
 */
public final class Ic2SteamKineticMixin {

    private static final String MACHINE = "ic2_120:steam_kinetic_generator";

    @Mixin(targets = "ic2_120.content.block.machines.SteamKineticGeneratorBlockEntity$steamTank$1", remap = false)
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
            boolean nativeResult = fluid == Ic2Support.ic2Fluid("steam")
                    || fluid == Ic2Support.ic2Fluid("superheated_steam");
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("steam"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }
    }
}
