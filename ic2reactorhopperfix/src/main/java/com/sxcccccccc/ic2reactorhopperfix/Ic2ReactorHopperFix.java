package com.sxcccccccc.ic2reactorhopperfix;

import net.minecraftforge.fml.common.Mod;

/**
 * IC2 EU 核反应堆自动化喂料修复（本包自用 fix mod）。
 *
 * <p>修复目标：ic2_120（Fabric 版 IC2，经 Sinytra Connector 在 Forge 上运行）的
 * 反应仓 / 访问接口在 Fabric Transfer API 的 {@code ItemStorage.SIDED} 上暴露的
 * {@code ReactorItemStorage} 事务快照为空实现，导致精妙存储高级漏斗升级、Create 溜槽等
 * “先 simulate 后真插”的 mod 物流与 FFAPI 桥接协作时产生幻影物品（simulate 无法回滚），
 * 表现为物品消失 / 复制 / 不落槽。原版漏斗直接真插不 simulate，故不受影响。</p>
 *
 * <p>修复方式：见 {@link com.sxcccccccc.ic2reactorhopperfix.mixin.ReactorItemStorageProviderMixin}，
 * 把仓/接口的物品存储 lookup 重定向到中心核堆的 RoutedItemStorage（快照/回滚正确）。</p>
 */
@Mod("ic2reactorhopperfix")
public class Ic2ReactorHopperFix {
}
