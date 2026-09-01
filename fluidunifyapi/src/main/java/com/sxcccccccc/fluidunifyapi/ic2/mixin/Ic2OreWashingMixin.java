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
 * IC2 洗矿机：水输入（模板 minecraft:water）。
 * 门禁点：$waterTankInternal$1.canInsert + insert 覆写 + 水容器入槽 isWaterInput。
 */
public final class Ic2OreWashingMixin {

    private static final String MACHINE = "ic2_120:ore_washing_plant";

    @Mixin(targets = "ic2_120.content.block.machines.OreWashingPlantBlockEntity$waterTankInternal$1", remap = false)
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
            boolean nativeResult = fluid == Ic2Support.water() || fluid == Ic2Support.flowingWater();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.water(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }

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
            if (!widened) {
                cir.setReturnValue(0L);
                cir.cancel();
            }
        }
    }

    @Mixin(targets = "ic2_120.content.block.machines.OreWashingPlantBlockEntity", remap = false)
    public abstract static class ContainerGate {

        @Inject(
                method = "isWaterInput(Lnet/minecraft/world/item/ItemStack;)Z",
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
