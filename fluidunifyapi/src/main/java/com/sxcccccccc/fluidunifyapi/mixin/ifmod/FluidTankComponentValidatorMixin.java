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

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Predicate;

/**
 * IF 通用咽喉点：Titanium {@link FluidTankComponent}（IF 全部 23 台 validator 机器
 * 的油箱基类，<b>不是</b> Forge FluidTank——本 mod 永不 mixin
 * net.minecraftforge.fluids.capability.templates.FluidTank）。
 *
 * <p>@Overwrite fill：原生体 = {@code tankAction.canFill() ? super.fill(...) : 0}。
 * 关键事实（2026-09-01 javap 3.8.35 实证）：{@code fillForced} 只跳过
 * tankAction 门禁，<b>不跳过 validator</b>——它 invokespecial Forge
 * FluidTank.fill，而 Forge fill 第一行就是 {@code isFluidValid} 检查。
 * 所以 ACCEPT 放行必须临时把 validator 换成 always-true 再走 fillForced
 * （= Forge 原生 fill：容量/模拟/非空罐 isFluidEqual 防混装逐字节保留），
 * finally 恢复。原生 validator 经反射按罐缓存（validator 字段在 Forge
 * FluidTank 父类，@Shadow 够不到；本 mod 从不 mixin Forge FluidTank）。</p>
 * <ul>
 *   <li>NEUTRAL：走 fillForced（= 原生语义，validator 未被替换）；</li>
 *   <li>ACCEPT：validator 临时换 always-true + fillForced，finally 恢复；</li>
 *   <li>REJECT：直接 0（REPLACE 模式否决模板流体）。</li>
 * </ul>
 *
 * <p>输入口定位 = 探针：哪个补丁的模板流体能被本罐原生 validator 接受，哪个补丁
 * 就命中本罐（无需逐机罐名表）。机器 id 从 componentHarness 类反查
 * （fill 发生时 harness 必已挂载——validators 只在机器运行期被调用）。</p>
 */
@Mixin(value = FluidTankComponent.class, remap = false)
public abstract class FluidTankComponentValidatorMixin {

    private static final Predicate<FluidStack> ALWAYS_TRUE = stack -> true;

    /** 罐 → 原生 validator（首次 fill 时反射缓存；只在换宽限前读，缓存永不改写）。 */
    private static final Map<FluidTankComponent<?>, Predicate<FluidStack>> NATIVE_VALIDATORS =
            Collections.synchronizedMap(new WeakHashMap<>());

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
                // 换宽限 validator 走原生 fillForced（= Forge FluidTank.fill，防混装/
                // 合并逐字节原生）；拿不到原生 validator 就按原生语义（拒绝）处理。
                Predicate<FluidStack> original = nativeValidatorOf(self);
                if (original == null) {
                    return 0;
                }
                self.setValidator(ALWAYS_TRUE);
                try {
                    return self.fillForced(resource, action);
                } finally {
                    self.setValidator(original);
                }
            }
            if (d == Decision.REJECT) {
                return 0;
            }
            // NEUTRAL：该补丁与本流体无关，继续查下一补丁
        }
        return self.fillForced(resource, action);
    }

    /**
     * 反射读 Forge FluidTank 的 protected validator 字段（运行时官方名，Forge 类
     * 不做 SRG 重映射；本 mod 从不 mixin Forge FluidTank，反射读字段合规）。
     */
    @SuppressWarnings("unchecked")
    private static Predicate<FluidStack> nativeValidatorOf(FluidTankComponent<?> tank) {
        return NATIVE_VALIDATORS.computeIfAbsent(tank, t -> {
            try {
                Field f = net.minecraftforge.fluids.capability.templates.FluidTank.class
                        .getDeclaredField("validator");
                f.setAccessible(true);
                return (Predicate<FluidStack>) f.get(t);
            } catch (Throwable e) {
                return null;
            }
        });
    }
}
