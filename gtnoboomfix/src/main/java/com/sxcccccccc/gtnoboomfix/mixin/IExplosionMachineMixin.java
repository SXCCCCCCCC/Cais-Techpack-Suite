package com.sxcccccccc.gtnoboomfix.mixin;

import com.gregtechceu.gtceu.api.machine.feature.IExplosionMachine;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 7.5.3 jar 的机器爆炸咽喉点：{@code IExplosionMachine.doExplosion(BlockPos, float)}
 * 接口 default 方法（javap 核实：内部 = {@code Level.removeBlock(pos, false)} +
 * {@code Level.explode(...)}，地形开关受 config doesExplosionDamagesTerrain 控制）。
 * jar 内没有任何实现类覆写 doExplosion —— 全部机器爆炸都汇聚到这个 default：
 *
 * <ul>
 *   <li>NotifiableEnergyContainer.acceptEnergyFromNetwork 超压分支
 *       （invokeinterface IExplosionMachine.doExplosion(F)）→ 覆盖全部电动单方块
 *       （TieredEnergyMachine 实现该接口）+ 能源舱 + 转换器</li>
 *   <li>BatteryBufferMachine$EnergyBatteryTrait.acceptEnergyFromNetwork 超压分支
 *       （invokevirtual BatteryBufferMachine.doExplosion(F)）</li>
 *   <li>SteamBoilerMachine.updateCurrentTemperature 干烧回水爆炸
 *       （invokevirtual SteamBoilerMachine.doExplosion(F)，4 种小锅炉共用）</li>
 *   <li>LargeBoilerMachine.updateCurrentTemperature 缺水爆炸 + 20% 连锁小爆炸
 *       （invokevirtual doExplosion(F) / doExplosion(BlockPos,F)，共 3 处）</li>
 *   <li>ActiveTransformerMachine 拆机爆炸、checkWeatherOrTerrainExplosion 天气检查
 *       —— config 本已关闭，无行为变化</li>
 * </ul>
 *
 * <p>{@code doExplosion(float)} 重载委托到本方法，一并失效。
 *
 * <p><b>写法（实踩教训）</b>：mixin 必须声明为 <b>interface</b>（接口目标不接受
 * 类形态 mixin，运行时抛 "target type mismatch ... is an interface in
 * MixinInfo$SubType$Standard"）；且 Mixin 0.8.x 的接口 mixin <b>不支持
 * {@code @Inject}</b>（InvalidInterfaceMixinException），只支持
 * <b>{@code @Overwrite}</b> 整体替换 default 方法体 —— 本方法为空实现即等价
 * "摘掉爆炸"，各调用点正常逻辑（能量接收返回、锅炉温控、蒸汽减产）原样保留。
 *
 * <p>remap=false：目标为 GT mod 接口（不在原版映射集）；handler 空体、
 * 无原版成员引用。
 */
@Mixin(value = IExplosionMachine.class, remap = false)
public interface IExplosionMachineMixin {

    @Overwrite
    default void doExplosion(BlockPos pos, float power) {
    }
}
