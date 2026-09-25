package com.sxcccccccc.fabrictransferfix;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Fabric Transfer API Forge 兼容层线程安全修复（本包自用 fix mod）。
 *
 * <p>修复目标：fabric-transfer-api（Fabric mod，经 Sinytra Connector 在 Forge 上运行）的
 * {@code TransferApiForgeCompat} 给每个方块实体挂 capability 桥，桥内部用
 * <b>静态普通 {@code HashMap}</b>（{@code CAPS}）缓存 Storage → LazyOptional 映射，无任何同步。
 * Create 管道每 tick 经 {@code FlowSource.FluidHandler.manageSource} 查询相邻方块实体的流体
 * capability，都会走 {@code CAPS.computeIfAbsent}；单人存档里 server 线程与渲染/worker 线程
 * 共享该 map，两线程同时放入新 Storage 时触发 JDK 的 modCount 检查 → {@code ConcurrentModificationException}
 * → 崩服。carpet tick warp（如 1000 倍速）把查询频率放大后必现。</p>
 *
 * <p>修复方式：见 {@link com.sxcccccccc.fabrictransferfix.mixin.TransferApiForgeCompatProviderMixin}，
 * 对 {@code CAPS} 仅有的两处 {@code computeIfAbsent} 调用点做 {@code synchronized(map)} 包裹，
 * 串行化全部访问，消除竞态。</p>
 */
@Mod("fabrictransferfix")
public class FabricTransferFix {

    private static final Logger LOGGER = LogUtils.getLogger();

    public FabricTransferFix() {
        LOGGER.info("Fabric Transfer API Forge Compat Fix loaded");
    }
}
