package com.sxcccccccc.ic2mekradfix.mixin;

import com.sxcccccccc.ic2mekradfix.compat.GtCompat;
import com.sxcccccccc.ic2mekradfix.compat.Ic2Compat;
import com.sxcccccccc.ic2mekradfix.compat.MekCompat;
import mekanism.common.lib.radiation.RadiationManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方向一：IC2 防辐射服 → 防 Mek 辐射。
 *
 * <p>javap 确认：{@code RadiationManager.lambda$radiate$1} 里
 * {@code IRadiationEntity.radiate(dose * (1 - min(1, getRadiationResistance(entity))))}——
 * Mek 防辐射服唯一作用点就是 {@code getRadiationResistance}（护甲槽屏蔽值求和，clamp [0,1]，
 * 全套 hazmat / 带屏蔽单元全套 MekaSuit = 1.0）。IC2 hazmat / 量子甲没有
 * {@code IRadiationShielding} capability，原判定恒为 0。</p>
 *
 * <p>修复：原抗性未满时，若 IC2 认为该实体辐射免疫（{@link Ic2Compat}）或 GT 认为
 * 受到全套 PPE 保护（{@link GtCompat}），返回 1.0——完全阻断吸收，语义与穿 Mek
 * 全套 hazmat 相同（只阻断吸收、不清除已累积辐射）。</p>
 */
@Mixin(value = RadiationManager.class, remap = false)
public abstract class MekRadiationManagerMixin {

    @Inject(method = "getRadiationResistance", at = @At("RETURN"), cancellable = true, remap = false)
    private void ic2mekradfix$ic2SuitsBlockMekRadiation(LivingEntity entity,
                                                        CallbackInfoReturnable<Double> cir) {
        if (cir.getReturnValue() >= MekCompat.FULL_SHIELDING) {
            return;
        }
        // 性能短路（2026-09-11 spark 实测）：Mek 每 tick 对全图每个 LivingEntity
        // 调 getRadiationResistance，防辐射服判定只对玩家有意义——非玩家实体直接
        // 回落 Mek 原判定（0 抗性），省掉每 tick 数千次反射+GT 判定
        // （修复前 ic2mekradfix 自身 1.56% + 触发的 GT ProtectionType 1.04%）。
        if (!(entity instanceof Player)) {
            return;
        }
        if (Ic2Compat.isIc2RadiationImmune(entity) || GtCompat.isGtPpeProtected(entity)) {
            cir.setReturnValue(MekCompat.FULL_SHIELDING);
        }
    }
}
