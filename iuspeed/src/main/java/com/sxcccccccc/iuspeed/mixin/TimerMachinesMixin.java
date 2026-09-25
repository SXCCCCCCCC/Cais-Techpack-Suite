package com.sxcccccccc.iuspeed.mixin;

import com.denfop.blockentity.mechanism.BlockEntityAlkalineEarthQuarry;
import com.denfop.blockentity.mechanism.BlockEntityGraphiteHandler;
import com.denfop.blockentity.mechanism.BlockEntityMatterFactory;
import com.denfop.blockentity.mechanism.BlockEntityMoonSpotter;
import com.denfop.componets.ComponentTimer;
import com.denfop.utils.Timer;
import com.sxcccccccc.iuspeed.IuSpeedConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <b>v1.4.0：剩余"自动工作型"计时机器的时限改造</b>（用户裁决：所有机器都想加速）。
 * 与 SiliconCrystalTimerMixin / ProgrammingTableTimerMixin 完全同型：
 * 各目标类<b>唯一构造器</b>{@code <init>(BlockPos, BlockState)} 的 RETURN 注入
 * （构造器注入禁 HEAD/self，RETURN + 纯 {@code (CallbackInfo)} 为项目验证过的
 * 经典形态），把构造完成的 {@link Timer} 的 hour/minute/seconds/max 四字段
 * 直写为 {@code 总秒数 / speed_multiplier}，{@code getTimers()} 与
 * {@code getDefaultTimers()} 两个列表必须同步改写（resetTime() 从 defaultTimers
 * 读回，getTimes() 进度条读 defaultTimers.getBar()），max 一并写
 * （getMax/getProgressBar/NBT 同步依赖）。每 tick 能耗原样、每 op 总能耗 ÷5，
 * 与项目约定一致。目标类运行时 jar 3.4.0.10 已核对：四类各自
 * {@code public final ComponentTimer timer} 自声明字段 + 唯一
 * {@code (BlockPos, BlockState)} 构造器；@Shadow 显式 {@code remap=false}
 * （v1.2.5 教训：remappable @Shadow 会被 0.8.5 整只拒载）。
 *
 * <p>四台机器逐个定案（桌面源码 + 运行时字节码）：
 * <ul>
 *   <li>{@code BlockEntityAlkalineEarthQuarry}（碱性采石场）：构造器 line 114
 *       {@code new ComponentTimer(this, new Timer(0, 0, 15))} 匿名子类
 *       line 115-119 {@code getTickFromSecond = max(1, 20 - level*1.9)}
 *       （等级加速语义保留），完成判定 updateEntityServer line 237
 *       {@code getTimers().get(0).getTime() <= 0}。15s → 3s @5x。</li>
 *   <li>{@code BlockEntityGraphiteHandler}（石墨炉）：构造器 line 65
 *       {@code new Timer(0, 1, 30)} 匿名 {@code getTickFromSecond = max(1, 20 -
 *       levelBlock*1.75)}（line 66-69），完成判定 line 207 同款
 *       {@code getTime() <= 0}；90s → 18s @5x。col（热量循坏，gameTime%40 递减、
 *       燧石 +30）是独立的弱周期维护字段，与 Timer 时长正交，不受影响。</li>
 *   <li>{@code BlockEntityMatterFactory}（物质工厂）：构造器 line 40
 *       {@code new Timer(0, 1, 0)}（无匿名，默认 20 tick/秒），完成判定
 *       updateEntityServer line 108 同款；60s → 12s @5x。setRecipeOutput/
 *       getOutput 的 resetTime()（line 124/129）读回的是已改写后的 defaultTimers。</li>
 *   <li>{@code BlockEntityMoonSpotter}（观月台）：构造器 line 58
 *       {@code new Timer(0, 0, 20)} 匿名 {@code getTickFromSecond = max(1, 20 -
 *       levelBlock*1.75)}（line 60-62），完成判定 line 194 同款；20s → 4s @5x。
 *       夜间限制（line 180 {@code isDay() → setCanWorkWithOut(false)}）是独立
 *       语义，只缩 Timer 基础时长、不碰昼夜门控。</li>
 * </ul>
 *
 * <p><b>不碰</b>（本 mixin 目标外，各自有专门处置/待用户裁决，见 IuSpeed.java：
 * 相位/天气调度机（太阳能板 8h/4h/4h、迷你板 3h/2h30/2h、雷击棒 5 分钟冷却）
 * 是昼夜/天气调度语义——缩相位会与实际太阳周期脱同步；PrimalPump 与 Primal
 * 三兄弟是玩家点击驱动。此四台只做基础时长替换，levelBlock 等级倍率语义
 * 原样叠加（与晶机/编程台一致）。
 */
@Mixin(value = {
        BlockEntityAlkalineEarthQuarry.class,
        BlockEntityGraphiteHandler.class,
        BlockEntityMatterFactory.class,
        BlockEntityMoonSpotter.class
}, remap = false)
public abstract class TimerMachinesMixin {

    @Shadow(remap = false)
    private ComponentTimer timer;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$shortenTimer(CallbackInfo ci) {
        int mult = IuSpeedConfig.SPEED_MULTIPLIER.get();
        if (mult > 1) {
            List<Timer> active = this.timer.getTimers();
            List<Timer> defaults = this.timer.getDefaultTimers();
            if (!active.isEmpty() && active.size() == defaults.size()) {
                for (int i = 0; i < active.size(); i++) {
                    int total = active.get(i).getTime() / mult;
                    if (total < 1) {
                        total = 1;
                    }
                    TimerDurationAccessor a = (TimerDurationAccessor) (Object) active.get(i);
                    TimerDurationAccessor d = (TimerDurationAccessor) (Object) defaults.get(i);
                    int h = total / 3600;
                    int m = (total % 3600) / 60;
                    int s = total % 60;
                    a.setHour(h);
                    a.setMinute(m);
                    a.setSeconds(s);
                    a.setMax(total);
                    d.setHour(h);
                    d.setMinute(m);
                    d.setSeconds(s);
                    d.setMax(total);
                }
            }
        }
    }
}
