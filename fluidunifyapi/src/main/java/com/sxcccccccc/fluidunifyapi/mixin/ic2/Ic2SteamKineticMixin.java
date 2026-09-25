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

    /** ioStorage.insert 有内联硬判定（非蒸汽非过热蒸汽 return 0L）——ACCEPT 时
     *  身份保持直插蒸汽罐（罐 canInsert 已被加宽）。 */
    @Mixin(targets = "ic2_120.content.block.machines.SteamKineticGeneratorBlockEntity$ioStorage$1", remap = false)
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
            boolean nativeResult = fluid == Ic2Support.ic2Fluid("steam")
                    || fluid == Ic2Support.ic2Fluid("superheated_steam");
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("steam"), fluid, nativeResult);
            if (widened && !nativeResult) {
                long r = Ic2Support.directInsert(this, "steamTank", variant, maxAmount, tx);
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
}
