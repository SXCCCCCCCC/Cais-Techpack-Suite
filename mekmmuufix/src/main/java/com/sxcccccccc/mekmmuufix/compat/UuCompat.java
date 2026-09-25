package com.sxcccccccc.mekmmuufix.compat;

import com.jerry.mekmm.common.tile.factory.TileEntityReplicatingFactory;
import com.jerry.mekmm.common.tile.machine.TileEntityFluidReplicator;
import com.jerry.mekmm.common.tile.machine.TileEntityReplicator;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * IC2 UU 成本桥接：把 mekmm 复制机 / 流体复制机 / 复制工厂的静态配方表
 * （{@code customRecipeMap}，原本由 mekmm config 列表生成）整体替换为
 * IC2 的 UU 复制成本体系计算出的内容。
 *
 * <h2>IC2 成本来源（javap 核实 ic2_120-0.5 jar）</h2>
 * <ul>
 *   <li>{@code ic2_120.config.Ic2Config.INSTANCE.getReplicationCostUb(String): Integer} ——
 *       唯一权威查询入口（IC2 自己的 UU 扫描仪 / 复制机都走它）：
 *       ① 白名单手工定价（UuReplicationDefaults，硬约束）→ ② 动态启发式计算
 *       （UuCostIndex → UuGraph Bellman-Ford 最短路，基于全服配方图回溯到白名单叶节点），
 *       null = 不可复制。</li>
 *   <li>返回单位 uB（micro-bucket）：1 mB UU = 1000 uB；1 uB = 1000 EU。</li>
 *   <li>{@code ic2_120.content.uu.UuCostIndex.INSTANCE.isReady(): boolean} ——
 *       动态索引是否构建完成（SERVER_STARTED 时重建）。未 ready 时 getReplicationCostUb
 *       只回白名单，所以填充逻辑按 readiness 状态做「白名单版 → 全量版」的两段填充。</li>
 * </ul>
 *
 * <h2>与 mekmm 的换算（1:1 语义）</h2>
 * 整合包 rotary 桥为 1:1（1 mB mekmm UU 气体 = 1 mB IC2 UU 流体，KubeJS 侧）。
 * 故：IC2 成本 X uB = X/1000 mB IC2 流体（1 mB = 1000 uB，与 IC2 复制机液滴消耗公式
 * droplets = uB×BUCKET/1e6 交叉验证一致）≡ X/1000 mB mekmm 气体。
 * 物品复制机：复制 1 个物品消耗 N mB UU 气体，N = ceil(成本uB / 1000)，最少 1——
 * 与 IC2 流体消耗量 1:1 对应，无任何额外比率假设。
 * 流体复制机：复制 1000 mB 流体消耗 N mB，N = 该流体桶物品（Fluid#getBucket）的 IC2 成本
 * （IC2 启发式只给物品定价，流体以其桶物品价格作为代理）。无桶物品不可复制。
 *
 * <h2>90B 上限截断</h2>
 * 成本 &gt; 90_000_000 uB（= 90_000 mB = 90B）的物品直接跳过，禁止复制，
 * 防止套娃配方成本爆炸（IC2 白名单最贵条目 346,466,802 uB 也会被截掉）。
 *
 * <h2>填充时机</h2>
 * 懒填充 + 有状态重填：各查询方法（isValidXxxInput / getRecipe / JEI 注册）HEAD 注入
 * ensureXxx()。IC2 索引未 ready 时先填白名单版；ready 后再重填一次全量版。
 * 每次 ServerStartedEvent 重置标志（新世界 / 重进存档时 IC2 索引会重建）。
 */
public final class UuCompat {

    /** 90B 上限，单位 uB：90,000 mB × 1000 uB/mB。包作者游戏内实测终极复制工厂填充上限 = 90B。 */
    public static final long MAX_UU_COST_UB = 90_000_000L;
    /** uB → mB 换算：1 mB = 1000 uB */
    private static final long UB_PER_MB = 1000L;

    /** 状态锁（填充可能发生在服务端 tick 线程与客户端 JEI 线程） */
    private static final Object LOCK = new Object();

    private static final Logger LOGGER = LoggerFactory.getLogger("mekmmuufix");

    private static volatile boolean itemMapsFilled = false;
    private static volatile boolean fluidMapFilled = false;
    /** 上次填充时 IC2 索引是否 ready（ready 状态翻转时需重填全量版） */
    private static volatile boolean lastItemFillIndexReady = false;
    private static volatile boolean lastFluidFillIndexReady = false;

    private UuCompat() {
    }

    /**
     * Forge ServerStartedEvent：IC2 索引在新服务器启动时重建，重置填充标志，
     * 下次查询时按新索引重填。同时做一次尽力而为的预热。
     */
    public static void onServerStarted() {
        synchronized (LOCK) {
            itemMapsFilled = false;
            fluidMapFilled = false;
            lastItemFillIndexReady = false;
            lastFluidFillIndexReady = false;
        }
        ensureItemMaps();
        ensureFluidMap();
    }

