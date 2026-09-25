package com.sxcccccccc.iuspeed.util;

import com.denfop.componets.ComponentProgress;
import com.denfop.utils.Timer;
import com.sxcccccccc.iuspeed.IuSpeed;
import com.sxcccccccc.iuspeed.IuSpeedConfig;
import com.sxcccccccc.iuspeed.mixin.TimerDurationAccessor;

import java.lang.reflect.Field;

/**
 * 操作时长缩放助手：把操作时长系字段除以倍率（仅整数，且不小于 1 tick）。
 *
 * <p>字段按名称分行处理（各机器类/组件类的字段集不同，缺省字段静默跳过）：
 * <ul>
 *   <li>{@code operationLength} —— 本周期完成判定用的操作时长（tick）；</li>
 *   <li>{@code defaultOperationLength} —— 超频/升级后重新派生的基准，必须同步缩；</li>
 *   <li>{@code operationChange} —— 多胞组件的当前基准（构造器与 operationLength 同值）。</li>
 * </ul>
 * <b>v1.1 新增</b> {@link #scaleProgress(Object)}：对<b>以 {@link ComponentProgress} 为
 * 操作时长核心</b>的机器（多方块冶炼炉 SmelteryFurnace/SmelteryCasting 的 108 tick、
 * AutoCrafter 的 100 tick），完成判定是 {@code getBar() >= 1}（即
 * {@code progress >= maxValue}）且<b>没有</b>每 tick 同步器，故缩 {@code maxValue}。
 * 不能做全局缩放：其余机器挂在机器上的 ComponentProgress 只是 GUI 显示器，
 * 其 maxValue 每 tick 被 {@code ComponentProcess} 同步回 {@code operationLength}，
 * 我们已缩该字段，若在构造时再缩 maxValue 会被同步覆盖，无害但徒劳；而
 * Primal 三兄弟（编程台/焊接台/电子组装台）的 progress 是玩家点击驱动的小游戏
 * 数值（300 目标），缩 maxValue 会破坏玩法——只在本 mixin 显式目标内调用。
 *
 * <p>每 tick 能耗（{@code energyConsume}）与任何 {@code defaultEnergyStorage}
 * 内部缓冲容量一律不动：只缩短时间，能耗语义保持"每 tick 不变"
 * （故每 op 总能耗按比例下降，与 GT duration/5 一致）。
 *
 * <p>为什么要反射而非 {@code @Shadow}：{@code defaultOperationLength} /
 * {@code operationChange} 在 IU 里是 <b>final</b> 实例字段，目标类的
 * {@code <init>} 在构造时就有值；在构造器 RETURN 时刻用反射
 * （{@code setAccessible(true)} 后写 final 非静态实例字段）改写，
 * 对象尚未逃离构造器，等价于构造期赋值，JVM 不校验 final 声明点之外写入
 * （反射路径下 Java 9+ 允许，唯一前提是同一 classloader 非模块化——本场景满足）。
 * 若用 {@code @Shadow} 则注入处理器是独立方法，对 final 字段 PUTFIELD
 * 会在运行时被 JVM 拒绝，故不可行。
 */
public final class IuSpeedHelper {

    private static final String[] SCALED_FIELDS = {
            "operationLength",
            "defaultOperationLength",
            "operationChange"
    };

    private IuSpeedHelper() {
    }

    /**
     * 在组件/机器 {@code <init>} 的 RETURN 前调用（{@code this} 尚未发布）。
     *
     * @param component 刚刚构造完成的实例（可能是匿名子类实例）
     */
    public static void scale(Object component) {
        int mult = IuSpeedConfig.SPEED_MULTIPLIER.get();
        if (mult <= 1) {
            return;
        }
        Class<?> cls = component.getClass();
        for (String name : SCALED_FIELDS) {
            try {
                Field f = findField(cls, name);
                if (f == null) {
                    continue; // 该类没有此字段（每个组件类字段集不同），跳过
                }
                f.setAccessible(true);
                if (f.getType() == Integer.TYPE) {
                    int old = f.getInt(component);
                    f.setInt(component, Math.max(1, old / mult));
                } else if (f.getType() == Double.TYPE) {
                    double old = f.getDouble(component);
                    f.setDouble(component, old / mult);
                }
            } catch (Throwable t) {
                IuSpeed.LOGGER.error("[iuspeed] {} {} 字段缩放失败", cls.getName(), name, t);
            }
        }
    }

