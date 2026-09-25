package com.sxcccccccc.iuspeed.mixin;

import com.denfop.blockentity.mechanism.steam.BlockEntitySteamPump;
import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>v1.4.0：蒸汽泵（{@code BlockEntitySteamPump}，id 区块
 * {@code BlockBaseMachine3Entity.steam_pump}）。</b>
 *
 * <p><b>它与 Pump / CombinedPump 的关键区别（决定处置形态）</b>：
 * <ul>
 *   <li>电泵 {@code BlockEntityPump}：onLoaded()（line 206+）必调
 *       {@code setUpgradestat()}，其中 {@code componentProgress.setMaxValue(
 *       operationLength)}——即组件 maxValue 总会被从 <b>defaultOperationLength</b>
 *       重派一次，因此只缩字段（standalone mixin 收编）就够；</li>
 *   <li>组合泵 {@code BlockEntityCombinedPump}：用<b>裸</b>{@code short progress}
 *       判完成（{@code progress >= this.operationLength}），不经过任何
 *       ComponentProgress——缩字段即够（standalone mixin 收编）；</li>
 *   <li>蒸汽泵 <b>既没有 setUpgradestat，也没有裸 progress</b>——完成判定是
 *       {@code componentProgress.getProgress() < componentProgress.getMaxValue()}
 *       (updateEntityServer line 120)，maxValue 由构造器一次性写死
 *       （line 66: {@code new ComponentProgress(this, 1, (short) operationLength)}
 *       = 25），<b>此后没有任何重派点</b>（全类 grep：无 setUpgradestat）。
 *       只缩字段则 maxValue 永远停留在 25 → 加速零生效！</li>
 * </ul>
 * 故本 mixin 在构造器 RETURN 同时调用
 * {@link IuSpeedHelper#scale}（缩 operationLength/defaultOperationLength 字段，
 * GUI/tooltip/未来重派一致性）+ {@link IuSpeedHelper#scaleProgress}
 * （缩其自声明 {@code ComponentProgress componentProgress} 的 maxValue：
 * 25 → 5 @5x）——后者由 helper 按<b>字段类型</b>找（含父类链），
 * 实现不依赖字段名。能量每 tick（steam.useEnergy(2)）原样。
 *
 * <p>构造器唯一 {@code (BlockPos, BlockState)}（运行时 javap 核对），注入
 * 形态 = 项目验证过的经典形态（唯一 ctor RETURN + 纯 {@code (CallbackInfo)}）。
 * SteamPump 不与电泵/组合泵共享 <init>，无 double-fire 风险。
 */
@Mixin(value = BlockEntitySteamPump.class, remap = false)
public abstract class SteamPumpDurationMixin {

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$scaleSteamPump(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
        IuSpeedHelper.scaleProgress(this);
    }
}