    /** 保证物品复制机（TileEntityReplicator）与复制工厂（TileEntityReplicatingFactory）的配方表已按 IC2 成本填充。 */
    public static void ensureItemMaps() {
        synchronized (LOCK) {
            boolean ready = isIc2IndexReady();
            if (itemMapsFilled && ready == lastItemFillIndexReady) {
                return;
            }
            Map<String, Integer> itemMap = buildItemMap();
            // 与 mekmm 约定一致：空表 = null = 无配方
            TileEntityReplicator.customRecipeMap = itemMap.isEmpty() ? null : new HashMap<>(itemMap);
            TileEntityReplicatingFactory.customRecipeMap = TileEntityReplicator.customRecipeMap == null
                    ? null : new HashMap<>(itemMap);
            itemMapsFilled = true;
            lastItemFillIndexReady = ready;
            LOGGER.info("[mekmmuufix] IC2 UU item recipe table filled: {} entries (indexReady={})", itemMap.size(), ready);
        }
    }

    /** 保证流体复制机的配方表已填充。 */
    public static void ensureFluidMap() {
        synchronized (LOCK) {
            boolean ready = isIc2IndexReady();
            if (fluidMapFilled && ready == lastFluidFillIndexReady) {
                return;
            }
            Map<String, Integer> fluidMap = buildFluidMap();
            TileEntityFluidReplicator.customRecipeMap = fluidMap.isEmpty() ? null : new HashMap<>(fluidMap);
            fluidMapFilled = true;
            lastFluidFillIndexReady = ready;
            LOGGER.info("[mekmmuufix] IC2 UU fluid recipe table filled: {} entries (indexReady={})", fluidMap.size(), ready);
        }
    }

    /** 枚举全注册表物品，按 IC2 成本生成 mekmm 物品复制配方表（itemId -> mB UU 气体）。 */
    private static Map<String, Integer> buildItemMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item == Items.AIR) {
                continue;
            }
            String id = ForgeRegistries.ITEMS.getKey(item).toString();
            Integer costUb = queryIc2CostUb(id);
            if (costUb == null || costUb <= 0 || costUb > MAX_UU_COST_UB) {
                continue; // 不可复制 / 无成本 / 超过 90B 上限（截断禁止复制）
            }
            map.put(id, ubToMb(costUb));
        }
        return map;
    }

    /** 枚举全注册表流体，以「桶物品的 IC2 成本」为代理生成流体复制配方表（fluidId -> mB UU 气体 / 1000mB）。 */
    private static Map<String, Integer> buildFluidMap() {
        Map<String, Integer> map = new HashMap<>();
        for (Fluid fluid : ForgeRegistries.FLUIDS.getValues()) {
            Item bucket = fluid.getBucket();
            if (bucket == null || bucket == Items.AIR) {
                continue; // 无桶流体没有物品成本代理，不可复制
            }
            String bucketId = ForgeRegistries.ITEMS.getKey(bucket).toString();
            Integer costUb = queryIc2CostUb(bucketId);
            if (costUb == null || costUb <= 0 || costUb > MAX_UU_COST_UB) {
                continue;
            }
            String fluidId = ForgeRegistries.FLUIDS.getKey(fluid).toString();
            map.put(fluidId, ubToMb(costUb));
        }
        return map;
    }

    /** uB → mB：向上取整，最少 1 mB（保证消耗不低于 IC2 成本）。 */
    private static int ubToMb(int costUb) {
        long mb = (costUb + UB_PER_MB - 1) / UB_PER_MB;
        return (int) Math.max(1L, mb);
    }

    /**
     * 反射调用 IC2 {@code Ic2Config.INSTANCE.getReplicationCostUb(String)}。
     * IC2 类经 Connector 运行时仅重映射 MC 引用，自身类名/方法名不变；
     * 形参 String 与返回 Integer 均为纯 Java 类型，反射签名稳定。
     *
     * @return 成本（uB），或 null（不可复制 / IC2 未装 / 版本变化）
     */
    public static Integer queryIc2CostUb(String itemId) {
        try {
            Class<?> cls = Class.forName("ic2_120.config.Ic2Config");
            Object instance = cls.getField("INSTANCE").get(null);
            Method m = cls.getDeclaredMethod("getReplicationCostUb", String.class);
            m.setAccessible(true);
            return (Integer) m.invoke(instance, itemId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 反射查询 IC2 动态成本索引是否构建完成。失败（IC2 未装）返回 false。 */
    private static boolean isIc2IndexReady() {
        try {
            Class<?> cls = Class.forName("ic2_120.content.uu.UuCostIndex");
            Object instance = cls.getField("INSTANCE").get(null);
            Method m = cls.getDeclaredMethod("isReady");
            return Boolean.TRUE.equals(m.invoke(instance));
        } catch (Exception e) {
            return false;
        }
    }
}
