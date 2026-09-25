package com.sxcccccccc.iufix.client;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 1.1.0：在客户端启动期把污染水着色 handler 注册进 fabric-rendering-fluids 的 registry。
 *
 * <p>注册时机：{@link FMLClientSetupEvent}（MOD 总线，仅客户端、每启动只触发一次，天然幂等）。
 * 此时 Connector 已挂载 fabric-api，registry 已初始化；渲染在首次帧绘制时才发生，远晚于此时。
 *
 * <p>注册对象：minecraft:water 与 flowing_water（IU 原 mixin 同时覆盖两者）。
 * 运行时 fabric-rendering-fluids jar 由 fabric-api 主 jar 的 META-INF/jarjar 提供，无需打入本 mod。
 */
@Mod.EventBusSubscriber(modid = "iufix", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class IufixPollutionWaterRegistration {

    private static final FluidRenderHandler HANDLER = new IufixPollutionWaterHandler();

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.WATER, HANDLER);
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_WATER, HANDLER);
    }
}