    /**
     * v1.1：缩放机器上所有 {@link ComponentProgress} 实例的 {@code maxValue}
     * （按字段类型找，含父类字段；volatile/final 引用不影响——只改对象内状态）。
     * 仅由 {@code ProgressLengthMachineMixin} 对时长型机器调用。
     */
    public static void scaleProgress(Object machine) {
        int mult = IuSpeedConfig.SPEED_MULTIPLIER.get();
        if (mult <= 1) {
            return;
        }
        Class<?> cls = machine.getClass();
        int hit = 0;
        for (Class<?> k = cls; k != null; k = k.getSuperclass()) {
            for (Field f : k.getDeclaredFields()) {
                if (f.getType() != ComponentProgress.class) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    ComponentProgress cp = (ComponentProgress) f.get(machine);
                    if (cp == null) {
                        continue;
                    }
                    int old = cp.getMaxValue();
                    cp.setMaxValue((short) Math.max(1, old / mult));
                    hit++;
                } catch (Throwable t) {
                    IuSpeed.LOGGER.error("[iuspeed] {} ComponentProgress 字段 {} 缩放失败",
                            cls.getName(), f.getName(), t);
                }
            }
        }
        if (hit == 0) {
            IuSpeed.LOGGER.warn("[iuspeed] {} 上没有找到 ComponentProgress 字段（时长可能未缩放，需查）",
                    cls.getName());
        } else {
            IuSpeed.LOGGER.info("[iuspeed] {}: 缩放 {} 个 ComponentProgress.maxValue → 默认 {}x",
                    cls.getName(), hit, mult);
        }
    }

    /**
     * v1.5.0：太空系统任务时长缩放。{@link Timer}（{@code com.denfop.utils.Timer}）
     * 是 FakePlanet/FakeSatellite/FakeAsteroid（研究/开采任务去程+回程）与
     * Sends（殖民地运输）共用的倒计时核心：时长在构造器里按"距离×基准×加成"算好，
     * 剩余时间 = getTime()，max = 总时长（进度条 getBar() = time/max 现算）。
     * 改写四字段（hour/minute/seconds/max）为 当前值/mult——进度条与完成判定
     * （getTime()&lt;=0 / canWork()）自动按新时长一致，GUI 无需另改。
     * 在任务对象构造器 RETURN 时调用（新任务与 NBT 读档任务都命中），
     * 引擎模块/燃料等级/殖民地等级等加成语义保留（它们作用在构造前的 seconds 计算上）。
     * 每 tick 能耗不变（太空任务的 energy 消耗链在 FakeSpaceSystemBase.manageEnergy，
     * 按 tick 扣，与本缩放无关）。
     */
    public static void scaleTimer(Timer timer) {
        // 太空任务倍率：用户裁决固定 10 倍（2026-09-03），不随全局 speed_multiplier。
        final int mult = 10;
        if (timer == null) {
            return;
        }
        int total = timer.getTime() / mult;
        if (total < 1) {
            total = 1;
        }
        TimerDurationAccessor a = (TimerDurationAccessor) (Object) timer;
        a.setHour(total / 3600);
        a.setMinute((total % 3600) / 60);
        a.setSeconds(total % 60);
        a.setMax(total);
    }

    private static Field findField(Class<?> cls, String name) {
        for (Class<?> k = cls; k != null; k = k.getSuperclass()) {
            try {
                return k.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // 上级类试试
            }
        }
        return null;
    }
}
