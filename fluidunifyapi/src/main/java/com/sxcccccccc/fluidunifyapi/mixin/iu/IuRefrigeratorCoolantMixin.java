package com.sxcccccccc.fluidunifyapi.mixin.iu;

import com.denfop.blockentity.mechanism.BlockEntityRefrigeratorCoolant;
import com.denfop.blocks.FluidName;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * IU 特殊机 2/2：冰箱冷却液灌装例程（updateEntityServer 内三处
 * {@code tank.getFluid().getFluid() == FluidName.fluidhydrogen/nitrogen} 的内联
 * == 判定，3.4.0.10 jar 字节码核对：本方法内 getFluid() 调用点 3 处，
 * ordinal 0 = 氢、ordinal 1 = 氮、ordinal 2 = 氦）。
 *
 * <p>手法：@Redirect 三处 {@code FluidStack.getFluid()}——罐内流体被补丁接受时
 * 返回模板流体本身，令内联 == 恒真走原生灌装分支；否则返回原值（字节码原样）。
 * 灌装后按量 drain，罐内流体身份不变。</p>
 */
@Mixin(value = BlockEntityRefrigeratorCoolant.class, remap = false)
public abstract class IuRefrigeratorCoolantMixin {

    private static final String MACHINE = "industrialupgrade:refrigerator_coolant";

    @Redirect(
            method = "updateEntityServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidStack;getFluid()Lnet/minecraft/world/level/material/Fluid;",
                    ordinal = 0
            ),
            require = 1,
            remap = false
    )
    private Fluid fluidunifyapi$hydrogenSite(FluidStack stack) {
        return templateOrActual(stack, FluidName.fluidhydrogen.getInstance().get());
    }

    @Redirect(
            method = "updateEntityServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidStack;getFluid()Lnet/minecraft/world/level/material/Fluid;",
                    ordinal = 1
            ),
            require = 1,
            remap = false
    )
    private Fluid fluidunifyapi$nitrogenSite(FluidStack stack) {
        return templateOrActual(stack, FluidName.fluidnitrogen.getInstance().get());
    }

    @Redirect(
            method = "updateEntityServer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidStack;getFluid()Lnet/minecraft/world/level/material/Fluid;",
                    ordinal = 2
            ),
            require = 1,
            remap = false
    )
    private Fluid fluidunifyapi$heliumSite(FluidStack stack) {
        return templateOrActual(stack, FluidName.fluidhelium.getInstance().get());
    }

    private static Fluid templateOrActual(FluidStack stack, Fluid template) {
        if (stack.isEmpty()) {
            return stack.getFluid();
        }
        Fluid actual = stack.getFluid();
        if (actual == template) {
            return actual;
        }
        return UnifiedFluidRegistry.decide(MACHINE, template, actual) == UnifiedFluidRegistry.Decision.ACCEPT
                ? template
                : actual;
    }
}
