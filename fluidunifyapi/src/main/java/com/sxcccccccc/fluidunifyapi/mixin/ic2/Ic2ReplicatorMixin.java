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
 * IC2 复制机：UU 物质输入（模板 ic2_120:uu_matter）。
 * 门禁点：isUuMatter（罐判定汇聚）与 replicatorIsDrainableUuContainer（UU 单元入槽）。
 */
public final class Ic2ReplicatorMixin {

    private static final String MACHINE = "ic2_120:replicator";

    @Mixin(targets = "ic2_120.content.block.machines.ReplicatorBlockEntity", remap = false)
    public abstract static class Predicates {

        @Inject(
                method = "isUuMatter(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$uuMatter(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("uu_matter"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        @Inject(
                method = "replicatorIsDrainableUuContainer(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$uuContainer(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            if (UnifiedFluidRegistry.decide(MACHINE, Ic2Support.ic2Fluid("uu_matter"), fluid)
                    == UnifiedFluidRegistry.Decision.ACCEPT) {
                cir.setReturnValue(true);
            }
        }
    }
}
