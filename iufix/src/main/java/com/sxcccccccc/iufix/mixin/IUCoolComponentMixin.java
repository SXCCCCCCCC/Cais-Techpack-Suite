package com.sxcccccccc.iufix.mixin;

import com.denfop.componets.BufferEnergy;
import net.minecraft.core.Direction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用户裁决（1.7.0）：把 IU 冷却温度机制（CoolComponent 0-100 °C）接 iufix.json 配置
 * {@code cooling}——默认 true = 原版（温度会涨、到 100 °C 停机/挂死）；false = 关闭
 * 散热需求（被冷却端机器温度恒 0、永不停机）。
 *
 * <p>背景（研究结论）：「温度到 100 就停机」= CoolComponent 被冷却端系统，不是
 * HeatComponent 热网络（供热侧阈值 1000/5000），也非 cooldownEnabled（CooldownTracker
 * 过热停转计时器）。硬闸门：ProcessMultiComponent:565
 * {@code !(!upgrade && getEnergy() >= capacity)} 温度 ≥100 不作业；
 * fillRatio ≥1 → operationLength=Integer.MAX_VALUE 永挂（<0.75 ×2 / <0.5 ×1.5 减速）。
 * 升温 +0.15/操作次（离心机 ×2.5）+0.05/工作tick；流体冰箱门 {@code getEnergy()<100} 停闸。
 * IU 自带未接线的总开关 {@code CoolComponent.cooling}（addEnergy 内
 * {@code if (delegate instanceof ICoolSink) if (!cooling) storage = 0}，声明后无任何代码
 * 赋值），IuFixConfig（mod 构造器）把配置值写入该静态后，升温路径即时归零。
 *
 * <p>为何还要补注入（裁决要求，已核实）：① {@code readFromNbt}（CoolComponent.java:92）
 * 把 NBT 存储的 100 原样写回，不经过 addEnergy——区块重载后机器温度仍 100，且此时
 * delegate 尚未创建（instanceof ICoolSink 不成立），静态开关覆盖不到；② 上述情景下
 * 硬闸门先于任何 addEnergy 求值（updateEntityServer 顺序：先读 getEnergy 门控、后
 * addEnergy），机器永远停在 100。注入点：addEnergy RETURN / readFromNbt RETURN，
 * 仅作用于「被冷却端」（sinkDirections 非空 = MultiMachine 家族/流体冰箱；制冷机、
 * 反应堆冷却器等 source 端不动，反应堆冷却机制原样保留）。cooling=true 时两处注入
 * 完全 no-op，与原版逐字节一致。
 *
 * <p>取证：@Inject require=1（0 命中直接崩溃，不被 defaultRequire=0 静默吞）+ 一次性
 * warn 探针（首次清零各行打印一行，常驻留作「mixin 是否应用」回归判断）。
 */
@Mixin(targets = "com.denfop.componets.CoolComponent", remap = false)
public class IUCoolComponentMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    private static final AtomicBoolean PROBED = new AtomicBoolean(false);

    @Shadow(remap = false)
    public BufferEnergy buffer;

    @Shadow(remap = false)
    public Set<Direction> sinkDirections;

    @Shadow(remap = false)
    public static boolean cooling;

    @Inject(method = "addEnergy", at = @At("TAIL"), require = 1)
    private void iufix$coolAddEnergy(double amount, CallbackInfoReturnable<Double> cir) {
        if (!cooling && !this.sinkDirections.isEmpty()) {
            this.buffer.storage = 0;
            iufix$probe(amount);
        }
    }

    @Inject(method = "readFromNbt", at = @At("TAIL"), require = 1)
    private void iufix$coolReadFromNbt(net.minecraft.nbt.CompoundTag nbt, CallbackInfo ci) {
        if (!cooling && !this.sinkDirections.isEmpty()) {
            this.buffer.storage = 0;
            iufix$probe(0);
        }
    }

    private void iufix$probe(double amount) {
        if (PROBED.compareAndSet(false, true)) {
            LOGGER.warn("[iufix] IUCoolComponentMixin active: cooling disabled (iufix.json cooling=false); "
                    + "sink cold-storage zeroed at addEnergy/readFromNbt (first amount={}); "
                    + "sinkDirections={}", amount, this.sinkDirections);
        }
    }
}
