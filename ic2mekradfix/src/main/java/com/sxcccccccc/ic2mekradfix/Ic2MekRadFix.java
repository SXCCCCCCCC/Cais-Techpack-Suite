package com.sxcccccccc.ic2mekradfix;

import net.minecraftforge.fml.common.Mod;

/**
 * IC2 × Mekanism × GregTech 防辐射服互通（本包自用 fix mod）。
 *
 * <p>背景：三 mod 的辐射防护判定各自独立——IC2（Fabric 版 ic2_120，经 Sinytra Connector
 * 运行）在辐射施加时按物品 id 判定「全套 hazmat / 量子甲」；Mekanism 用 Forge 的
 * {@code IRadiationShielding} capability 对 4 个护甲槽求和（全套 = 1.0）降低辐射吸收量；
 * GT 用 {@code HazardProperty$ProtectionType.isProtected}（ArmorComponentItem.isPPE
 * 或 {@code gtceu:ppe_armor} tag）判定致癌辐射防护。原生互通情况：Mek 全套 hazmat 因 id
 * 含 "hazmat" 被 IC2 判定意外接受；其余跨家组合大多零防护（详见研究报告 §6 互通矩阵）。</p>
 *
 * <p>修复（4 个 mixin）：</p>
 * <ol>
 *   <li>{@link com.sxcccccccc.ic2mekradfix.mixin.MekRadiationManagerMixin}：IC2 免疫或
 *       GT 全套 PPE 的穿着在 Mek 侧抗性按 1.0 计，完全阻断吸收。</li>
 *   <li>{@link com.sxcccccccc.ic2mekradfix.mixin.Ic2RadiationHandlerMixin}：Mek 全防护
 *       （全套 hazmat 或带屏蔽单元的全套 MekaSuit）或 GT 全套 PPE 对 IC2 背包放射性物品
 *       辐射免疫。</li>
 *   <li>{@link com.sxcccccccc.ic2mekradfix.mixin.Ic2NuclearReactorMixin}：同上判定接入
 *       IC2 核反应堆热辐射闸门（NuclearReactorBlockEntity 自带独立的 hasFullHazmat）。</li>
 *   <li>{@link com.sxcccccccc.ic2mekradfix.mixin.GtProtectionTypeMixin}：IC2 免疫或
 *       Mek 全防护的穿着对 GT 致癌辐射（背包放射性物品 + 环境辐射区两调用点）视为受保护。</li>
 * </ol>
 *
 * <p>语义口径：与三家原生一致——全套才免疫（二进制）、只阻断吸收不清除已累积辐射、
 * MekaSuit 需安装辐射屏蔽单元模块、GT 只磨损自家 PPE 装备（IC2/Mek 装备不被磨损，
 * 可接受的不对称）。</p>
 */
@Mod("ic2mekradfix")
public class Ic2MekRadFix {
}
