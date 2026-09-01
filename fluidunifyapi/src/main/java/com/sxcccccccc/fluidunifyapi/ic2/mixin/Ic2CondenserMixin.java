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
 * IC2 冷凝机输入口：蒸汽罐（模板 ic2_120:steam）+ 水桶入槽（模板 minecraft:water）。
 * 蒸馏水输出侧不碰。
 */
public final class Ic2CondenserMixin {

    private static final String MACHINE = "ic2_120:condenser";

    @Mixin(targets = "ic2_120.content.block.machines.CondenserBlockEntity$steamTank$1", remap = false)
    public abstract static class SteamTankGate {

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
            boolean nativeResult = fluid == Ic2Support.ic2Fluid("steam") || fluid == Ic2Support.ic2Fluid("superheated_steam");
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.ic2Fluid("steam"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }
    }

    /** 水桶入槽（SLOT_WATER_INPUT）：原生只收空桶/水桶，补丁新流体的桶也可放。 */
    @Mixin(targets = "ic2_120.content.block.machines.CondenserBlockEntity", remap = false)
    public abstract static class ContainerGate {

        @Inject(
                method = "isWaterBucket(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$waterBucket(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            ItemStack stack = (ItemStack) stackObj;
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem(stack));
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
