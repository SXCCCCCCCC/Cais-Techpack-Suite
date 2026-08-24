package com.sxcccccccc.iuunify.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.componets.Fluids;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阶段二 C 线：输入侧 tag 化的登记侧。
 *
 * <p>目标类 {@code com.denfop.componets.Fluids} 是所有 IU 机器流体罐的创建者——
 * 全部 {@code addTank(...)} 重载最终汇聚到
 * {@code addTank(Fluids$InternalFluidTank)}（jar 字节码实证，见
 * {@code iu_patched_readable/com/denfop/componets/Fluids.java}）。在该方法 TAIL
 * 把（罐 → owner BE）交给 {@link InputTagRegistry} 按规则表解析出应宽限的 tag。
 *
 * <p>重载纪律（mc-bugfix-sop §5 实踩教训）：{@code addTank} 有 7 个重载，method
 * 必须写完整描述符锁定唯一目标，否则 Mixin 0.8.5 按类文件顺序取第一个同名方法。
 *
 * <p>输出侧/判定侧零影响：本 mixin 只登记"宽限 tag"，不替换任何谓词——罐的原始
 * 精确 id 谓词（焦炉产物罐 creosote、判定校验等）原样保留。
 */
@Mixin(value = Fluids.class)
public abstract class FluidsTankRegisterMixin {

    /**
     * 目标签名（jar javap 实证）：{@code public Fluids$InternalFluidTank
     * addTank(Fluids$InternalFluidTank)}；描述符
     * {@code (Lcom/denfop/componets/Fluids$InternalFluidTank;)Lcom/denfop/componets/Fluids$InternalFluidTank;}。
     * remap=false：目标类/方法是 mod 成员，不参与原版映射（字符串原样进 jar）。
     *
     * <p>0.3.2 修复（同 FluidNameRedirectMixin 的排雷序列，CallbackInjector
     * getDescriptor 字节码实证）：@Inject handler 参数只允许 [目标方法参数] +
     * [CallbackInfo]，this 不占参数位——原写法 {@code (Fluids self, tank, CIR)}
     * 会 InvalidInjectionException "Expected (tank, CIR)"。owner 改经
     * {@code @Shadow AbstractComponent.getParent()}（非 final，abstract shadow）
     * 在 handler 内取得再传入
     * {@link InputTagRegistry#register(BlockEntityBase, Fluids.InternalFluidTank)}。
     */
    @Shadow(remap = false)
    public abstract BlockEntityBase getParent();

    @Inject(
            method = "addTank(Lcom/denfop/componets/Fluids$InternalFluidTank;)Lcom/denfop/componets/Fluids$InternalFluidTank;",
            at = @At("TAIL"),
            require = 1,
            remap = false
    )
    private void iuunify$registerTank(Fluids.InternalFluidTank tank, CallbackInfoReturnable<Fluids.InternalFluidTank> cir) {
        InputTagRegistry.register(getParent(), tank);
    }
}
