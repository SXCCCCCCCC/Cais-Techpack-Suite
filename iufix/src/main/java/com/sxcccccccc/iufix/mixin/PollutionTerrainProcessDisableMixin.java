package com.sxcccccccc.iufix.mixin;

import com.denfop.api.pollution.PollutionManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.3.4 用户裁决 E2：污染只保留视觉效果，禁一切世界实质改造。
 *
 * <p>取消 {@link PollutionManager} 三个纯地形改造入口（可读源码确认均为纯方块改造：
 * 按污染等级调度方块替换/植被清除，无数值产生/扩散/渲染副作用）：
 * <ul>
 *   <li>{@code processSoilTerrain}——泥化（表面方块替换）；</li>
 *   <li>{@code processVegetationDecay}——植被枯死；</li>
 *   <li>{@code processLeafDecay}——树叶脱落。</li>
 * </ul>
 * 数值链（pollutionAir/pollutionSoil 产生、扩散、ChunkLevel 更新）、同步、视觉链
 * （雾/遮罩/块着色/粉尘粒子）均在 {@code PollutionManager.tick/update} 等方法内，
 * 本 mixin 不触碰。@Mixin remap=false，denfop 方法运行时名即官方名。
 */
@Mixin(value = PollutionManager.class, remap = false)
public abstract class PollutionTerrainProcessDisableMixin {

    @Inject(method = "processSoilTerrain(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"), cancellable = true)
    private void iufix$disableSoilTerrain(ServerLevel world, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "processVegetationDecay(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"), cancellable = true)
    private void iufix$disableVegetationDecay(ServerLevel world, CallbackInfo ci) {
        ci.cancel();
    }

    @Inject(method = "processLeafDecay(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"), cancellable = true)
    private void iufix$disableLeafDecay(ServerLevel world, CallbackInfo ci) {
        ci.cancel();
    }
}
