package com.sxcccccccc.mekmmuufix.mixin;

import com.sxcccccccc.mekmmuufix.compat.UuCompat;
import com.jerry.mekmm.api.recipes.FluidStackGasToFluidStackRecipe;
import com.jerry.mekmm.common.tile.machine.TileEntityFluidReplicator;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 流体复制机（TileEntityFluidReplicator）配方查询点：
 * 静态 {@code isValidFluidInput(FluidStack)}（流体槽校验）与静态
 * {@code getRecipe(FluidStack, GasStack)}（recipeCacheLookupMonitor 咽喉点）。
 * 两处 HEAD 注入 {@code UuCompat.ensureFluidMap()}，把配方表换成 IC2 UU 成本体系内容
 * （流体以其桶物品的 IC2 成本为代理，无桶流体不可复制）。
 */
@Mixin(value = TileEntityFluidReplicator.class, remap = false)
public abstract class TileEntityFluidReplicatorMixin {

    @Inject(method = "isValidFluidInput(Lnet/minecraftforge/fluids/FluidStack;)Z",
            at = @At("HEAD"), remap = false)
    private static void mekmmuufix$ensureFluidMapValid(CallbackInfoReturnable<Boolean> cir) {
        UuCompat.ensureFluidMap();
    }

    @Inject(method = "getRecipe(Lnet/minecraftforge/fluids/FluidStack;Lmekanism/api/chemical/gas/GasStack;)"
            + "Lcom/jerry/mekmm/api/recipes/FluidStackGasToFluidStackRecipe;",
            at = @At("HEAD"), remap = false)
    private static void mekmmuufix$ensureFluidMapRecipe(CallbackInfoReturnable<FluidStackGasToFluidStackRecipe> cir) {
        UuCompat.ensureFluidMap();
    }
}
