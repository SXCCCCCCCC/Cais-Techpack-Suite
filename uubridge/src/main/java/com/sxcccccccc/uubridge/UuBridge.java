package com.sxcccccccc.uubridge;

import net.minecraftforge.fml.common.Mod;

/**
 * UU 链桥（IU Industrial Upgrade 3.4.0.10 x IC2 Refabricated）。
 *
 * <p>第一部分：模式水晶桥（0.1.1 起双向，用户裁决）。OEI 统一后新产出的
 * 水晶全部是 {@code ic2_120:crystal_memory}，IU 扫描机/模式存储机的水晶判定
 * （槽位 canPlaceItem 按 item 实例、5 处 {@code instanceof ItemCrystalMemory}）
 * 全部失效。本 mod 通过 4 个 mixin 放宽判定 + {@code CrystalMemoryBridge} 伪装
 * item，让 IC2 水晶作为 IU 模式载体使用。成本已统一用 IC2 计算，两种格式
 * 双向互通：IU 写入时一次写双键（IU {@code "Pattern"} 完整栈 + IC2
 * {@code "UuTemplate"} {@code {ItemId: ...}}，与 UuTemplateData.toNbt 同构，
 * IC2 机器零改动原生可读）；IU 读取优先 {@code "Pattern"}、无则读
 * {@code "UuTemplate"} 转换。0.1.0 单向守卫（写时删 UuTemplate）已移除。
 *
 * <p>第二部分：UU 成本整表替换。IU 复制机/扫描机/模式存储/工具提示/JEI 全部从
 * 自研 "replicator" 配方表读成本；本 mod 在 TagsUpdatedEvent（LOWEST，IU 的
 * {@code ReplicatorRecipe.init()} 之后）清空该表并从 IC2 权威成本
 * {@code Ic2Config.getReplicationCostUb} 重新填充（0.1.2 起精确传值：
 * {@code ReplicatorRecipe.add(ItemStack, double col)} 直接收
 * {@code costUb / 1000.0}，double 除法无取整 ⇒ matter = uB/1e6 精确；
 * IU 复制机动态消耗（consumeUu + extraUuStored 找零）无损，无需 ceil 地板、
 * 无 90B 截断；两阶段 ready 翻转，ServerStartedEvent 重置），
 * IU 整条 UU 链自动切到 IC2 定价。
 */
@Mod("uubridge")
public class UuBridge {

    public UuBridge() {
    }
}
