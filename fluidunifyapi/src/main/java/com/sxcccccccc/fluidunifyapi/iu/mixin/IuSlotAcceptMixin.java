package com.sxcccccccc.fluidunifyapi.iu.mixin;

import com.denfop.inventory.InventoryFluidByList;
import com.sxcccccccc.fluidunifyapi.core.MachineAdapters;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * IU 咽喉点 3：GUI 物品槽（桶/流体单元入口）接受判定——与罐门禁共用同一张补丁表，
 * 桶入槽与罐入流天然同步（SOP"双通道门禁"教训：两条通道必须一起打通）。
 *
 * <p>探针口径：槽的原生接受集合 {@link #acceptedFluids} 里哪个流体命中补丁模板，
 * 该槽就是该补丁的目标口。</p>
 */
@Mixin(value = InventoryFluidByList.class, remap = false)
public abstract class IuSlotAcceptMixin {

    @Shadow(remap = false)
    private Set<Fluid> acceptedFluids;

    @Inject(
            method = "acceptsLiquid(Lnet/minecraft/world/level/material/Fluid;)Z",
            at = @At("RETURN"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void fluidunifyapi$acceptSlot(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        boolean nativeResult = cir.getReturnValueZ();
        String machineId = MachineAdapters.machineIdOf(
                ((InventoryFluidByList) (Object) this).base.getClass());
        if (machineId == null || !UnifiedFluidRegistry.hasPatch(machineId)) {
            return;
        }
        Set<Fluid> nativeSet = this.acceptedFluids;
        boolean widened = UnifiedFluidRegistry.acceptOverrideProbed(
                machineId, fluid, nativeSet == null ? f -> false : nativeSet::contains);
        if (widened != nativeResult) {
            cir.setReturnValue(widened);
        }
    }
}
