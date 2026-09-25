package com.sxcccccccc.gtnoboomfix.mixin;

import com.gregtechceu.gtceu.common.machine.storage.CreativeEnergyContainerMachine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造能源容器的超压爆炸是<b>延迟爆炸</b>（javap 核实 7.5.3 jar）：
 * {@code acceptEnergyFromNetwork} 超压分支只置 {@code doExplosion = true}
 * （不直接炸），真实爆炸在 {@code updateEnergyTick()} 的 {@code offsetTimer % 20 == 0}
 * 检查里直调 {@code Level.explode(...)}（威力 1，ExplosionInteraction.NONE）——
 * 它不走 IExplosionMachine 咽喉点（本类只实现 ILaserContainer/IUIMachine）。
 *
 * <p>手法：每 tick 在 updateEnergyTick 开头清标志 —— 标志永远活不到 %20 检查，
 * 爆炸永不触发；其余逻辑（能量输出泵、GUI、返回语义）原样保留。
 *
 * <p>remap=false：目标为 GT mod 类；@Shadow 只读写目标类自有私有字段
 * doExplosion，无原版成员引用。
 */
@Mixin(value = CreativeEnergyContainerMachine.class, remap = false)
public abstract class CreativeEnergyContainerMachineMixin {

    @Shadow
    private boolean doExplosion;

    @Inject(method = "updateEnergyTick", at = @At("HEAD"))
    private void gtnoboomfix$clearExplosionFlag(CallbackInfo ci) {
        doExplosion = false;
    }
}
