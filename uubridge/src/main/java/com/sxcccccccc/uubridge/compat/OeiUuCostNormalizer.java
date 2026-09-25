package com.sxcccccccc.uubridge.compat;

import com.mafuyu404.oneenoughitem.init.ItemReplacementCache;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * OEI 统一后 UU 成本归一化后处理（0.1.3 起，用户裁决硬绑定 OEI 前置依赖）。
 *
 * <p>问题：IC2 成本是"每注册 id 一价"（白名单手定价 + 全包配方 Bellman-Ford 动态价），
 * OEI 把一族物品（如 A/B/C → D）统一成同一视觉物品后，复制表里出现一堆同名不同价
 * 的条目（成本多态）。本类在 OEI 规则装载完成 <b>且</b> IC2 动态索引构建完成之后
 * 做一次后处理：
 * <ol>
 *   <li>经 OEI 公开 API {@code ItemReplacementCache}（OEI 已解析/校验过
 *       {@code oei|oef|oeb:replacements/*.json} 并维护运行时缓存，不重复读数据包）
 *       枚举全部替换族：遍历注册表，{@code isSourceItemId(id)} 为真则
 *       {@code matchItem(id)} 得罐体 id，按罐体分组。</li>
 *   <li>对每个族的全部成员（源 + 罐体）<b>先快照</b> IC2 原始成本
 *       （{@link UuCostFiller#queryCostUb}），剔除 null/&le;0；全族无价的跳过
 *       （保持不可复制）；有价取 min；一个 id 跨多个族时全局再取 min。</li>
 *   <li>统一写入：反射向 IC2 运行时白名单
 *       {@code Ic2Config.current.getUuReplication().getReplicationWhitelist()}
 *       {@code put(id, min)}——服务端查价序 synced(null)→白名单→动态，白名单在
 *       动态之前且 {@code prettyAllReplicationCosts()} 也读白名单，一处写入服务端
 *       扣费/重发包内容全部生效（不写盘，重启幂等重算）。</li>
 *   <li>经 IC2 自带通道重发成本表：反射
 *       {@code ConfigSyncHelper.INSTANCE.sendToPlayer(player, REPLICATION_COSTS_ID,
 *       prettyAllReplicationCosts())}（登入时 Ic2_120 同款调用），客户端
 *       syncedReplicationCosts 覆盖为归一化版。</li>
 *   <li>发 {@code UuBridgeNetwork} 重填包 + 服务端 {@code UuCostFiller.forceRefill()}，
 *       两端 IU replicator 表重填，多态消除。</li>
 * </ol>
 *
 * <p>时序（latest.log 实测）：OEI 规则装载在资源加载期（19:39:33）；Forge
 * {@code ServerStartedEvent} 时 IC2 索引尚未 ready（19:39:50 uubridge 只填了白名单
 * 版）；fabric {@code SERVER_STARTED} 重建索引在其后（19:40:53 前 ready=true）。
 * 因此初始执行不用 {@code ServerStartedEvent} 直跑，而是置 pending 后由
 * {@code ServerTickEvent} 轮询 {@code UuCostIndex.isReady()} 首次为真时执行一次。
 * {@code /reload}（服务端 SERVER_DATA_LOAD）后 OEI 规则/白名单（reloadOrThrow 从盘
 * 重读会冲掉内存写入）可能变化 → 在服务端 TagsUpdatedEvent 重跑（幂等：min 对已
 * min 化的值再取 min 结果不变）。
 *
 * <p>反射链（IC2 是 Kotlin object，Connector 运行时类名/方法名不变，与
 * {@link UuCostFiller} 同款手法）：INSTANCE 字段 → getCurrent/getUuReplication/
 * getReplicationWhitelist/prettyAllReplicationCosts 公开 getter。
 */
@Mod.EventBusSubscriber(modid = "uubridge", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class OeiUuCostNormalizer {

    private static final Logger LOGGER = LoggerFactory.getLogger("uubridge");

    /** 后处理开关（uubridge 0.1.3 起默认开；本 mod 即 OEI×IC2×IU 定制，不做配置面）。 */
    private static final boolean ENABLED = true;

    /** ServerStartedEvent 后置 pending，等待 IC2 动态索引 ready。 */
    private static volatile boolean pending = false;
    /** 本次会话已执行过初始归一化（/reload 重跑不受此标志限制）。 */
    private static volatile boolean done = false;

    private OeiUuCostNormalizer() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        pending = true;
        done = false;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!ENABLED || !pending || done) {
            return;
        }
        if (!UuCostFiller.indexReady()) {
            return; // fabric SERVER_STARTED 重建索引晚于 Forge ServerStartedEvent
        }
        pending = false;
        run(event.getServer());
        done = true;
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (!ENABLED || !done) {
            return;
        }
        // /reload 场景重跑：OEI 规则重载 + Ic2Config.reloadOrThrow 冲掉白名单内存写入
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            run(null);
        }
    }

    /** 核心：收族 → 快照 → min → 白名单写入 → 刷 ① 同步表 → 重发客户端 → IU 表重填。 */
    private static void run(MinecraftServer server) {
        Map<String, Set<String>> families;
        try {
            families = collectFamilies();
        } catch (Throwable t) {
            // OEI 缺失（类加载失败）：静默降级，UU 链退回未归一化状态
            LOGGER.warn("[uubridge] OEI cost normalization failed (OEI absent?), skipped: {}", t.toString());
            return;
        }
        if (families.isEmpty()) {
            LOGGER.info("[uubridge] OEI cost normalization: no item replacement families, skipped");
            return;
        }

        // 先全量快照再写入，避免跨族顺序污染（一个 id 属多个族时快照恒为原始值）
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        for (Set<String> members : families.values()) {
            for (String id : members) {
                snapshot.putIfAbsent(id, UuCostFiller.queryCostUb(id));
            }
        }

        // 族内 min（无价成员剔除，唯部分成员无价不影响）；跨族全局按 id 再取 min
        Map<String, Integer> overrides = new LinkedHashMap<>();
        for (Set<String> members : families.values()) {
            int familyMin = Integer.MAX_VALUE;
            for (String id : members) {
                Integer cost = snapshot.get(id);
                if (cost != null && cost > 0) {
                    familyMin = Math.min(familyMin, cost);
                }
            }
            if (familyMin == Integer.MAX_VALUE) {
                continue; // 全族无价：保持不可复制
            }
            for (String id : members) {
                overrides.merge(id, familyMin, Math::min);
            }
        }

        if (overrides.isEmpty()) {
            LOGGER.info("[uubridge] OEI cost normalization: {} families, no costed members", families.size());
            return;
        }

        // ① 写入 IC2 运行时白名单（内存，不写盘）
        try {
            Map<String, Integer> whitelist = ic2Whitelist();
            int changed = 0;
            for (Map.Entry<String, Integer> e : overrides.entrySet()) {
                Integer old = whitelist.get(e.getKey());
                // 注意 Integer 数值比较（引用比较恒判不等，changed 计数会虚高）
                if (old == null || old.intValue() != e.getValue().intValue()) {
                    whitelist.put(e.getKey(), e.getValue());
                    changed++;
                }
            }
            LOGGER.info("[uubridge] OEI cost normalization: {} families, {} ids overridden ({} changed)",
                    families.size(), overrides.size(), changed);
        } catch (Throwable t) {
            LOGGER.warn("[uubridge] OEI cost normalization: whitelist write failed, skipped: {}", t.toString());
            return;
        }

        // ② 刷 syncedReplicationCosts ①：单人档 JVM 共享，客户端 join 时把 ① 写进共享
        //    Ic2Config.INSTANCE，服务端查价也先读 ① → 只写白名单会被旧同步表遮蔽。
        //    dedicated 服务端 ① 本为 null，写入无害且与重发包内容一致。
        try {
            syncSyncedTableOnServer();
        } catch (Throwable t) {
            LOGGER.warn("[uubridge] OEI cost normalization: synced-table refresh failed: {}", t.toString());
        }

        // ③ 重发客户端（IC2 自带分块通道，dedicated 客户端用）
        try {
            resendReplicationCostsToAll(server);
        } catch (Throwable t) {
            LOGGER.warn("[uubridge] OEI cost normalization: client resend failed: {}", t.toString());
        }

        // ④ 两端 IU 表重填
        UuBridgeNetwork.sendRefillToAll(server);
        UuCostFiller.forceRefill();
    }

    /**
     * 反射调用 {@code Ic2Config.INSTANCE.applyServerReplicationCosts(json)} 把 ①
     * syncedReplicationCosts 刷成归一化版（json = 写白名单后的 prettyAllReplicationCosts，
     * buildAllReplicationCosts 白名单优先 → 覆盖值已并入）。
     */
    private static void syncSyncedTableOnServer() throws Exception {
        Object ic2Config = UuCostFiller.ic2ConfigInstance();
        Class<?> cls = ic2Config.getClass();
        String json = (String) cls.getMethod("prettyAllReplicationCosts").invoke(ic2Config);
        cls.getMethod("applyServerReplicationCosts", String.class).invoke(ic2Config, json);
    }

    /** OEI 公开 API 收族：源 id → 罐体 id 分组。tag 规则天然忽略（注册表遍历只碰到物品）。 */
    private static Map<String, Set<String>> collectFamilies() {
        if (!ItemReplacementCache.hasAnyMappings()) {
            return Map.of();
        }
        Map<String, Set<String>> families = new LinkedHashMap<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item == null || item == Items.AIR) {
                continue;
            }
            String id = ForgeRegistries.ITEMS.getKey(item).toString();
            if (ItemReplacementCache.isSourceItemId(id)) {
                String result = ItemReplacementCache.matchItem(id);
                // 罐体必须是注册物品（id 是 tag 或已注销物品的规则不参与成本归一化）
                if (result != null && !result.equals(id)
                        && ResourceLocation.tryParse(result) != null
                        && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(result))) {
                    families.computeIfAbsent(result, k -> new LinkedHashSet<>()).add(id);
                }
            }
        }
        // 补罐体自身进族（快照/写入都要覆盖 D）
        for (Map.Entry<String, Set<String>> e : families.entrySet()) {
            e.getValue().add(e.getKey());
        }
        return families;
    }

    /** 反射取 IC2 运行时白名单 Map（可变，Jackson 反序列化的 LinkedHashMap）。 */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> ic2Whitelist() throws Exception {
        Object ic2Config = UuCostFiller.ic2ConfigInstance();
        Object mainConfig = ic2Config.getClass().getMethod("getCurrent").invoke(ic2Config);
        Object uuReplication = mainConfig.getClass().getMethod("getUuReplication").invoke(mainConfig);
        return (Map<String, Integer>) uuReplication.getClass().getMethod("getReplicationWhitelist").invoke(uuReplication);
    }

    /** 反射重发 IC2 复制成本表（登入时 Ic2_120.java:308 同款调用），覆盖客户端 synced 表。 */
    private static void resendReplicationCostsToAll(MinecraftServer server) throws Exception {
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        Object ic2Config = UuCostFiller.ic2ConfigInstance();
        String json = (String) ic2Config.getClass().getMethod("prettyAllReplicationCosts").invoke(ic2Config);

        Class<?> helperCls = Class.forName("ic2_120.content.network.ConfigSyncHelper");
        Object helper = helperCls.getField("INSTANCE").get(null);
        // Kotlin companion object 的静态字段 "Companion" 在外层类上（javap 实证），
        // 不在 ConfigSyncPacket$Companion 类里（0.1.3 首发版在此踩过 NoSuchFieldException）
        Class<?> packetCls = Class.forName("ic2_120.content.network.ConfigSyncPacket");
        Object companion = packetCls.getField("Companion").get(null);
        ResourceLocation channelId = (ResourceLocation) companion.getClass()
                .getMethod("getREPLICATION_COSTS_ID").invoke(companion);
        Method send = helperCls.getMethod("sendToPlayer", ServerPlayer.class, ResourceLocation.class, String.class);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            send.invoke(helper, player, channelId, json);
        }
    }
}
