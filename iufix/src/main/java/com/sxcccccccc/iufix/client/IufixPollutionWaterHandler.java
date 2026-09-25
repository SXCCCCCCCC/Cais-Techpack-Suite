package com.sxcccccccc.iufix.client;

import com.denfop.api.pollution.client.PollutionLocalTintCache;
import com.denfop.api.pollution.client.PollutionLocalTintState;
import com.denfop.api.pollution.client.PollutionVisualColors;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * 1.1.0 污染水着色：以 fabric-rendering-fluids 的 {@link FluidRenderHandler} 替代 IU 原
 * {@code MixinLiquidBlockRendererTint}（fabric 环境下与 {@code FluidRendererMixin} 同点
 * Redirect 冲突，链路失效）。
 *
 * <p>分发机制（fabric-rendering-fluids 3.0.29，jarjar 于 fabric-api 0.92.6）：
 * <ul>
 *   <li>{@code tesselate} HEAD（tessellateViaHandler）对<b>任意</b>流体按
 *       {@code fluidState.getType()} 查 {@code FluidRenderHandlerRegistryImpl.getOverride}，
 *       不区分 Fabric/Forge 流体类型——注册 Forge 流体（minecraft:water）即可命中。</li>
 *   <li>handler 非 null 时调 {@code handler.renderFluid(...)} 并 cancel 原方法；本 handler
 *       使用接口默认 {@code renderFluid}，其委托 {@code RegistryImpl.renderFluid} 重入
 *       原版 {@code LiquidBlockRenderer.tesselate}（fabric_customRendering ThreadLocal 防
 *       重入）——几何/贴图保持原版。</li>
 *   <li>重入渲染中的 {@code getTintColor} 调用点被 fabric modTintColor Redirect：
 *       handler 非 null 时改为 {@code handler.getFluidColor(ctr.view, ctr.pos, ctr.fluidState) | 0xFF000000}。</li>
 *   <li>重入前的 HEAD 收尾 {@code ctr.getSprites} 调本 handler 的
 *       {@link #getFluidSprites}（接口 abstract 方法），填充 ctr.sprites 供原版渲染使用。</li>
 * </ul>
 *
 * <p>着色公式逐行复刻 IU 13 源码 {@code MixinLiquidBlockRendererTint.iu$applyPollutionWaterTint}
 * （10 jar 字节码一致，仅 alpha 由 fabric 侧强制 255）。
 */
public class IufixPollutionWaterHandler implements FluidRenderHandler {

    @Override
    public TextureAtlasSprite[] getFluidSprites(BlockAndTintGetter view, BlockPos pos, FluidState state) {
        // 与原版 handler==null 分支（fabric redirectSprites 默认）行为一致：Forge 原版贴图
        return ForgeHooksClient.getFluidSprites(view, pos, state);
    }

    @Override
    public int getFluidColor(BlockAndTintGetter getter, BlockPos pos, FluidState fluidState) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluidState);
        if (pos == null || getter == null) {
            return extensions.getTintColor(fluidState, getter, pos);
        }
        if (!fluidState.is(Fluids.WATER) && !fluidState.is(Fluids.FLOWING_WATER)) {
            return extensions.getTintColor(fluidState, getter, pos);
        }

        BlockPos samplePos = PollutionLocalTintCache.getCellSamplePos(pos);
        int original = extensions.getTintColor(fluidState, getter, samplePos);
        PollutionLocalTintState local = PollutionLocalTintCache.get(samplePos);
        if (!local.isActive()) {
            return original;
        }

        int alpha = original >>> 24 & 0xFF;
        if (alpha <= 0) {
            alpha = 255;
        }
        int originalRgb = original & 0xFFFFFF;

        float soilDominance = this.getSoilDominance(local);
        float radiationDominance = this.getRadiationDominance(local);
        float strength = Mth.clamp(
                Math.max(local.getBiomeInfluence() * (0.92F + soilDominance * 0.08F - radiationDominance * 0.12F),
                        local.getAirInfluence() * 0.34F)
                        + local.getSoilInfluence() * (0.2F + soilDominance * 0.12F)
                        + local.getRadiationInfluence() * 0.18F,
                0.0F, 1.0F);
        int polluted = PollutionVisualColors.applyBiomeDegradation(
                originalRgb, local.getWaterTintColor(), strength, local.getSaturationLoss() * 0.95F);

        float muddiness = PollutionVisualColors.smoothstep01(Mth.clamp(
                local.getSoilInfluence() * 0.95F + local.getBiomeInfluence() * 0.32F
                        - local.getRadiationInfluence() * 0.08F,
                0.0F, 1.0F));
        if (muddiness > 0.001F) {
            polluted = PollutionVisualColors.desaturate(polluted, muddiness * 0.3F);
            polluted = PollutionVisualColors.lerpColor(polluted, PollutionVisualColors.SOIL_WATER, muddiness * 0.58F);
            polluted = PollutionVisualColors.multiply(polluted, Mth.clamp(1.0F - muddiness * 0.24F, 0.6F, 1.0F));
        }

        if (local.getRadiationInfluence() > 0.2F) {
            polluted = PollutionVisualColors.applyRadiationWaterBoost(polluted, local.getRadiationInfluence() * 0.9F);
        }

        return alpha << 24 | polluted & 0xFFFFFF;
    }

    private float getSoilDominance(PollutionLocalTintState local) {
        float sum = local.getAirInfluence() + local.getSoilInfluence() + local.getRadiationInfluence();
        return sum <= 1.0E-4F ? 0.0F : Mth.clamp(local.getSoilInfluence() / sum, 0.0F, 1.0F);
    }

    private float getRadiationDominance(PollutionLocalTintState local) {
        float sum = local.getAirInfluence() + local.getSoilInfluence() + local.getRadiationInfluence();
        return sum <= 1.0E-4F ? 0.0F : Mth.clamp(local.getRadiationInfluence() / sum, 0.0F, 1.0F);
    }
}
