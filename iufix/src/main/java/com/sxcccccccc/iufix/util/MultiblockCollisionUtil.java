package com.sxcccccccc.iufix.util;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blockentity.collision.BlockEntityCollisionProxy;
import com.denfop.blocks.BlockCollisionProxy;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 1.4.0：多胞碰撞代理层的全维度清扫工具（IE 模式代理层重构的一部分）。
 *
 * <p>静态注册表：BlockEntityCollisionProxy 构造时自注册（见
 * BlockEntityCollisionProxyIEModeMixin 的 {@code <init>} TAIL 注入）。
 * 1.20.1 的 Level/LevelChunk 均无「已加载 BE 全量可枚举」的公开 API
 * （Level 只有 TickingBlockEntity 列表，LevelChunk 的 BE map 是私有的），
 * 因此用注册表 + 惰性剪枝（扫描时移除已失效条目）替代，不触碰任何原版类。
 *
 * <p>清扫规则（与 IE 的 posInMB 推导模型一致，全部基于「推导出的 master 位」判定）：
 * <ul>
 * <li>死指针：masterPos == null，或 masterPos 处的 TE 不是 BlockEntityBase
 *     （旧世界数据里可能指向空气/代理/其他方块）——回收该代理方块；</li>
 * <li>过期代理：masterPos == 正在 refresh 的 master 位，但该 cell 不在 desired
 *     集合里（历史污染里多出来的代理）——回收；desired 传空集表示全部回收。</li>
 * </ul>
 */
public final class MultiblockCollisionUtil {

    private MultiblockCollisionUtil() {
    }

    /** 全部已加载代理（双端共用；引用身份去重，避免误伤 equals 覆写）。 */
    private static final Set<BlockEntityCollisionProxy> PROXIES =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));

    /** 代理构造时自注册（客户端、服务端各自注册各自实例）。 */
    public static void register(BlockEntityCollisionProxy proxy) {
        PROXIES.add(proxy);
    }

    /**
     * 全维度清扫：遍历注册表里属于 {@code level} 的代理，
     * 回收死指针；若 {@code masterPos} 非空，还回收「指向它但不在 desired 里的过期代理」。
     * 返回实际移除的方块数。仅服务端调用（客户端不拆方块）。
     */
    public static int sweep(Level level, BlockPos masterPos, Set<BlockPos> desired) {
        int removed = 0;
        synchronized (PROXIES) {
            Iterator<BlockEntityCollisionProxy> it = PROXIES.iterator();
            while (it.hasNext()) {
                BlockEntityCollisionProxy proxy = it.next();
                if (proxy.getLevel() != level) {
                    continue;
                }
                if (proxy.isRemoved()) {
                    it.remove();
                    continue;
                }
                BlockPos mp = proxy.getMasterPos();
                if (mp == null) {
                    if (removeProxyBlock(level, proxy.getBlockPos())) {
                        removed++;
                    }
                    it.remove();
                    continue;
                }
                if (!isHealthyMaster(level, mp)) {
                    // 1.4.3-hotfix2 警告分级：清扫回收是预期内的自我修复，静默。
                    if (removeProxyBlock(level, proxy.getBlockPos())) {
                        removed++;
                    }
                    it.remove();
                    continue;
                }
                if (masterPos != null && mp.equals(masterPos) && desired != null && !desired.contains(proxy.getBlockPos())) {
                    if (removeProxyBlock(level, proxy.getBlockPos())) {
                        removed++;
                    }
                    it.remove();
                }
            }
        }
        return removed;
    }

    /**
     * 反污染命令用：整维度清扫死指针（不清理指向有效 master 的过期代理——那是 refresh 的职责）。
     * 返回 [死指针数, 健康代理数]；{@code dryRun} 时只计数不拆方块。
     */
    public static int[] cleanDimension(Level level, boolean dryRun) {
        int dead = 0;
        int healthy = 0;
        synchronized (PROXIES) {
            Iterator<BlockEntityCollisionProxy> it = PROXIES.iterator();
            while (it.hasNext()) {
                BlockEntityCollisionProxy proxy = it.next();
                if (proxy.getLevel() != level) {
                    continue;
                }
                if (proxy.isRemoved()) {
                    it.remove();
                    continue;
                }
                BlockPos mp = proxy.getMasterPos();
                if (mp == null || !isHealthyMaster(level, mp)) {
                    dead++;
                    if (!dryRun && removeProxyBlock(level, proxy.getBlockPos())) {
                        it.remove();
                    }
                } else {
                    healthy++;
                }
            }
        }
        return new int[]{dead, healthy};
    }

    /** 健康校验：master 位的 TE 存在且是 BlockEntityBase。调用方需保证 chunk 已加载（见 load 期守卫）。 */
    public static boolean isHealthyMaster(Level level, BlockPos masterPos) {
        return level != null && masterPos != null
                && level.getBlockEntity(masterPos) instanceof BlockEntityBase;
    }

    /** 该位方块仍是代理方块才移除（防止注册表残留把后来放的其他方块误拆）。 */
    private static boolean removeProxyBlock(Level level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof BlockCollisionProxy) {
            level.removeBlock(pos, false);
            return true;
        }
        return false;
    }
}
