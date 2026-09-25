package com.sxcccccccc.iuspeed.mixin;

import com.denfop.api.space.fakebody.FakeAsteroid;
import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.5.0：小行星任务（FakeAsteroid）去程+回程时长 10 倍速。与
 * {@link FakePlanetTimerMixin} 完全同型：时长
 * {@code 距离比*12*60*0.5 + 距太阳系*60*60} 叠引擎/燃料加成后
 * {@code new Timer(seconds)} ×2（去/回），两个构造器（UUID 新任务 /
 * CompoundTag 读档）RETURN 时 Timer 四字段 /10。
 * 细节见 FakePlanetTimerMixin javadoc。
 */
@Mixin(value = FakeAsteroid.class, remap = false)
public abstract class FakeAsteroidTimerMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleAsteroidTimers(CallbackInfo ci) {
        FakeAsteroid self = (FakeAsteroid) (Object) this;
        IuSpeedHelper.scaleTimer(self.getTimerTo());
        IuSpeedHelper.scaleTimer(self.getTimerFrom());
    }
}
