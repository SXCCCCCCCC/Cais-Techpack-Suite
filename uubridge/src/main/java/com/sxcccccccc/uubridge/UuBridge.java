package com.sxcccccccc.uubridge;

import net.minecraftforge.fml.common.Mod;

/**
 * UU 链桥（IU Industrial Upgrade 3.4.0.10 x IC2 Refabricated）。
 *
 * <p>第一部分：模式水晶桥。OEI 统一后新产出的水晶全部是
 * {@code ic2_120:crystal_memory}，IU 扫描机/模式存储机的水晶判定
 * （槽位 canPlaceItem 按 item 实例、5 处 {@code instanceof ItemCrystalMemory}）
 * 全部失效。本 mod 通过 4 个 mixin 放宽判定 + {@code CrystalMemoryBridge}
 * 伪装 item，让 IC2 水晶作为 IU 模式载体使用（数据单方向：只读写 IU 格式
 * {@code "Pattern"} 键，写入时删除 IC2 {@code "UuTemplate"} 键防止 IC2 复制机
 * 0 成本免费复制漏洞）。
 *
 * <p>第二部分：UU 成本整表替换。IU 复制机/扫描机/模式存储/工具提示/JEI 全部从
 * 自研 "replicator" 配方表读成本；本 mod 在 TagsUpdatedEvent（LOWEST，IU 的
 * {@code ReplicatorRecipe.init()} 之后）清空该表并从 IC2 权威成本
 * {@code Ic2Config.getReplicationCostUb} 重新填充（matter buckets = uB/1e6，
 * 90B 上限截断，两阶段 ready 翻转，ServerStartedEvent 重置），
 * IU 整条 UU 链自动切到 IC2 定价。
 */
@Mod("uubridge")
public class UuBridge {

    public UuBridge() {
    }
}
