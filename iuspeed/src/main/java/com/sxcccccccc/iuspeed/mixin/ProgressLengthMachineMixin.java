package com.sxcccccccc.iuspeed.mixin;

import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * v1.1：以 {@link com.denfop.componets.ComponentProgress#maxValue} 为<b>操作时长核心</b>
 * 的机器族。<b>多方块冶炼炉（用户在用的"冶炼炉"）</b>就是这里：
 *
 * <ul>
 *   <li>{@code BlockEntitySmelteryFurnace}：构造器
 *       {@code addComponent(new ComponentProgress(this, 1, 108))}；完成由
 *       {@code BlockEntitySmelteryController.updateEntityServer} 判定
 *       {@code furnace.getComponent().getBar() >= 1}（progress >= maxValue）。</li>
 *   <li>{@code BlockEntitySmelteryCasting}：同上，{@code casting.getProgress().getBar() >= 1}。</li>
 *   <li>{@code BlockEntityAutoCrafter}：{@code ComponentProgress(1, (short) defaultOperationLength)}=100，
 *       完成判定 {@code componentProgress.getProgress() >= getMaxValue()}；升级时
 *       {@code setMaxValue(getOperationLength(defaultOperationLength))} 重算，
 *       故 {@code defaultOperationLength}（final int 字段）也必须同步缩小。</li>
 * </ul>
 *
 * <p>同时调用 {@link IuSpeedHelper#scale(Object)}（覆盖 AutoCrafter 的
 * {@code defaultOperationLength}；冶炼炉类无此字段，自动跳过）与
 * {@link IuSpeedHelper#scaleProgress(Object)}。guiProgress/progress 短整型是组件内部值，
 * 不用动——完成判定只依赖 maxValue 比例。
 *
 * <p><b>v1.4.0 收编三台"无 ComponentProcess 的纯 ComponentProgress 自动机"</b>
 * （用户裁决"所有机器都想加速"；旧版排除清单里它们被误归为"不碰"，现定案为
 * <b>自动计时机器</b>——每 tick 固定 +1、无玩家交互、完成即产出）：
 * <ul>
 *   <li>{@code BlockEntityNightConverter}（夜转换器）：字段名
 *       {@code public final ComponentProgress progress}（line 40，注意不叫
 *       {@code componentProgress}——helper 的 {@code scaleProgress} 按<b>字段类型</b>
 *       找，与名字无关），构造器 {@code new ComponentProgress(this, 1, 80)}
 *       （line 48），updateEntityServer line 96 完成判定
 *       {@code getBar() >= 1} → 重置 + consume + 产出；门控是夜间能量注入口
 *       （{@code ne.getEnergy() >= 62.5 && output != null}，line 93）。
 *       maxValue 80 → 16 @5x。check 为 {@code >=}，旧存档进度越过新 maxValue
 *       也能自愈（到 16 tick 即完成重置）。</li>
 *   <li>{@code BlockEntitySoilAnalyzer}（土壤分析器）：字段
 *       {@code progress}（line 40），构造器 {@code (this, 1, (short) 400)}
 *       （line 48），完成判定 line 114 {@code getProgress() >= getMaxValue()}
 *       → {@code analyzed = true} + 记录本区块辐射（一次性分析，之后
 *       {@code getBar() < 1} 门不再放行）。400 → 80 @5x；tolerant check。</li>
 *   <li>{@code BlockEntityQuantumMiner}（量子矿机）：字段 {@code progress}
 *       （line 29），构造器 {@code (this, 1, 1000)}（line 36），updateEntityServer
 *       line 45 完成判定 <b>{@code getBar() == 1}（精确相等！）</b> → 重置 +
 *       随机出 crafting_elements（metas 637/638/639/640/643/644/648/649）。
 *       1000 → 200 @5x。<b>为什么精确相等也安全</b>：进度<b>不写 NBT</b>
 *       （ComponentProgress 无 NBT 方法，矿机自身也零 NBT），区块重载即归零，
 *       从 0 计到新 maxValue 时 {@code progress*1D/maxValue} 在 IEEE double 下
 *       恰好等于 1.0（分子分母同为 ≤32767 整数），不会跳过；能量门槛 62.5
 *       QE/t 逐 tick 不变、每 op 总能量 ÷5。</li>
 * </ul>
 * 三台机器的 {progress} 短整型与 guiProgress 都不动，完成判定只看
 * progress/maxValue 比例。
 */
@Mixin(value = {
        com.denfop.blockentity.smeltery.BlockEntitySmelteryFurnace.class,
        com.denfop.blockentity.smeltery.BlockEntitySmelteryCasting.class,
        com.denfop.blockentity.mechanism.BlockEntityAutoCrafter.class,
        com.denfop.blockentity.mechanism.BlockEntityNightConverter.class,
        com.denfop.blockentity.mechanism.BlockEntitySoilAnalyzer.class,
        com.denfop.blockentity.mechanism.BlockEntityQuantumMiner.class
}, remap = false)
public abstract class ProgressLengthMachineMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleProgressLength(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
        IuSpeedHelper.scaleProgress(this);
    }
}
