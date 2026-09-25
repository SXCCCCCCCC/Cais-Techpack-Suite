package com.sxcccccccc.gtnoboomfix;

import net.minecraftforge.fml.common.Mod;

/**
 * GTCEu Modern 爆炸/损坏机制关闭（1.20.1-Tec 包自用，简化 GT 设计决策）。
 *
 * <p>包内构建了 FE↔EU 能量桥（gregfluxology），FE 转电压是内置转换，
 * 误接错电压即可引爆机器——作者拍板关闭全部爆炸/损坏机制。
 *
 * <p>实现依据为 <b>gtceu-1.20.1-7.5.3.jar 实际字节码</b>（javap 核对，与
 * 源码仓库 7.5.x HEAD 有结构差异：源码用 GTUtil.doExplosion + 挂载
 * EnvironmentalExplosionTrait，7.5.3 jar 改为 {@code IExplosionMachine}
 * 接口 default 方法承载全部机器爆炸落地）。
 *
 * <ul>
 *   <li>{@link com.sxcccccccc.gtnoboomfix.mixin.IExplosionMachineMixin}：
 *       接口 mixin（mixin 声明为 interface）+ {@code @Overwrite} 整体替换
 *       default 方法 {@code doExplosion(BlockPos, float)} 为空实现 —— 一处封死
 *       NotifiableEnergyContainer 超压（覆盖全部电动单方块 + 能源舱 + 转换器）、
 *       BatteryBuffer 超压、小锅炉干烧回水爆炸、大锅炉缺水爆炸 + 20% 连锁、
 *       变压器拆机爆炸、天气/地形爆炸（后两者 config 本已关闭，行为不变）。
 *       锅炉温控/减产、能量接收逻辑全部保留。</li>
 *   <li>{@link com.sxcccccccc.gtnoboomfix.mixin.CreativeEnergyContainerMachineMixin}：
 *       创造能源容器是延迟爆炸（acceptEnergyFromNetwork 只置 doExplosion 标志，
 *       updateEnergyTick 内 %20 tick 直调 level.explode，不走 IExplosionMachine），
 *       每 tick 开头清标志（@Shadow + @Inject HEAD）。</li>
 *   <li>{@link com.sxcccccccc.gtnoboomfix.mixin.FluidPipeBlockEntityMixin}：
 *       checkAndDestroy 开头 return（@Inject HEAD cancellable），管道全免疫
 *       （过热烧/泄漏/酸蚀/冻碎/等离子熔 + 小爆炸 + 点火 + 灼伤一并消除）。</li>
 * </ul>
 *
 * <p>明确保留：涡轮转子磨损 + 转子接触伤害（作者要求不动 RotorHolderPartMachine）。
 */
@Mod("gtnoboomfix")
public class GtNoBoomFix {
}
