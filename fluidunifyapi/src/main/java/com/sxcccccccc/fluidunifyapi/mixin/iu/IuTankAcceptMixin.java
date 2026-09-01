package com.sxcccccccc.fluidunifyapi.mixin.iu;

import com.denfop.componets.Fluids;
import com.sxcccccccc.fluidunifyapi.core.MachineAdapters;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.iu.IuTankOwnerRegistry;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * IU 咽喉点 2：罐接受判定（全部输入罐的汇聚点，iuunify 字节码实证过"isFluidValid、
 * FluidTank.fill 都走 acceptsFluid"）。
 *
 * <p>探针咨询：用罐的原生谓词 {@link #acceptedFluids} 反推"哪个补丁的模板流体被这
 * 个罐原生接受"→ 命中即对该补丁口径 decide。只改输入定义（接受集合），不碰
 * FluidTank.fill 的混液语义——防混装（非空罐 isFluidEqual）完全原生。</p>
 *
 * <p>新机器语义：BE 构造晚于补丁应用，本 mixin 对配方族机器自然退化为无补丁路径
 * （machineIdOf 查不到即直接放行原生判定）。</p>
 */
@Mixin(value = Fluids.InternalFluidTank.class, remap = false)
public abstract class IuTankAcceptMixin {

    @Shadow(remap = false)
    private Predicate<Fluid> acceptedFluids;

    @Inject(
            method = "acceptsFluid(Lnet/minecraft/world/level/material/Fluid;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void fluidunifyapi$accept(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        Class<?> owner = IuTankOwnerRegistry.ownerOf(this);
        String machineId = MachineAdapters.machineIdOf(owner);
        if (machineId == null || !UnifiedFluidRegistry.hasPatch(machineId)) {
            return; // 原生路径
        }
        Predicate<Fluid> nativePredicate = this.acceptedFluids;
        // 必须 cancel：SOP 铁律——HEAD 注入只 setReturnValue 不 cancel 则返回值被忽略
        cir.setReturnValue(UnifiedFluidRegistry.acceptOverrideProbed(machineId, fluid, nativePredicate));
        cir.cancel();
    }
}
