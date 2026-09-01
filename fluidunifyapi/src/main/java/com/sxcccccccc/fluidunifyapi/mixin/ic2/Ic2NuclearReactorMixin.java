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
 * IC2 核反应堆：冷却液输入（模板 ic2_120:coolant）与热冷却液容器槽
 * （模板 ic2_120:hot_coolant）。
 * 门禁点：$inputTank$1.canInsert（inline == COOLANT）、
 * isCoolantInputContainer/isHotCoolantInputContainer（容器入槽）、
 * isCoolantFluid（tick 消耗守卫）。
 */
public final class Ic2NuclearReactorMixin {

    private static final String MACHINE = "ic2_120:nuclear_reactor";

    @Mixin(targets = "ic2_120.content.block.nuclear.NuclearReactorBlockEntity$inputTank$1", remap = false)
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
            boolean nativeResult = fluid == Ic2Support.ic2Fluid("coolant");
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("coolant"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
                cir.cancel();
            }
        }
    }

    /** ioStorage.insert 内联判定 + 归一化为 COOLANT_STILL——ACCEPT 时身份保持直插
     *  输入罐（罐 canInsert 已被加宽）。注意非热模式 getFluidStorageForSide 返回
     *  Storage.empty()，流体口只在热模式存在（机器原生语义，不归本 mod 管）。 */
    @Mixin(targets = "ic2_120.content.block.nuclear.NuclearReactorBlockEntity$ioStorage$1", remap = false)
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
            boolean nativeResult = fluid == Ic2Support.ic2Fluid("coolant");
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("coolant"), fluid, nativeResult);
            if (widened && !nativeResult) {
                long r = Ic2Support.directInsert(this, "inputTank", variant, maxAmount, tx);
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

    @Mixin(targets = "ic2_120.content.block.nuclear.NuclearReactorBlockEntity", remap = false)
    public abstract static class Predicates {

        @Inject(
                method = "isCoolantInputContainer(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$coolantContainer(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            if (UnifiedFluidRegistry.decide(MACHINE, Ic2Support.ic2Fluid("coolant"), fluid)
                    == UnifiedFluidRegistry.Decision.ACCEPT) {
                cir.setReturnValue(true);
            }
        }

        @Inject(
                method = "isHotCoolantInputContainer(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$hotCoolantContainer(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            if (UnifiedFluidRegistry.decide(MACHINE, Ic2Support.ic2Fluid("hot_coolant"), fluid)
                    == UnifiedFluidRegistry.Decision.ACCEPT) {
                cir.setReturnValue(true);
            }
        }

        @Inject(
                method = "isCoolantFluid(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$coolantFluid(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(
                    MACHINE, Ic2Support.ic2Fluid("coolant"), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }
    }
}
