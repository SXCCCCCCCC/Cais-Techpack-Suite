package com.sxcccccccc.ic2mekradfix.mixin;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.sxcccccccc.ic2mekradfix.compat.Ic2Compat;
import com.sxcccccccc.ic2mekradfix.compat.MekCompat;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 方向三（GT 侧）：IC2 / Mek 防辐射服 → 防 GT 致癌辐射。
 *
 * <p>GT 辐射防护判定唯一汇聚点 = {@code HazardProperty$ProtectionType.isProtected(LivingEntity)}
 * （javap 实证 7.5.3 jar；普通 Forge 类，非 Connector 翻译类）。FULL = 四护甲槽每件为
 * {@code ArmorComponentItem} 且 {@code isPPE()}，或带 {@code gtceu:ppe_armor} tag。
 * 调用点仅两处——背包放射性物品（CommonEventListener.tickPlayerHazards）与环境辐射区
 * （LocalizedHazardSavedData）——注入 isProtected 一处覆盖两者。</p>
 *
 * <p>修复：原判定 false 时，若 IC2 认为辐射免疫（{@link Ic2Compat}，量子甲或全套 hazmat）
 * 或 Mek 定义下全防护（{@link MekCompat}，屏蔽值和 ≥ 1.0）→ true。仅对 FULL 生效
 * （MASK/HANDS 属吸入/接触类危害，不在本 fix 范围，语义口径「全套才免疫」）。</p>
 *
 * <p>语义：只阻断致癌 progression 吸收、不清除已累积；GT 只磨损自家 PPE 装备
 * （damageEquipment 只对 ArmorComponentItem+isPPE 或 ppe_armor tag 生效），IC2/Mek
 * 装备不被磨损（可接受的不对称）。</p>
 */
@Mixin(value = HazardProperty.ProtectionType.class, remap = false)
public abstract class GtProtectionTypeMixin {

    @Inject(method = "isProtected", at = @At("RETURN"), cancellable = true, remap = false)
    private void ic2mekradfix$ic2AndMekSuitsBlockGtRadiation(LivingEntity entity,
                                                             CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        if ((Object) this != (Object) HazardProperty.ProtectionType.FULL) {
            return;
        }
        if (Ic2Compat.isIc2RadiationImmune(entity) || MekCompat.hasFullShielding(entity)) {
            cir.setReturnValue(true);
        }
    }
}
