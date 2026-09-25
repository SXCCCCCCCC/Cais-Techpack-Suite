package com.sxcccccccc.iuspeed.mixin;

import com.denfop.blockentity.mechanism.BlockEntityPrimalSiliconCrystalHandler;
import com.denfop.blockentity.mechanism.BlockEntitySiliconCrystalHandler;
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
 * <b>v1.2.6：晶体生长两台机器的直接时限改造（按用户裁决，放弃 percent 速率方案）。</b>
 *
 * <p>两台机器（原始 = 炉驱，3 分钟；GUI = 电驱，45 秒）在各自唯一构造器的 RETURN
 * 处，把构造完成的 {@link Timer} 的时长字段直接写为
 * {@code (原始总秒数 / speed_multiplier)}（默认 /5：180s → 36s、45s → 9s；
 * 1 = 不变）。没有任何"倍率/速率"机制——percent 恒为默认 1.0，timer.work(1)
 * 每秒递减 1，总时长从源头就是缩短后的值。
 *
 * <p><b>对象图（desktop 源码 ComponentTimer.java 逐行核对）：</b>
 * <ul>
 *   <li>{@code ComponentTimer} 构造器把调用方传入的<b>原始</b> Timer 实例放进
 *       {@code timers} 列表（Arrays.asList），并把这些原值的<b>克隆</b>存进
 *       {@code defaultTimers}；机器 tick 核心判完成用
 *       {@code timer.getTimers().get(0).getTime() <= 0}（两机器的 updateEntityServer
 *       各自确认）；</li>
 *   <li>{@code resetTime()}（setCanWork 转换、getOutput、purifier、
 *       {:code onLoaded} 等都会调）用 defaultTimers 克隆<b>读回</b>计时——因此只改
 *       活动 Timer 不会生效（resetTime 会把原值读回来）；<b>两个列表里的 Timer
 *       实例必须同步改写</b>，本 mixin 对 {@code getTimers()} 与
 *       {@code getDefaultTimers()} 全部改写；</li>
 *   <li>{@code max = h*3600+m*60+s} 也必须同步（{@code getMax()} /
 *       {@code getProgressBar()} 进度条 / NBT 写出的当前值）；Timer.readTimer
 *       不回写 max，活动 Timer 的 max 一经设置即为缩短值，resetTime 后 progress
 *       显示一致；</li>
 *   <li>GUI 机（{@code BlockEntitySiliconCrystalHandler}）的 timer 是 ComponentTimer
 *       匿名子类（getTickFromSecond 按 levelBlock 覆盖）——改的是其持有的共享
 *       Timer 实例，等级加速语义不受影响；</li>
 *   <li>其余 8 处 ComponentTimer 使用者（太阳能板组、雷击棒、编程台、观月台、
 *       石墨炉、物质厂、碱性采石场等）的 Timer 实例只字未动——mixin 只作用于
 *       这两台机器，accessor 接口方法对其它任何对象无副作用。</li>
 * </ul>
 *
 * <p>注入形态 = 项目已验证的经典形态（与 {@code ProgressLengthMachineMixin}
 * 完全同型：机器类自身唯一 {@code <init>} 的 {@code RETURN} + 纯
 * {@code (CallbackInfo)} handler；1.1.0 crash 栈
 * {@code handler$zzh000$iuspeed$scaleProgressLength ← <init>(.java:42)} 已证明
 * 该形态在运行环境可 APPLY 并执行）。
 *
 * <p><b>v1.2.5 的教训（留档）</b>：类 mixin 的 {@code @Shadow} 注解<b>必须显式写
 * {@code @Shadow(remap = false)}</b>——0.8.5 的
 * {@code org.spongepowered.asm.mixin.transformer.MixinInfo$State.validateRemappable}
 * 对 {@code remap} 取值默认<b>真</b>的 @Shadow（字段、方法皆然）一律抛
 * {@code InvalidMixinException: Found a remappable @Shadow annotation on ...}
 * （已反汇编运行时 mixin-0.8.5.jar 逐指令核实：默认值 Boolean.TRUE；为真即抛）。
 * 该异常发生在 post-initialise 的 mixin 面板校验，该 mixin 被<b>整体拒载</b>，
 * 而配置不因此崩溃——表现为：机器照常运行但加速零生效（1.2.5 实测两台均为
 * 原时长，日志留下 [main/ERROR] 一条）。本项目其余 mixin 原本不用 @Shadow，
 * 故 1.2.5 是第一个触发点。本修复在此以 {@code @Shadow(remap = false)} 显式覆盖。
 *
 * <p><b>用户指令要求查证的转向路径</b>（@ModifyArgs/@ModifyArg/@WrapOperation 改
 * {@code new Timer} 参数）：0.8.5 的 {@code ModifyArgsInjector} / {@code
 * ModifyArgInjector} 均带 {@code RestrictTargetLevel.ALLOW_ALL}（构造器调用可命中，
 * @Redirect 的 "Illegal @Redirect of constructor" 限制是 Redirect 自己所加，
 * 与 ModifyArgs 无关）；但 @ModifyArgs 依赖<i>合成 Args 类</i>（ArgsClassGenerator：
 * {@code org/spongepowered/asm/synthetic/args/...}），其 JPMS/ModLauncher 包注册缺陷
 * 对应 mixin issue #584（社区出现专修 mod，故障形态
 * {@code NoClassDefFoundError: org/spongepowered/asm/synthetic/args/Args$1}），
 * 在本环境 modlauncher-10.0.9 + JPMS 下风险未排除；@WrapOperation 属 MixinExtras
 * （本包虽已装载 0.5.3，但为 mod 外部依赖）。v1.2.6 采用<b>零合成类、规则已
 * 逐字节核实</b>的 accessor 方案（见 {@link TimerDurationAccessor}），
 * @ModifyArgs 路线留作备选。
 */
@Mixin(value = {
        BlockEntityPrimalSiliconCrystalHandler.class,
        BlockEntitySiliconCrystalHandler.class
}, remap = false)
public abstract class SiliconCrystalTimerMixin {

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
