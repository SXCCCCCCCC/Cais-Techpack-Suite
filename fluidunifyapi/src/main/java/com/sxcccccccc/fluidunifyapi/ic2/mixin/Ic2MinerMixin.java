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
 * IC2 矿机：钻头冷却液输入（原生 WATER 或 LAVA 二选一，
 * 模板口径 [minecraft:water, minecraft:lava] 多模板 consultAny）。
 * 门禁点：$fluidTankInternal$1.canInsert。
 */
public final class Ic2MinerMixin {

    private static final String MACHINE = "ic2_120:miner";

    // 0.6 jar 实证：矿机流体罐在基类 BaseMinerBlockEntity（非 MinerBlockEntity）
    @Mixin(targets = "ic2_120.content.block.machines.BaseMinerBlockEntity$fluidTankInternal$1", remap = false)
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
            boolean nativeResult = fluid == Ic2Support.water() || fluid == Ic2Support.lava();
            boolean widened = UnifiedFluidRegistry.acceptOverrideAny(MACHINE,
                    new Fluid[]{Ic2Support.water(), Ic2Support.lava()}, fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }
    }
}
