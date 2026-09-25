package com.sxcccccccc.ic2mekradfix.mixin;

import com.sxcccccccc.ic2mekradfix.compat.GtCompat;
import com.sxcccccccc.ic2mekradfix.compat.MekCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方向二（闸门 1）：Mek 防辐射服 → 防 IC2 背包放射性物品辐射。
 *
 * <p>javap 确认：{@code RadiationHandler.tick} 每 20 tick 对不满足
 * {@code isImmune(player)} 且携带放射性物品的玩家施加 {@code ic2_120:radiation} 效果；
 * 效果持续期间不再查护甲，故闸门唯一 = {@code isImmune}（量子甲 || 全套 hazmat）。
 * 全套 Mek hazmat 因物品 id 含 "hazmat" 已被 IC2 谓词意外接受，但 MekaSuit 不被认可。</p>
 *
 * <p>修复：原判定 false 时，若 Mek 定义下全防护（{@link MekCompat}，屏蔽值和 ≥ 1.0，
 * 即全套 hazmat 或带辐射屏蔽单元的全套 MekaSuit）或 GT 全套 PPE（{@link GtCompat}，
 * 原生 FULL.isProtected，hazmat/quarktech 全套）→ true。把名字碰巧升级为显式保障，
 * 并补上 MekaSuit 与 GT 套装。</p>
 *
 * <p>实现说明：目标类在 intermediary 命名的 Fabric jar 里（经 Sinytra Connector 运行），
 * 无法放入编译 classpath，故用 targets 字符串 + {@code @Coerce} + 方法名解析（名字唯一，
 * 规避 descriptor 的 intermediary/SRG 差异）；handler 体内对 MC/Mek 的直接引用由
 * reobfJar 重映射为 SRG 名，与 Connector 重映射后的运行时一致。</p>
 */
@Mixin(targets = "ic2_120.content.effect.RadiationHandler", remap = false)
public abstract class Ic2RadiationHandlerMixin {

    @Inject(method = "isImmune", at = @At("RETURN"), cancellable = true, remap = false)
    private void ic2mekradfix$mekSuitsBlockIc2Radiation(@Coerce Object living,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        if (living instanceof LivingEntity entity
                && (MekCompat.hasFullShielding(entity) || GtCompat.isGtPpeProtected(entity))) {
            cir.setReturnValue(true);
        }
    }
}
