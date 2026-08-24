package com.sxcccccccc.iuunify.mixin;

import com.denfop.inventory.InventoryFluidByList;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * 0.3.4 任务一分支二：UI 槽位接受判定的 tag 宽限（按清单机器）。
 *
 * <p>目标类 {@code InventoryFluidByList} 是"按流体清单验收"的流体输入槽
 * （生物质发电机、循环机等：{@code acceptsLiquid = usually || acceptedFluids.contains(fluid)}，
 * 反编译实证）。在 RETURN 处宽限：清单内的流体与传入流体同属一个统一 tag
 * （跨 mod 同义流体，如 ic2_120:biomass × industrialupgrade:biomass）即放行。
 * 语义 = "清单或 tag"并集：未入 12 统一 tag 的流体恒保持原判定，行为不变。
 *
 * <p>配合任务一分支一的物品侧桥接：IC2R 桶/单元现在能进 UI 槽了，但
 * m_7013_ 的 acceptsLiquid 检查仍按精确 id——没有本宽限，生物质发电机的
 * ic2 生物质单元与 IU 生物质槽位跨 id 不匹配仍会被拒。
 *
 * <p>访问器：getAcceptedFluids() 是 public（无需 @Shadow，0.3.3 铁律）。
 */
@Mixin(value = InventoryFluidByList.class)
public abstract class InventoryFluidByListAcceptMixin {

    /**
     * 目标（jar javap 实证）：{@code protected boolean
     * acceptsLiquid(net.minecraft.world.level.material.Fluid)}；描述符
     * {@code (Lnet/minecraft/world/level/material/Fluid;)Z}。
     * remap=false：目标类/方法是 mod 成员。
     */
    @Inject(
            method = "acceptsLiquid(Lnet/minecraft/world/level/material/Fluid;)Z",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void iuunify$tagWiden(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            return;
        }
        Set<Fluid> accepted = ((InventoryFluidByList) (Object) this).getAcceptedFluids();
        if (accepted == null || accepted.isEmpty()) {
            return;
        }
        for (Fluid f : accepted) {
            if (InputTagRegistry.sameUnifiedTag(f, fluid)) {
                cir.setReturnValue(true);
                return;
            }
        }
    }
}
