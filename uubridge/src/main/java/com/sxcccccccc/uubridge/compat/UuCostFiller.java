package com.sxcccccccc.uubridge.compat;

import com.denfop.IUItem;
import com.denfop.api.Recipes;
import com.denfop.api.recipe.ReplicatorRecipe;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * IU "replicator" 配方表整体替换为 IC2 UU 成本体系（方案 A，照抄 mekmmuufix 模式）。
 *
 * <p>IU 的 UU 成本不是分散读的：扫描机槽位门禁（InventoryScannable）、扫描成本、
 * 模式存储读档、复制机消耗、tooltip、JEI 全部从自研配方系统一张 "replicator"
 * 表读取（{@code ReplicatorRecipe.init()} 在 {@code IUCore} 的 TagsUpdatedEvent
 * getore 里注册 ~250 条硬编码价）。只要在运行时把这张表整体替换为 IC2 成本换算
 * 结果，IU 整条链路自动切到 IC2 定价。
 *
 * <p>权威成本来源（Connector 环境下 IC2 类名/方法名不变，反射签名稳定）：
 * <ul>
 *   <li>{@code ic2_120.config.Ic2Config.INSTANCE.getReplicationCostUb(String): Integer}
 *       —— 唯一权威查询入口：① 服务端同步表（客户端）② 配置白名单
 *       （config/ic2_120.json replicationWhitelist，423 条）③ 动态图
 *       （UuCostIndex → UuGraph Bellman-Ford）。null = 不可复制。</li>
 *   <li>单位 uB（micro-bucket）：1 mB = 1000 uB = 1,000,000 EU；IU matter 单位是
 *       buckets：matter = col/1000，col(mB) = uB/1000 ⇒
 *       <b>matter(buckets) = uB / 1_000_000</b>（与 IC2 复制机 droplets 公式
 *       交叉验证一致）。</li>
 *   <li>{@code UuCostIndex.INSTANCE.isReady()} —— 动态索引构建状态（服务端
 *       SERVER_STARTED 重建）。未 ready 时查询只回白名单 → 两段式填充：
 *       白名单版 → ready 翻转后全量版重填。</li>
 * </ul>
 *
 * <p>90B 上限截断：成本 &gt; 90,000,000 uB（= 90,000 mB）跳过禁止复制，防止套娃
 * 配方成本爆炸（IC2 白名单最贵条目 346,466,802 uB 也会被截掉；IU 复制机罐
 * 16000 mB 装不下超高成本，截断更干净）。
 *
 * <p>时序：IU 每次 TagsUpdatedEvent 重注册旧价表（{@code ReplicatorRecipe.init()}，
 * 有 register 标志门控只跑一次 SERVER_DATA_LOAD），本 handler 以
 * {@code EventPriority.LOWEST} 挂同一事件在 IU 之后执行"清表 + 重填"，最终表内容
 * 恒为 IC2 版。ServerStartedEvent 重置填充标志并强制重填（IC2 索引此时已重建
 * ready → 全量版；单人游戏集成服同样触发，客户端在 join 后同步表已到）。
 * 填表后刷新客户端快照 {@code IUItem.fluidMatterRecipe} 与 JEI 静态列表
 * （{@code ReplicatorHandler.getRecipes().clear() + initRecipes()}，反射调用，
 * 服务端/JEI 未装时安全跳过）。
 */
