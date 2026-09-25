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
 * 方向二（闸门 2）：Mek 防辐射服 → 防 IC2 核反应堆热辐射。
 *
 * <p>javap 确认：{@code NuclearReactorBlockEntity.applyHeatEffects} 对附近玩家调
 * 该类自己的 {@code hasFullHazmat}（与 RadiationHandler 的判定重复实现，谓词只含
 * "hazmat"、不含 rubber_boots）决定是否免于核堆热伤害。全套 Mek hazmat 同样因 id
 * 含 "hazmat" 已被意外接受，但 MekaSuit 不被认可。</p>
 *
 * <p>修复：原判定 false 时，Mek 全防护（{@link MekCompat}）或 GT 全套 PPE
 * （{@link GtCompat}）→ true。实现方式与 {@link Ic2RadiationHandlerMixin} 相同
 * （targets 字符串 + {@code @Coerce} + 方法名解析，remap=false）。</p>
 */
@Mixin(targets = "ic2_120.content.block.nuclear.NuclearReactorBlockEntity", remap = false)
public abstract class Ic2NuclearReactorMixin {

    @Inject(method = "hasFullHazmat", at = @At("RETURN"), cancellable = true, remap = false)
    private void ic2mekradfix$mekSuitsBlockReactorHeat(@Coerce Object living,
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
