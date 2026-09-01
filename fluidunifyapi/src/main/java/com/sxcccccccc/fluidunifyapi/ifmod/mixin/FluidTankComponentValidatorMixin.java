package com.sxcccccccc.fluidunifyapi.ifmod.mixin;

import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.sxcccccccc.fluidunifyapi.core.MachineAdapters;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry.Decision;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Predicate;

/**
 * IF 通用咽喉点：Titanium {@link FluidTankComponent}（IF 全部 23 台 validator 机器
 * 的油箱基类，<b>不是</b> Forge FluidTank——本 mod 永不 mixin
 * net.minecraftforge.fluids.capability.templates.FluidTank）。
 *
 * <p>@Overwrite fill：原生体 = {@code tankAction.canFill() ? super.fill(...) : 0}，
 * super.fill 即 Forge FluidTank.fill（validator 门禁 + 非空罐 isFluidEqual 防混装
 * + 合并）——防混装语义完全原生、逐字节不动，我们只替换"validator 是谁"：</p>
 * <ul>
 *   <li>NEUTRAL：原 validator 照常；</li>
 *   <li>ACCEPT：临时换 always-true 后走原生 super.fill（合并/防混装检查仍生效），
 *       finally 恢复原 validator——同一调用栈内完成，无跨线程状态泄漏；</li>
 *   <li>REJECT：直接 0（REPLACE 模式否决模板流体）。</li>
 * </ul>
 *
 * <p>输入口定位 = 探针：哪个补丁的模板流体能被本罐原生 validator 接受，哪个补丁
 * 就命中本罐（无需逐机罐名表）。机器 id 从 componentHarness 类反查
 * （fill 发生时 harness 必已挂载——validators 只在机器运行期被调用）。</p>
 */
@Mixin(value = FluidTankComponent.class, remap = false)
public abstract class FluidTankComponentValidatorMixin {

    @Shadow(remap = false)
    protected Predicate<FluidStack> validator;

    private static final Predicate<FluidStack> ALWAYS_TRUE = stack -> true;

    /**
     * @reason 补丁驱动的 validator 替换（见类注释）；原生体逻辑在 ACCEPT 分支经
     * super.fill 完整保留。
     * @author fluidunifyapi
     */
    @Overwrite(remap = false)
    public int fill(FluidStack resource, FluidAction action) {
        FluidTankComponent<?> self = (FluidTankComponent<?>) (Object) this;
        if (!self.getTankAction().canFill()) {
            return 0;
        }
        if (resource == null || resource.isEmpty()) {
            return 0;
        }
        Object harness = self.getComponentHarness();
        if (harness == null) {
            return superFill(resource, action);
        }
        String machineId = MachineAdapters.machineIdOf(harness.getClass());
        if (machineId == null || !UnifiedFluidRegistry.hasPatch(machineId)) {
            return superFill(resource, action);
        }

        Fluid fluid = resource.getFluid();
        Predicate<FluidStack> nativeValidator = this.validator;
        // 探针：找命中本罐的补丁（模板流体被原生 validator 接受 = 该补丁目标口）
        for (var patch : UnifiedFluidRegistry.rawPatches(machineId)) {
            Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
            if (template == null) {
                continue;
            }
            boolean portMatched = nativeValidator == null
                    || nativeValidator.test(new FluidStack(template, 1));
            if (!portMatched) {
                continue;
            }
            Decision d = UnifiedFluidRegistry.decide(machineId, template, fluid);
            if (d == Decision.ACCEPT) {
                // 换宽限 validator 走原生 super.fill：门禁宽限、防混装/合并逐字节原生
                Predicate<FluidStack> old = this.validator;
                this.validator = ALWAYS_TRUE;
                try {
                    return superFill(resource, action);
                } finally {
                    this.validator = old;
                }
            }
            if (d == Decision.REJECT) {
                return 0;
            }
            // NEUTRAL：该补丁与本流体无关，继续查下一补丁
        }
        return superFill(resource, action);
    }

    /** invokespecial 语义直调 FluidTank.fill（不经过被 overwrite 的本方法）。 */
    private int superFill(FluidStack resource, FluidAction action) {
        return ((FluidTankComponent<?>) (Object) this).fillForced(resource, action);
    }
}
