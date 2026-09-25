package com.sxcccccccc.fabrictransferfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Function;

/**
 * 修复 fabric-transfer-api Forge 兼容层 {@code TransferApiForgeCompat.CAPS}
 * 静态 {@code HashMap} 的线程安全竞态。
 *
 * <p>背景：{@code CAPS} 是 {@code TransferApiForgeCompat} 里的
 * {@code static final Map<Storage<?>, LazyOptional<?>> CAPS = new HashMap<>()}，
 * 供所有方块实体的 capability 桥（本类，即 {@code TransferApiForgeCompat$1}）共享。
 * 本类的 {@code getCapability} 在命中 ITEM_HANDLER / FLUID_HANDLER 时先
 * {@code find()} 出 Storage，再 {@code CAPS.computeIfAbsent(storage, LazyOptional::of)} 缓存。
 * mapping 函数纯构造无副作用（已反编译确认），但 JDK 的 {@code HashMap.computeIfAbsent}
 * 会在 apply 前后校验 modCount——单人存档里 server 线程（Create 管道 tick 查询相邻方块实体）
 * 与渲染/worker 线程并发放入新 Storage 时，modCount 变化 → {@code ConcurrentModificationException}
 * → 崩服。carpet tick warp 放大 tick 频率后必现。</p>
 *
 * <p>修复：对 {@code CAPS} 仅有的两处 {@code computeIfAbsent} 调用（物品路径 line 81、
 * 流体路径 line 89）做 {@code synchronized(map)} 包裹，串行化全部访问。map 极小
 * （每方块实体一条缓存），锁开销可忽略。</p>
 *
 * <p>实现说明：目标类是 Connector 重映射后的 Fabric jar（对 MC 的引用已变 SRG 名，
 * fabric 自身类名/方法名运行时不变），经 Sinytra Connector 运行。用 {@code targets}
 * 字符串 + {@code @Coerce}，不直接引用 fabric 类型。</p>
 */
@Mixin(targets = "net.fabricmc.fabric.impl.transfer.compat.TransferApiForgeCompat$1", remap = false)
public abstract class TransferApiForgeCompatProviderMixin {

    /**
     * 把 {@code CAPS.computeIfAbsent} 换成持锁版本：同一把 map 锁串行化所有访问，
     * apply 期间 map 不可能被结构性修改，modCount 检查不再触发 CME。
     */
    @Redirect(
            method = "getCapability",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Map;computeIfAbsent(Ljava/lang/Object;Ljava/util/function/Function;)Ljava/lang/Object;"
            ),
            require = 2,
            remap = false
    )
    // 注意：handler 必须是非 static——Mixin 要求 handler 的 static 修饰与目标方法一致，
    // getCapability 是实例方法，static handler 会在注入校验阶段直接报
    // "'static' modifier of handler method does not match target"。
    private Object fabrictransferfix$syncComputeIfAbsent(Map map, @Coerce Object key, Function mappingFunction) {
        synchronized (map) {
            return map.computeIfAbsent(key, mappingFunction);
        }
    }
}
