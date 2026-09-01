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
 * IC2 发酵机：isBiomass（罐判定汇聚点，canInsert/ioStorage/tick 都走它）与
 * isBiomassFilledContainer（生物质容器入槽）两个叶子谓词各一个 RETURN 宽限。
 * 模板口径 = ic2_120:biomass。生物沼气输出侧不碰。
 */
public final class Ic2FermenterMixin {

    private static final String MACHINE = "ic2_120:fermenter";

    @Mixin(targets = "ic2_120.content.block.machines.FermenterBlockEntity", remap = false)
    public abstract static class Predicates {

        @Inject(
                method = "isBiomass(Lnet/minecraft/class_3611;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$biomass(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("biomass"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        @Inject(
                method = "isBiomassFilledContainer(Lnet/minecraft/class_1799;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$biomassContainer(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            if (UnifiedFluidRegistry.decide(MACHINE, Ic2Support.ic2Fluid("biomass"), fluid)
                    == UnifiedFluidRegistry.Decision.ACCEPT) {
                cir.setReturnValue(true);
            }
        }
    }
}
