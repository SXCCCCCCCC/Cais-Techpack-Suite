package com.sxcccccccc.uubridge.compat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * uubridge 自用最小网络通道（0.1.3）：成本归一化完成后服务端广播"重填"通知，
 * 客户端收到后 {@link UuCostFiller#forceRefill()} 重填 IU replicator 表。
 *
 * <p>为什么需要：单人档客户端与服务端共享同一 JVM（IU 配方表是静态的，服务端重填
 * 即两端生效），但 dedicated 服务器上客户端的 IU 表（JEI/水晶 tooltip 显示用）是
 * 客户端在 TagsUpdatedEvent 里自己填的，成本重发包（IC2 的 ConfigSyncHelper）不会
 * 触发它 → 需要本包补一次触发。实际扣费走服务端，客户端表只影响显示。
 */
public final class UuBridgeNetwork {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("uubridge", "refill"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static final int REFILL_MSG_ID = 0;

    private UuBridgeNetwork() {
    }

    /** 主类构造器调用一次。 */
    public static void register() {
        CHANNEL.registerMessage(
                REFILL_MSG_ID,
                RefillMsg.class,
                (msg, buf) -> { /* 空消息，无载荷 */ },
                buf -> new RefillMsg(),
                (msg, ctx) -> {
                    ctx.get().enqueueWork(UuCostFiller::forceRefill);
                    ctx.get().setPacketHandled(true);
                }
        );
    }

    /** 服务端：归一化完成、IC2 成本重发之后广播。server 为 null（/reload 路径）时静默跳过。 */
    public static void sendRefillToAll(MinecraftServer server) {
        if (server == null) {
            return;
        }
        CHANNEL.send(PacketDistributor.ALL.noArg(), new RefillMsg());
    }

    /** 空消息体。 */
    public static final class RefillMsg {
        public RefillMsg() {
        }
    }
}
