package com.sxcccccccc.iufix.mixin;

import com.denfop.api.pollution.layer.EnvironmentalLayerManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.3.4 用户裁决 E2：污染只保留视觉效果，禁一切世界实质改造。
 *
 * <p>{@code EnvironmentalLayerManager.tick}（static，唯一 public 入口，字节码确认）仅
 * 调度 4 个纯方块改造：processAirToxicOvergrowth（毒藤/苔藓疯长）、processRadiationErosion
 * （辐射侵蚀）、processSoot（煤烟堆积）、processRadiationDust（辐射尘堆积）——无数值/
 * 视觉逻辑，整体取消。@Mixin remap=false，static 方法 handler 无 this。
 */
@Mixin(value = EnvironmentalLayerManager.class, remap = false)
public abstract class EnvironmentalLayerTickDisableMixin {

    @Inject(method = "tick(Lnet/minecraft/server/level/ServerLevel;)V",
            at = @At("HEAD"), cancellable = true)
    private static void iufix$disableEnvironmentalLayer(ServerLevel level, CallbackInfo ci) {
        ci.cancel();
    }
}
