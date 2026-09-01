package com.sxcccccccc.fluidunifyapi.mixin.iu;

import com.denfop.componets.Fluids;
import com.sxcccccccc.fluidunifyapi.iu.IuTankOwnerRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IU 咽喉点 1：罐登记时记录所属 BE 类（iuunify FluidsTankRegisterMixin 同款注入点，
 * 包内实证所有 addTank 重载都汇聚到单参版本）。探针咨询路径依赖该登记。
 */
@Mixin(value = Fluids.class, remap = false)
public abstract class IuTankRegisterMixin {

    @Inject(
            method = "addTank(Lcom/denfop/componets/Fluids$InternalFluidTank;)Lcom/denfop/componets/Fluids$InternalFluidTank;",
            at = @At("TAIL"),
            require = 1,
            remap = false
    )
    private void fluidunifyapi$registerTank(Fluids.InternalFluidTank tank,
                                            CallbackInfoReturnable<Fluids.InternalFluidTank> cir) {
        IuTankOwnerRegistry.register(((Fluids) (Object) this).getParent().getClass(), tank);
    }
}
