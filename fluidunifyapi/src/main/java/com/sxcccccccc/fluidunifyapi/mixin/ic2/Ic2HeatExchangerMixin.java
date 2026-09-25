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
 * IC2 流体热交换器：四个叶子谓词（isHotCoolant/isLava/isCoolant/isPahoehoe）
 * 是全部判定点的汇聚（canInsert/ioStorage/容器判定/tick 都走它们）——各一个
 * RETURN 宽限即可全覆盖。模板口径：热冷却液/岩浆/冷却液/绳状岩浆各一。
 * 容器入槽（isValid 的桶分支）按"桶→流体→四模板任一 ACCEPT"宽限。
 */
public final class Ic2HeatExchangerMixin {

    private static final String MACHINE = "ic2_120:liquid_heat_exchanger";

    // 流体常量放 Ic2Support（mixin 包内禁止非 mixin 静态字段：
    // handler 引用本类非常量字段会生成对 mixin 类的直接引用 → IllegalClassLoadError）

    @Mixin(targets = "ic2_120.content.block.machines.FluidHeatExchangerBlockEntity", remap = false)
    public abstract static class Predicates {

        @Inject(
                method = "isHotCoolant(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$hotCoolant(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.hotCoolant(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        @Inject(
                method = "isLava(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$lava(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.lava(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        @Inject(
                method = "isCoolant(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$coolant(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.coolant(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        @Inject(
                method = "isPahoehoe(Lnet/minecraft/world/level/material/Fluid;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$pahoehoe(@Coerce Object fluidObj, CallbackInfoReturnable<Boolean> cir) {
            Fluid fluid = Ic2Support.normalize((Fluid) fluidObj);
            boolean nativeResult = cir.getReturnValueZ();
            boolean widened = UnifiedFluidRegistry.acceptOverride(MACHINE, Ic2Support.pahoehoe(), fluid, nativeResult);
            if (widened != nativeResult) {
                cir.setReturnValue(widened);
            }
        }

        /** 容器入槽（0.6 无 isValid）：输入满容器判定 = fheMatchesInputFilledContainer，
         * 桶/单元流体命中四模板任一 ACCEPT 即放行。 */
        @Inject(
                method = "fheMatchesInputFilledContainer(Lnet/minecraft/world/item/ItemStack;)Z",
                at = @At("RETURN"),
                cancellable = true,
                require = 1,
                remap = false
        )
        private void fluidunifyapi$container(@Coerce Object stackObj, CallbackInfoReturnable<Boolean> cir) {
            if (cir.getReturnValueZ()) {
                return;
            }
            Fluid fluid = Ic2Support.normalize(Ic2Support.fluidOfItem((ItemStack) stackObj));
            if (fluid == null) {
                return;
            }
            Fluid[] templates = {Ic2Support.hotCoolant(), Ic2Support.lava(), Ic2Support.coolant(), Ic2Support.pahoehoe()};
            for (Fluid template : templates) {
                if (UnifiedFluidRegistry.decide(MACHINE, template, fluid) == UnifiedFluidRegistry.Decision.ACCEPT) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }
}
