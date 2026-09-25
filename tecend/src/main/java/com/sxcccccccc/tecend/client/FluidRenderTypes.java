package com.sxcccccccc.tecend.client;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.registry.ModFluids;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 金猪流体登记为不透明渲染（熔岩那种）。
 *
 * <p>1.20.1 Forge 的流体渲染层**不在** {@code IClientFluidTypeExtensions} 里（javap 47.4.10 实证：
 * 那个接口只有 tint / still / flowing / overlay / 雾那几组方法，没有 getRenderType），
 * 而是 {@code LiquidBlockRenderer} 通过 {@code ItemBlockRenderTypes.getRenderLayer(FluidState)} 取的 ——
 * 所以在客户端初始化时往这张表里登记。不登记就是默认的 translucent（熔融金属看着会发透）。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FluidRenderTypes {
    private FluidRenderTypes() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // 熔融金属：不透明
            ItemBlockRenderTypes.setRenderLayer(ModFluids.GOLD_PIG.get(), RenderType.solid());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_GOLD_PIG.get(), RenderType.solid());

            // 三种溶液：半透明（像水那样）
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FRAGMENT_SOLUTION.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_FRAGMENT_SOLUTION.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ACTIVATED_SOLUTION.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_ACTIVATED_SOLUTION.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.ACIDIC_SOLUTION.get(), RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(ModFluids.FLOWING_ACIDIC_SOLUTION.get(), RenderType.translucent());
        });
    }
}
