package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import com.denfop.componets.ProcessMultiComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 多胞（EnumMultiMachine，如大型组装/离心等多机格）操作组件。
 * 构造器按 {@code enumMultiMachine.lenghtOperation / getspeed()} 定长，
 * 完成判定 {@code progress[i] >= this.operationLength}
 * （desktop 源码 line 596）；升级/模块改动时由
 * {@code operationLength = upgradeSlot.getOperationLength1(defaultOperationLength)}
 * （line 638）重新派生——故 {@code defaultOperationLength} 同样必须缩。
 */
@Mixin(value = ProcessMultiComponent.class)
public abstract class ProcessMultiComponentMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleOperationLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
