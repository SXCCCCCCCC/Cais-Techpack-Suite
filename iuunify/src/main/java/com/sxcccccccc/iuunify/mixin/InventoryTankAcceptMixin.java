package com.sxcccccccc.iuunify.mixin;

import com.denfop.inventory.InventoryTank;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 0.3.4 任务一分支二：UI 槽位接受判定的 tag 宽限（罐直连槽）。
 *
 * <p>目标类 {@code InventoryTank} 是"罐内容即验收标准"的流体输入/输出槽
 * （{@code acceptsLiquid = tank.isEmpty() || tank.getFluid().getFluid() == fluid}，
 * 反编译实证；gas_combiner、蒸汽机等输入槽走这里）。在 RETURN 处宽限：
 * 罐内流体与传入流体同属一个统一 tag 即放行（例如罐里是 IE 杂酚油时，
 * ic2 杂酚油单元也能放进槽；罐里是 ic2 蒸馏水时，mek 蒸馏水单元同理）。
 * 罐空时原判定已放行，不受影响。
 *
 * <p>访问器：tank 是 public final 字段（0.3.3 铁律：直接经
 * {@code ((InventoryTank)(Object)this).tank} 访问，无 @Shadow）。
 */
@Mixin(value = InventoryTank.class)
public abstract class InventoryTankAcceptMixin {

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
        IFluidTank tank = ((InventoryTank) (Object) this).tank;
        FluidStack fs = tank.getFluid();
        if (!fs.isEmpty() && InputTagRegistry.sameUnifiedTag(fs.getFluid(), fluid)) {
            cir.setReturnValue(true);
        }
    }
}
