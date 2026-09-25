package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import com.denfop.componets.BioProcessMultiComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 生物多胞（BlockEntityBioMultiMachine）操作组件。
 * 完成判定 {@code progress[i] >= this.operationLength}（desktop 源码 line 199）。
 */
@Mixin(value = BioProcessMultiComponent.class)
public abstract class BioProcessMultiComponentMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleOperationLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