@Mod.EventBusSubscriber(modid = "uubridge", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UuCostFiller {

    /** 90B 上限，单位 uB：90,000 mB × 1000 uB/mB（照抄 mekmmuufix 实证值）。 */
    public static final long MAX_UU_COST_UB = 90_000_000L;
    /** uB → mB 换算：1 mB = 1000 uB */
    private static final double UB_PER_MB = 1000.0;

    private static final Object LOCK = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger("uubridge");

    private static volatile boolean filled = false;
    /** 上次填充时 IC2 动态索引是否 ready（ready 状态翻转时需重填全量版） */
    private static volatile boolean lastFillIndexReady = false;

    private static Method ic2CostUbMethod;
    private static Object ic2ConfigInstance;
    private static Method ic2IndexReadyMethod;
    private static Object ic2IndexInstance;

    private UuCostFiller() {
    }

    /**
     * TagsUpdatedEvent（LOWEST，IU 的 getore 之后）。
     *
     * <p>不做 UpdateCause 过滤：服务端 SERVER_DATA_LOAD（IU 注册表时机）与客户端
     * CLIENT_PACKET_RECEIVED（Forge 补丁实证：客户端 handleUpdateTags 构造
     * {@code new TagsUpdatedEvent(registry, true, isLocalConnection())} → 恒为
     * CLIENT_PACKET_RECEIVED，IU 的 SERVER_DATA_LOAD 过滤下客户端多人永不重注册
     * 表）都需要填充——客户端走 IC2 同步表/本地白名单，保证 JEI 与 tooltip 正常。
     * 本 handler 每次幂等"清 + 填"，最终表内容恒为 IC2 版。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        ensureFill();
    }

    /**
     * ServerStartedEvent：IC2 动态索引在服务器启动时重建（ready 翻转），重置
     * 填充标志并强制重填全量版。单人游戏集成服同样触发。
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        synchronized (LOCK) {
            filled = false;
            lastFillIndexReady = false;
        }
        ensureFill();
    }

    /** 两段式填充：索引未 ready 填白名单版；ready（或已翻转）填全量版。 */
    public static void ensureFill() {
        synchronized (LOCK) {
            boolean ready = isIc2IndexReady();
            if (filled && ready == lastFillIndexReady) {
                return;
            }
            fill();
            filled = true;
            lastFillIndexReady = ready;
            LOGGER.info("[uubridge] IU replicator table refilled from IC2 costs (indexReady={})", ready);
        }
    }

    /** 清空 IU 表并从 IC2 成本重新填充。 */
    private static void fill() {
        // 清空：removeAll 删 map_recipes + map_recipe_managers_itemStack（槽位门禁列表），
        // 后续 ReplicatorRecipe.add → addRecipe 自动重建两者（含 InventoryScannable 门禁）。
        Recipes.recipes.removeAll("replicator");

        int count = 0;
        int capped = 0;
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            if (item == Items.AIR) {
                continue;
            }
            String id = ForgeRegistries.ITEMS.getKey(item).toString();
            Integer costUb = queryIc2CostUb(id);
            if (costUb == null || costUb <= 0) {
                continue; // 不可复制 / 无成本（IC2 语义：没标价 = 不可扫描）
            }
            if (costUb > MAX_UU_COST_UB) {
                capped++; // 90B 上限截断：禁止复制
                continue;
            }
            // ReplicatorRecipe.add(ItemStack, double col)：col 单位 mB = uB/1000，
            // 向上取整保证消耗不低于 IC2 成本（IU consumeUu 按 mB 取整 + 找零无损）；
            // 内部存 matter = col/1000（buckets）= uB/1e6。
            ReplicatorRecipe.add(new ItemStack(item), Math.ceil(costUb / UB_PER_MB));
            count++;
        }

        // 客户端 tooltip 快照刷新（IUEventHandler 读 IUItem.fluidMatterRecipe）
        IUItem.fluidMatterRecipe = Recipes.recipes.getRecipeStack("replicator");

        // JEI 静态列表刷新（客户端类，反射调用；服务端/JEI 未装时类加载失败被吞）
        refreshJei();

        LOGGER.info("[uubridge] replicator table: {} entries added, {} capped at 90B", count, capped);
    }

    /**
     * 反射调用 IC2 {@code Ic2Config.INSTANCE.getReplicationCostUb(String)}。
     * IC2 类经 Connector 运行时仅重映射 MC 引用，自身类名/方法名不变。
     *
     * @return 成本（uB），或 null（不可复制 / IC2 未装 / 版本变化——反射失败一律
     *         回 null；此时表内容为空、IU 复制机全停，日志与启动表现可定位）
     */
    private static Integer queryIc2CostUb(String itemId) {
        try {
            if (ic2CostUbMethod == null) {
                Class<?> cls = Class.forName("ic2_120.config.Ic2Config");
                ic2ConfigInstance = cls.getField("INSTANCE").get(null);
                ic2CostUbMethod = cls.getDeclaredMethod("getReplicationCostUb", String.class);
                ic2CostUbMethod.setAccessible(true);
            }
            return (Integer) ic2CostUbMethod.invoke(ic2ConfigInstance, itemId);
        } catch (Exception e) {
            return null;
        }
    }

    /** 反射查询 IC2 动态成本索引是否构建完成。失败（IC2 未装）返回 false。 */
    private static boolean isIc2IndexReady() {
        try {
            if (ic2IndexReadyMethod == null) {
                Class<?> cls = Class.forName("ic2_120.content.uu.UuCostIndex");
                ic2IndexInstance = cls.getField("INSTANCE").get(null);
                ic2IndexReadyMethod = cls.getDeclaredMethod("isReady");
                ic2IndexReadyMethod.setAccessible(true);
            }
            return Boolean.TRUE.equals(ic2IndexReadyMethod.invoke(ic2IndexInstance));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JEI 静态列表刷新：{@code ReplicatorHandler.getRecipes().clear() + initRecipes()}。
     * ReplicatorHandler 是客户端 JEI 集成类（服务端加载该类会因静态初始化引用
     * JEI 接口抛 Error——NoClassDefFoundError/ExceptionInInitializerError 是
     * Error 不是 Exception，必须 catch Throwable）。失败静默属正常。
     */
    private static void refreshJei() {
        try {
            Class<?> handler = Class.forName("com.denfop.integration.jei.replicator.ReplicatorHandler");
            Object list = handler.getDeclaredMethod("getRecipes").invoke(null);
            if (list instanceof List<?> recipes) {
                recipes.clear();
            }
            handler.getDeclaredMethod("initRecipes").invoke(null);
        } catch (Throwable ignored) {
            // 服务端 / JEI 未装 / 类不在类路径：正常跳过
        }
    }
}
