package com.sxcccccccc.iuspeed.mixin;

import com.denfop.api.space.colonies.Sends;
import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.5.0：殖民地运输（Sends）时长 10 倍速。时长
 * {@code seconds = 距离比 * (16.66*60*0.8)}（卫星分支 5*60*0.8 + 行星段），
 * 最终 {@code timerToPlanet = new Timer(seconds / (4 + dop))}（dop=殖民地等级/10）。
 * 两个构造器（UUID 新建 / CompoundTag 读档）RETURN 时 Timer 四字段 /10
 * （IuSpeedHelper.scaleTimer，固定 10 倍不随全局 speed_multiplier）——与
 * "new Timer 前 seconds/10" 数学等价；殖民地等级加成作用于构造前的 seconds，
 * 语义保留。细节见 FakePlanetTimerMixin javadoc。
 */
@Mixin(value = Sends.class, remap = false)
public abstract class SendsTimerMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleSendsTimer(CallbackInfo ci) {
        Sends self = (Sends) (Object) this;
        IuSpeedHelper.scaleTimer(self.getTimerToPlanet());
    }
}
