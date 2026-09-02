package com.sxcccccccc.fluidunifyapi.mixin.ic2;

import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.ic2.Ic2Support;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IC2 流体加热机：燃料输入（模板 ic2_120:biofuel）。
 * 0.6 运行时反编译实证：罐 canInsert/insert、tryInsertFuel 消耗、
 * isSupportedFuelContainer 容器槽判定全部收敛到叶子谓词
 * isSupportedFuelFluid（== BIOFUEL_STILL || BIOFUEL_FLOWING），另 FuelType.matches
 * 控制发热类型——叶子+matches 宽限即全覆盖，无需罐 mixin。
 * processFuelContainers 原生只吃 ic2 自家 cell（无 Forge IFluidHandlerItem）；
 * 第三方 Forge 桶经 FluidUtil 探针 + 机器自家插入例程反射真插（SOP 真事务手法，
 * 自 fluidunifyfix M2 迁移）。
 */
public final class Ic2FluidHeatGeneratorMixin {

    private static final String MACHINE = "ic2_120:fluid_heat_generator";
    private static final Fluid BIOFUEL = Ic2Support.ic2Fluid("biofuel");

    @Mixin(targets = "ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity", remap = false)
    public abstract static class Predicates {

        @Inject(
                method = "isSupportedFuelFluid(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$fuelFluid(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
            if (!UnifiedFluidRegistry.hasPatch(MACHINE)) {
                return;
            }
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, BIOFUEL, Ic2Support.normalize(fluid), nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        /** 第三方 Forge 桶消耗（原生只吃 ic2 自家 cell）；不 cancel，原生体照跑。 */
        @Inject(
                method = "processFuelContainers",
                at = @At("HEAD"),
                require = 1,
                remap = false
        )
        private void fluidunifyapi$processContainers(CallbackInfo ci) {
            if (!UnifiedFluidRegistry.hasPatch(MACHINE)) {
                return;
            }
            Container inv = (Container) (Object) this;
            ItemStack stack = inv.getItem(0);
            if (stack.isEmpty()) {
                return;
            }
            FluidStack contained = FluidUtil.getFluidContained(stack).orElse(FluidStack.EMPTY);
            ItemStack remainder = stack.getCraftingRemainingItem();
            if (contained.isEmpty() || remainder.isEmpty()) {
                return; // ic2 自家容器（无 Forge handler → 探针空）/非桶类，原生逻辑接管
            }
            Fluid fluid = Ic2Support.normalize(contained.getFluid());
            if (UnifiedFluidRegistry.decide(MACHINE, BIOFUEL, fluid)
                    != UnifiedFluidRegistry.Decision.ACCEPT) {
                return;
            }
            if (!Ic2Support.fireboxConsumeContainer(this, fluid, remainder)) {
                return; // 空容器槽满/罐满，不动
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                inv.setItem(0, ItemStack.EMPTY);
            }
            inv.setChanged();
        }
    }

    @Mixin(targets = "ic2_120.content.block.machines.FluidHeatGeneratorBlockEntity$FuelType", remap = false)
    public abstract static class FuelTypeGate {

        @Inject(
                method = "matches(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$matches(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
            if (!UnifiedFluidRegistry.hasPatch(MACHINE)) {
                return;
            }
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, BIOFUEL, Ic2Support.normalize(fluid), nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }
    }
}
