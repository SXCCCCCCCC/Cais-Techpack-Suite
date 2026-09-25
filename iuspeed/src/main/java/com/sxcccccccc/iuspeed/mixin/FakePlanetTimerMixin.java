package com.sxcccccccc.iuspeed.mixin;

import com.denfop.api.space.fakebody.FakePlanet;
import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.5.0：太空行星任务（研究/开采，FakePlanet）去程+回程时长 10 倍速。
 *
 * <p>时长定案（桌面源码 + 运行时 javap 实证）：构造器里
 * {@code seconds = 距离比 * (12*60*0.5) + 距太阳系距离*60*60}，再叠引擎模块
 * （每级 -12.5%）与火箭燃料等级（除 coef），最终
 * {@code timerToPlanet = timerFromPlanet = new Timer(seconds)}——用户实测"研究台
 * 进度条+火箭来回"就是这两个 Timer（getTime() 剩余 / max 总长，GUI getBar()
 * 现算）。本 mixin 在两个构造器（UUID 新任务 / CompoundTag 读档）RETURN 时把
 * 已构造的 Timer 四字段（hour/minute/seconds/max）直接 /10（IuSpeedHelper.scaleTimer，
 * 固定 10 倍不随全局 speed_multiplier）——与"new Timer(seconds/10)"数学等价，
 * 且读档的在途任务同样变快；引擎/燃料加成作用于构造前的 seconds 计算，语义保留。
 * @Redirect 构造器（new Timer 调用）是项目红线；@ModifyVariable 需数 ordinal 且
 * 覆盖不到 NBT 构造器——故取构造器 RETURN + TimerDurationAccessor（硅晶机/编程台
 * 同款已验证手法）。
 *
 * <p>每 tick 能耗不变：太空任务能量链在 FakeSpaceSystemBase.manageEnergy 按 tick
 * 扣，与本缩放无关（时间越短总能耗越少，与项目其他加速一致）。
 */
@Mixin(value = FakePlanet.class, remap = false)
public abstract class FakePlanetTimerMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scalePlanetTimers(CallbackInfo ci) {
        FakePlanet self = (FakePlanet) (Object) this;
        IuSpeedHelper.scaleTimer(self.getTimerTo());
        IuSpeedHelper.scaleTimer(self.getTimerFrom());
    }
}
