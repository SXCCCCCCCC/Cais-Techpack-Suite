package com.sxcccccccc.mekmmuufix;

import com.sxcccccccc.mekmmuufix.compat.UuCompat;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * mekmmuufix：让 mekmm 复制机 / 流体复制机 / 复制工厂直接使用 IC2 的 UU 复制成本体系。
 *
 * <p>实现 = mixin 在 mekmm 各配方查询点（isValidXxxInput / getRecipe / JEI 注册）HEAD 注入
 * {@link UuCompat} 的懒填充，把 mekmm 静态配方表替换为 IC2
 * {@code Ic2Config.INSTANCE.getReplicationCostUb(...)}（白名单 + 启发式图计算）的换算结果，
 * 超过 90B（90_000 mB = 90_000_000 uB）的物品截断禁止复制。</p>
 *
 * <p>本事件：每次服务器启动（IC2 索引重建）重置填充状态并预热。</p>
 */
@Mod(MekmmUuFix.MODID)
public class MekmmUuFix {

    public static final String MODID = "mekmmuufix";

    public MekmmUuFix() {
        MinecraftForge.EVENT_BUS.register(MekmmUuFix.class);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        UuCompat.onServerStarted();
    }
}
