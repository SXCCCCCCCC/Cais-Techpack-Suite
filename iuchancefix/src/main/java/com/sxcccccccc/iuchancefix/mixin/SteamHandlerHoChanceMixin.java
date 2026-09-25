package com.sxcccccccc.iuchancefix.mixin;

import com.denfop.api.recipe.MachineRecipe;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamHandlerHeavyOre;
import com.sxcccccccc.iuchancefix.util.ChanceFixUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 蒸汽分离器（steam_handler_ore）与电动系共用 handlerho 配方表，但原实现
 * col[i] = max(0, min(col[i], 95) - 5)（钳 95 再减 5）——元数据改成 100 只会
 * 得到 90%。此注入 TAIL 对"煤粉做合金"配方把 col 全槽强置 100，余不动。
 * 注意该类的 operateOnce 有两条路径（ComponentSteamProcess 匿名类 + 本类公有
 * 重载），两者都读 this.col，经 setRecipeOutput 一次注入全部覆盖。
 */
@Mixin(value = BlockEntitySteamHandlerHeavyOre.class, remap = false)
public abstract class SteamHandlerHoChanceMixin {

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
