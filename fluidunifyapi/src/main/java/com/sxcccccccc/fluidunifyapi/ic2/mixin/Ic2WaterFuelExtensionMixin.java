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
 * IC2 水力发电机的槽位容器判定：顶层扩展函数
 * {@code ic2_120.content.item.CellsAndBucketsKt.isWaterFuel(ItemStack)}（static，
 * Kotlin 顶层 fun 编译到 XxxKt 类）。RETURN 宽限：容器流体命中水模板补丁即放行。
 */
public final class Ic2WaterFuelExtensionMixin {

    private static final String MACHINE = "ic2_120:water_generator";

    @Mixin(targets = "ic2_120.content.item.CellsAndBucketsKt", remap = false)
    public abstract static class ExtensionGate {

        @Inject(
                method = "isWaterFuel(Lnet/minecraft/class_1799;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private static void fluidunifyapi$waterFuel(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
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
