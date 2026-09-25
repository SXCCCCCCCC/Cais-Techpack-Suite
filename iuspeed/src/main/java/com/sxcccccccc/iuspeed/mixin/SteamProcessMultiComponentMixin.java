package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import com.denfop.componets.SteamProcessMultiComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 蒸汽多胞（BlockEntitySteamMultiMachine）操作组件。
 * 完成判定 {@code progress[i] >= this.operationLength}（desktop 源码 line 174）。
 */
@Mixin(value = SteamProcessMultiComponent.class)
public abstract class SteamProcessMultiComponentMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleOperationLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
