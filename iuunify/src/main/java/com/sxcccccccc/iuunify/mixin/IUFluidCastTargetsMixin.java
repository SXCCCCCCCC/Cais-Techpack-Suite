package com.sxcccccccc.iuunify.mixin;

import com.denfop.api.space.dimension.SpaceBodyDefinitionRegistry;
import com.denfop.api.space.dimension.worldgen.SpaceMaterialSet;
import com.denfop.api.space.dimension.worldgen.feature.SpaceRegionalHydrologyFeature;
import com.denfop.api.space.dimension.worldgen.feature.SpaceVolcanoPlacementLogic;
import com.denfop.proxy.ClientProxy;
import com.denfop.world.GeneratorVolcano;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 空标记 mixin：只为 {@link com.sxcccccccc.iuunify.asm.IuUnifyTransformPlugin}
 * 的 {@code preApply} 提供 9 处 {@code (IUFluid)} 强转宿主类的加载拦截点。
 *
 * <p>mixin 应用器在目标类首次加载时调用 preApply，届时 ClassNode 已读出、
 * 由 plugin 完成 checkcast/方法引用改写（见 plugin 的 javadoc 证据链与替换规则）。
 * 本类无任何注入方法（@Overwrite/@Inject/@Redirect 均无），纯"挂号"。
 *
 * <p>覆盖：客户端启动（ClientProxy.onClientSetup）、主世界火山
 * （GeneratorVolcano 构造器线程 3 处）、太空维度材质/火山/水文/天体定义
 * （SpaceMaterialSet 2 处、SpaceVolcanoPlacementLogic、SpaceRegionalHydrologyFeature、
 * SpaceBodyDefinitionRegistry）。SpaceConfiguredFeaturesBootstrap 仅数据生成
 * 路径执行，不在覆盖范围（阶段二任务清单明确不修）。
 */
@Mixin(value = {
        ClientProxy.class,
        GeneratorVolcano.class,
        SpaceMaterialSet.class,
        SpaceVolcanoPlacementLogic.class,
        SpaceRegionalHydrologyFeature.class,
        SpaceBodyDefinitionRegistry.class
})
public interface IUFluidCastTargetsMixin {
}
