package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import com.denfop.componets.ComponentProcess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 电机器操作组件（几乎所有 IU 标准电机/流体机/特殊机共用）：
 * 构造完成后把 {@code operationLength / defaultOperationLength} 除以倍率。
 *
 * <p>运行时读点：{@code ComponentProcess.updateEntityServer()}
 * （desktop 源码 line 326）{@code this.componentProgress.getProgress() >= this.operationLength}
 * 才完成；超频/包络（{@code setOverclockRates} line 66-76）从
 * {@code defaultOperationLength} 重新派生，故两者都要缩。
 * 各具体机器类（重熔机 300、电路装配机 300、接口/杆制造 400、组装中心 800 etc.）
 * 及匿名子类（{@code new ComponentProcess(this, 300, 1) { ... }}）全部经由本类构造器
 * —— 一处注入，全族覆盖。
 */
@Mixin(value = ComponentProcess.class)
public abstract class ComponentProcessMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleOperationLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
