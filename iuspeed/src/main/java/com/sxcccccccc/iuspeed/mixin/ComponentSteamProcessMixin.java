package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import com.denfop.componets.ComponentSteamProcess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 蒸汽机操作组件（如 BlockEntitySteamHandlerHeavyOre，
 * ctor 里 {@code new ComponentSteamProcess(this, (int)(300 / this.getSpeed()), 1, ...)}）。
 * 完成判定 {@code componentProgress.getProgress() >= this.operationLength}
 * （desktop 源码 line 209）。
 */
@Mixin(value = ComponentSteamProcess.class)
public abstract class ComponentSteamProcessMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleOperationLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
