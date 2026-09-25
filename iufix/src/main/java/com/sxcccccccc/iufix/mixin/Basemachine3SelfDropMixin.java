package com.sxcccccccc.iufix.mixin;

import com.denfop.blocks.mechanism.BlockBaseMachine3Entity;
import com.denfop.blocks.state.DefaultDrop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 用户裁决（2026-09-03）：{@code BlockBaseMachine3Entity.getDefaultDrop()} 恒返回
 * {@code DefaultDrop.Self}——basemachine3 全系机器镐挖直接掉整机（getPickBlock +
 * 组件 NBT），扳手拆除退化为可选，纯 QoL。
 *
 * <p><b>背景（实测"挖A掉B"）</b>：火箭组装机等多胞机器的主方块密封在代理格
 * （BlockCollisionProxy）内，扳手右键够不到（代理格非 Wrenchable，ItemToolWrench
 * 只认点击格方块）；只能镐挖外壳，而原掉落链代理格 onDestroyedByPlayer →
 * destroyBlock(masterPos, true, player) → getDrops → playerDestroy（wasWrench =
 * 手持物命中 forge:tools/wrench，镐子恒 false）→ BlockEntityBase.adjustDrop 的
 * DefaultDrop.Machine 分支无条件返回基础机器外壳（BlockResource.Type.machine，
 * BlockEntityBase.java 1158-1161）。Machine 机器只有扳手路径 fortune≥2（随机
 * 0-99 的 98%）才掉整机——多胞机器扳手够不到，整机永远拿不到。
 *
 * <p><b>做法（最小面：单方法单注入点）</b>：HEAD 拦截枚举类自声明的
 * {@code getDefaultDrop()}（IU 官方名映射，无参 → DefaultDrop），无条件返回
 * {@code DefaultDrop.Self}。所有消费方语义统一为"掉整机"：
 * <ul>
 *   <li>BlockEntityBase.adjustDrop 双分支（1148/1171）：!wrench Self →
 *       getPickBlock 整机；wrench Self → 同样整机；</li>
 *   <li>BlockEntityElectricBlock.adjustDrop（514/531）：{@code wrench ||
 *       getDefaultDrop()==Self} 分支把保留电能（80%，fortune=100 时 100%）写入
 *       掉落物——储能类机器镐挖开始保电；</li>
 *   <li>BlockEntityMultiMachine/SteamMultiMachine/BioMultiMachine/ElectricMachine
 *       各自的 adjustDrop switch：Self 分支同为掉整机。</li>
 * </ul>
 * 代价：挖机器不再掉基础/高级机器外壳（合成原料）——外壳应有独立合成途径
 * （待 JEI 实测；缺则 kubejs 补）。
 *
 * <p>取证：@Inject require=1（0 命中直接应用失败，不被 mixins.json 的
 * injectors.defaultRequire=0 静默吞掉）。目标为 enum 类（本项目新形态）：
 * 注入目标是普通实例方法，枚举常量本体（字段/构造器）未触碰。
 */
@Mixin(value = BlockBaseMachine3Entity.class, remap = false)
public abstract class Basemachine3SelfDropMixin {

    @Inject(method = "getDefaultDrop", at = @At("HEAD"), cancellable = true, require = 1)
    private void iufix$alwaysSelfDrop(CallbackInfoReturnable<DefaultDrop> cir) {
        cir.setReturnValue(DefaultDrop.Self);
    }
}
