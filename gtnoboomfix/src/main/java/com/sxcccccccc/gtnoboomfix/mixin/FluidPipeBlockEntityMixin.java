package com.sxcccccccc.gtnoboomfix.mixin;

import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 流体管道损坏全链免疫（javap 核实 7.5.3 jar）：
 * {@code checkAndDestroy(FluidStack)} 判定 5 种损坏（burning 超温 /
 * leaking 气体泄漏 / corroding 酸蚀 / shattering 低温冻碎 / melting 等离子熔），
 * 命中后 {@code destroyPipe(...)} 落地：流体损耗 + 热/寒/化学伤害 +
 * 小爆炸 + 1/4 概率点燃周围方块 + 管道原地变火方块 + 管道移除。
 *
 * <p>手法：方法开头直接 return（HEAD cancel）——管道对所有流体全免疫，
 * destroyPipe 永不执行，爆炸/火灾/灼伤/腐蚀一并消除；流体正常输送保留。
 * 站管旁灼伤（EntityDamageUtil temperature/chemical，BYPASS_ARMOR）只在这条
 * 链上触发，随之消失。代价：管道材质差异（耐温/气密/耐酸）失去意义——
 * 超简化包可接受。
 *
 * <p>remap=false：目标为 GT mod 类；handler 为空体，无原版成员引用。
 */
@Mixin(value = FluidPipeBlockEntity.class, remap = false)
public abstract class FluidPipeBlockEntityMixin {

    @Inject(method = "checkAndDestroy", at = @At("HEAD"), cancellable = true)
    private void gtnoboomfix$pipeDamageImmune(FluidStack stack, CallbackInfo ci) {
        ci.cancel();
    }
}
