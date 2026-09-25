package com.sxcccccccc.iuchancefix.mixin;

import com.denfop.api.recipe.MachineRecipe;
import com.denfop.blockentity.base.BlockEntityBaseHandlerHeavyOre;
import com.sxcccccccc.iuchancefix.util.ChanceFixUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 电动系矿物分离器{DEFAULT/IMPROVED/ADVANCED/PERFECT/PHOTONIC 六款共用
 * handlerho 配方表}。原实现 setRecipeOutput：
 * col[i] = (int)(metadata "input"+i * coef); col[i] = Math.min(col[i], 95)
 * —— 即使配方元数据改成 100，默认款落在 min(100*1,95)=95，提高款 coef 1.1/1.2/1.3
 * 更甚。此注入在 TAIL 对"煤粉做合金"配方把 col 全槽强置 100（100% 确定性），
 * 其余配方 col 计算完全不动（"其余一切不变"）。
 */
@Mixin(value = BlockEntityBaseHandlerHeavyOre.class, remap = false)
public abstract class HandlerHoChanceMixin {

    @Shadow(remap = false)
    private int[] col;

    @Inject(method = "setRecipeOutput", at = @At("TAIL"), remap = false)
    private void iuchancefix$forceCoalAlloyChance100(MachineRecipe output, CallbackInfo ci) {
        if (output == null || col == null) {
            return;
        }
        if (ChanceFixUtil.isCoalAlloyRecipe(output.getRecipe())) {
            for (int i = 0; i < col.length; i++) {
                col[i] = 100;
            }
        }
    }

}
