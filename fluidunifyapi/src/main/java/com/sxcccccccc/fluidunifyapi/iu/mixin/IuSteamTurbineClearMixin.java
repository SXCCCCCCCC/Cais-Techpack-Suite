package com.sxcccccccc.fluidunifyapi.iu.mixin;

import com.denfop.blockentity.mechanism.steamturbine.tank.BlockEntityBaseSteamTurbineTank;
import com.denfop.blocks.FluidName;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * IU 特殊机 1/2：汽轮机水箱 clear 例程——原生只清"精确 == 水 / 精确 == IU 蒸汽"
 * 的罐。补丁引入的跨 mod 同族流体（如 GT 蒸汽）也应当能被清罐（这是输出/排空
 * 侧动作，只做 ACCEPT 宽限，REPLACE 否决不适用）。
 *
 * <p>原方法体两个分支（steam=true 清 WATER / steam=false 清 fluidsteam）逐行
 * 等价重写 + 宽限；HEAD cancellable 整方法替换（inline == 无法 @Redirect）。</p>
 */
@Mixin(value = BlockEntityBaseSteamTurbineTank.class, remap = false)
public abstract class IuSteamTurbineClearMixin {

    private static final String MACHINE = "industrialupgrade:steam_turbine_tank";

    @Shadow(remap = false)
    private com.denfop.componets.Fluids.InternalFluidTank tank;

    @Inject(
            method = "clear",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void fluidunifyapi$clearWidened(boolean steam, CallbackInfo ci) {
        if (tank.getFluid().isEmpty()) {
            ci.cancel();
            return; // 原逻辑：空罐什么都不做
        }
        Fluid actual = tank.getFluid().getFluid();
        Fluid target = steam ? Fluids.WATER : FluidName.fluidsteam.getInstance().get();
        boolean accepted = actual == target
                || UnifiedFluidRegistry.decide(MACHINE, target, actual) == UnifiedFluidRegistry.Decision.ACCEPT;
        if (accepted) {
            tank.drain(tank.getFluidAmount(), IFluidHandler.FluidAction.EXECUTE);
        }
        ci.cancel();
    }
}
