package com.sxcccccccc.tecend.common;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

/**
 * 通用流体类型：贴图路径 + 物理参数由构造参数给。
 *
 * <p>贴图是按颜色程序生成的（见 assets/tecend/textures/block/*_still.png 与 *_flowing.png），
 * 所以不用每做一个液体就写一个类。渲染层在 {@code client/FluidRenderTypes} 里登记。</p>
 *
 * <p>注意 1.20.1 Forge 的 {@code IClientFluidTypeExtensions} 没有 {@code getRenderType}
 * （javap 实证），渲染层走 {@code ItemBlockRenderTypes} —— 别在这里找它。</p>
 */
public class TecendFluidType extends FluidType {

    private final ResourceLocation still;
    private final ResourceLocation flowing;

    public TecendFluidType(ResourceLocation still, ResourceLocation flowing,
                           int density, int viscosity, int temperature) {
        super(FluidType.Properties.create()
                .density(density)
                .viscosity(viscosity)
                .temperature(temperature)
                .canConvertToSource(false)
                .canDrown(false)
                .canSwim(false)
                .supportsBoating(false));
        this.still = still;
        this.flowing = flowing;
    }

    /** 水一样的物理参数（溶液类默认） */
    public static TecendFluidType solution(ResourceLocation still, ResourceLocation flowing) {
        return new TecendFluidType(still, flowing, 1000, 1000, 300);
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return still;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowing;
            }
        });
    }
}
