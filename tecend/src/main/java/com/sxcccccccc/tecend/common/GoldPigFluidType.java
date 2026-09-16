package com.sxcccccccc.tecend.common;

import com.sxcccccccc.tecend.TecEnd;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidType;

import java.util.function.Consumer;

/**
 * 「金猪」流体的类型定义（第 ⑨ 步的产物：熔融金 + 一只猪）。
 *
 * <p>客户端表现：still / flowing 两张贴图都是动画（64 帧，fm 1），走 {@link IClientFluidTypeExtensions}。</p>
 *
 * <p><b>渲染层不在这里设</b>：1.20.1 Forge 的 {@code IClientFluidTypeExtensions} 没有 {@code getRenderType}
 * （javap 47.4.10 实证，只有 tint / still / flowing / overlay / 雾那几组），流体的渲染层由
 * {@code ItemBlockRenderTypes.getRenderLayer(FluidState)} 决定 —— 登记在 {@code client/FluidRenderTypes}。</p>
 */
public class GoldPigFluidType extends FluidType {

    /** 贴图路径（图在 assets/tecend/textures/block/ 下，配同名 .mcmeta 播放动画） */
    public static final ResourceLocation STILL = new ResourceLocation(TecEnd.MOD_ID, "block/gold_pig_still");
    public static final ResourceLocation FLOWING = new ResourceLocation(TecEnd.MOD_ID, "block/gold_pig_flowing");

    public GoldPigFluidType() {
        super(FluidType.Properties.create()
                .descriptionId("fluid_type.tecend.gold_pig")
                .density(3000)          // 比水重：像熔岩一样沉
                .viscosity(6000)        // 粘：流得慢
                .temperature(1300)      // 熔融金属
                .canConvertToSource(false)
                .canDrown(false)
                .canSwim(false)
                .supportsBoating(false));
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }
        });
    }
}
