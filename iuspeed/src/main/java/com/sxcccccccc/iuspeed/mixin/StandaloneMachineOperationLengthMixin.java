package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 第二大家族：自带 {@code operationLength} 字段、自行 tick 的机器
 * （流体混合/分离/加热/整合、固体/电解、蒸汽机、基因、油提纯、世界收集器等）。
 * 这些机器不用 ComponentProcess，各自在构造器里给
 * {@code defaultOperationLength = operationLength = <字面量>}，
 * 并各自在 updateEntityServer 里 {@code progress >= this.operationLength} 判完成
 * （如 FluidMixer desktop 源码 line 452）。对每个目标类的构造器 RETURN 注入缩放。
 *
 * <p>目标清单原则（已逐类核对桌面源码 3.4.0.1）：
 * <ul>
 *   <li>只收<b>自动计时型</b>机器：自己声明 {@code int operationLength}、
 *       构造器里赋值、updateEntityServer 里自行 progress 迭代判完成。
 *       <b>v1.4.0</b>（用户裁决"所有机器都想加速"）把石头/黑曜石发生器
 *       （BaseGenStone / BaseObsidianGenerator）、泵族（Pump / CombinedPump；
 *       SteamPump 走专属 mixin，因它只有 ComponentProgress、无回流重算）与
 *       PrimalFluidHeater 收编入列；仍然不收：玩家点击驱动（PrimalPump
 *       onSneakingActivated 逐步 +4）、平台组装器（多因子协议）与
 *       相位/天气调度机（太阳能板族/雷击棒）。</li>
 *   <li>只收<b>自己声明</b>{@code int operationLength} 字段、且<b>构造器里赋值</b>的类
 *       （字段声明在父类的除外——父类收、子类不收，避免子类经 super 链时父子
 *       &lt;init&gt; 双触发；本 mixin 里 BaseGenStone / BaseObsidianGenerator
 *       即"收抽象父类、全族覆盖"的形态——BlockEntityGenerationStone /
 *       BlockEntityObsidianGenerator 子类只传参）；</li>
 *   <li>与 ComponentProcess 家族零重叠（38 个组件机全在 ComponentProcessMixin 收）；
 *       ComponentProgress 以 maxValue 为时长核心的机器
 *       （NightConverter/SoilAnalyzer/QuantumMiner）在 ProgressLengthMachineMixin 收；</li>
 *   <li>每个目标类<b>单一构造器</b>。例外：BlockEntityBaseAdditionGenStone
 *       双构造器（this() 链式）——由 {@code AdditionGenStoneDurationMixin}
 *       按 7 参构造器 descriptor 定向注入，this() 链不会双触发；</li>
 * </ul>
 * WorldCollector 一族（Aer/Aqua/Crystallize/Earth/Ender/Nether 六个装配器）
 * 字段赋在父类 BlockEntityBaseWorldCollector 构造器（line 67: = 800），
 * 子类构造器只是传 {@code EnumTypeCollector}——收父类即全族覆盖。
 */
@Mixin(targets = {
        "com.denfop.blockentity.base.BlockEntityBaseWorldCollector",
        "com.denfop.blockentity.base.BlockEntityConverterSolidMatter",
        "com.denfop.blockentity.base.BlockEntityDoubleMolecular",
        "com.denfop.blockentity.base.BlockEntityQuantumMolecular",
        "com.denfop.blockentity.base.BlockEntityRefrigeratorFluids",
        "com.denfop.blockentity.mechanism.BlockEntityBioGenerator",
        "com.denfop.blockentity.mechanism.BlockEntityElectricDryer",
        "com.denfop.blockentity.mechanism.BlockEntityElectricSqueezer",
        "com.denfop.blockentity.mechanism.BlockEntityFluidAdapter",
        "com.denfop.blockentity.mechanism.BlockEntityFluidHeater",
        "com.denfop.blockentity.mechanism.BlockEntityFluidIntegrator",
        "com.denfop.blockentity.mechanism.BlockEntityFluidMixer",
        "com.denfop.blockentity.mechanism.BlockEntityFluidSeparator",
        "com.denfop.blockentity.mechanism.BlockEntityGasCombiner",
        "com.denfop.blockentity.mechanism.BlockEntityGeneticReplicator",
        "com.denfop.blockentity.mechanism.BlockEntityGeneticStabilize",
        "com.denfop.blockentity.mechanism.BlockEntityIncubator",
        "com.denfop.blockentity.mechanism.BlockEntityIndustrialOrePurifier",
        "com.denfop.blockentity.mechanism.BlockEntityInsulator",
        "com.denfop.blockentity.mechanism.BlockEntityItemDivider",
        "com.denfop.blockentity.mechanism.BlockEntityItemDividerFluids",
        "com.denfop.blockentity.mechanism.BlockEntityMutatron",
        "com.denfop.blockentity.mechanism.BlockEntityNeutronSeparator",
        "com.denfop.blockentity.mechanism.BlockEntityOilPurifier",
        "com.denfop.blockentity.mechanism.BlockEntityPolymerizer",
        "com.denfop.blockentity.mechanism.BlockEntityPrimalFluidIntegrator",
        "com.denfop.blockentity.mechanism.BlockEntityPrimalGasChamber",
        "com.denfop.blockentity.mechanism.BlockEntityReverseTransriptor",
        "com.denfop.blockentity.mechanism.BlockEntityRNACollector",
        "com.denfop.blockentity.mechanism.BlockEntityRodManufacturer",
        "com.denfop.blockentity.mechanism.BlockEntityRotorAssembler",
        "com.denfop.blockentity.mechanism.BlockEntitySingleFluidAdapter",
        "com.denfop.blockentity.mechanism.BlockEntitySolidFluidIntegrator",
        "com.denfop.blockentity.mechanism.BlockEntitySolidFluidMixer",
        "com.denfop.blockentity.mechanism.BlockEntitySolidMixer",
        "com.denfop.blockentity.mechanism.BlockEntitySolidStateElectrolyzer",
        "com.denfop.blockentity.mechanism.BlockEntityTripleSolidMixer",
        "com.denfop.blockentity.mechanism.BlockEntityWaterRotorAssembler",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamBioGenerator",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamCrystalCharge",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamDryer",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamSharpener",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamSolidFluidMixer",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamSqueezer",
        "com.denfop.blockentity.mechanism.steam.BlockEntitySteamWireInsulator",
        "com.denfop.blockentity.mechanism.BlockEntityBaseGenStone",
        "com.denfop.blockentity.base.BlockEntityBaseObsidianGenerator",
        "com.denfop.blockentity.mechanism.BlockEntityPump",
        "com.denfop.blockentity.mechanism.combpump.BlockEntityCombinedPump",
        "com.denfop.blockentity.mechanism.BlockEntityPrimalFluidHeater"
})
public abstract class StandaloneMachineOperationLengthMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleOperationLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
