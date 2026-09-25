package com.sxcccccccc.iuspeed.mixin;

import com.denfop.utils.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 供 {@link SiliconCrystalTimerMixin} 直接改写已完成构造的 {@link Timer} 的
 * 时长字段。setter 由 Mixin 生成<b>在 Timer 类内部</b>（见 AccessorInfo.initType：
 * void 返回 → FIELD_SETTER；对任意可见性非 final 字段生效，
 * AccessorGeneratorFieldSetter.generate 生成本类内 PUTFIELD——包级/私有字段
 * 跨类访问问题在本类内部生成时不存在）。
 *
 * <p>字段均为 Timer 自身声明的 {@code private int}（非 final，运行时 jar 已核对）。
 * hour / minute / seconds 是 {@code Timer(int, int, int)} 的主倒计时三件套，
 * max = h*3600 + m*60 + s（{@link Timer#getMax()} 与进度条用）。
 * 三个时长字段与 max 必须同步改写，进度条/完成判定才一致。
 */
@Mixin(value = Timer.class, remap = false)
public interface TimerDurationAccessor {

    @Accessor("hour")
    void setHour(int hour);

    @Accessor("minute")
    void setMinute(int minute);

    @Accessor("seconds")
    void setSeconds(int seconds);

    @Accessor("max")
    void setMax(int max);
}
