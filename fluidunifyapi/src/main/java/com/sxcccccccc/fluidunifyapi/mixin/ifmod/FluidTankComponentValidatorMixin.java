package com.sxcccccccc.fluidunifyapi.mixin.ifmod;

import com.hrznstudio.titanium.component.fluid.FluidTankComponent;
import com.sxcccccccc.fluidunifyapi.core.MachineAdapters;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry.Decision;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * IF 通用咽喉点：Titanium {@link FluidTankComponent}（IF 全部 23 台 validator 机器
 * 的油箱基类，<b>不是</b> Forge FluidTank——本 mod 永不 mixin
 * net.minecraftforge.fluids.capability.templates.FluidTank）。
 *
 * <p>@Overwrite fill：原生体 = {@code tankAction.canFill() ? super.fill(...) : 0}。
 * validator 字段在父类 Forge FluidTank（@Shadow 字段不爬父链，够不到），但
 * {@code isFluidValid()}（validator 判定的公开通道）与 {@code fillForced()}
 * （= 原生 fill 去掉 validator 检查，isEmpty/isFluidEqual 防混装/合并逐字节保留）
 * 都是公开 API——探针用前者、放行用后者，无需反射无需换 validator：</p>
 * <ul>
 *   <li>NEUTRAL：走 fillForced（= 原生语义）；</li>
 *   <li>ACCEPT：走 fillForced（门禁宽限、防混装/合并仍原生）；</li>
 *   <li>REJECT：直接 0（REPLACE 模式否决模板流体）。</li>
 * </ul>
 *
 * <p>输入口定位 = 探针：哪个补丁的模板流体能被本罐原生 validator 接受，哪个补丁
 * 就命中本罐（无需逐机罐名表）。机器 id 从 componentHarness 类反查
 * （fill 发生时 harness 必已挂载——validators 只在机器运行期被调用）。</p>
 */
@Mixin(value = FluidTankComponent.class, remap = false)
public abstract class FluidTankComponentValidatorMixin {

    /**
     * @reason 补丁驱动的 validator 宽限（见类注释）；原生体逻辑在
     * ACCEPT/NEUTRAL 分支经 fillForced 完整保留。
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
            return self.fillForced(resource, action);
        }
        String machineId = MachineAdapters.machineIdOf(harness.getClass());
        if (machineId == null || !UnifiedFluidRegistry.hasPatch(machineId)) {
            return self.fillForced(resource, action);
        }

        Fluid fluid = resource.getFluid();
        // 探针：找命中本罐的补丁（模板流体被原生 validator 接受 = 该补丁目标口）
        for (var patch : UnifiedFluidRegistry.rawPatches(machineId)) {
            Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
            if (template == null) {
                continue;
            }
            boolean portMatched = self.isFluidValid(new FluidStack(template, 1));
            if (!portMatched) {
                continue;
            }
            Decision d = UnifiedFluidRegistry.decide(machineId, template, fluid);
            if (d == Decision.ACCEPT) {
                return self.fillForced(resource, action);
            }
            if (d == Decision.REJECT) {
                return 0;
            }
            // NEUTRAL：该补丁与本流体无关，继续查下一补丁
        }
        return self.fillForced(resource, action);
    }
}
