package com.sxcccccccc.iuchancefix.util;

import com.denfop.api.recipe.BaseMachineRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/**
 * 判定"矿物分离器（handler_ho）煤粉做合金"配方：
 * 输入=含杂煤粉 (industrialupgrade:crafting_elements/crafting_498_element)，
 * 输出=合金用煤粉 (industrialupgrade:crafting_elements/crafting_499_element)，chance=75。
 * 以"输出含 crafting_499_element"为标记（唯一性已核验：3.4.0.10 jar
 * BlockEntityHandlerHeavyOre.init 中该配方是全 handlerho 列表唯一 499 输出）。
 */
public final class ChanceFixUtil {

    public static final String COAL_DUST_FOR_ALLOYS_ID = "industrialupgrade:crafting_elements/crafting_499_element";

    private ChanceFixUtil() {
    }

    public static boolean isCoalAlloyRecipe(BaseMachineRecipe recipe) {
        if (recipe == null || recipe.output == null || recipe.output.items == null) {
            return false;
        }
        for (ItemStack stack : recipe.output.items) {
            if (stack != null && !stack.isEmpty()
                    && COAL_DUST_FOR_ALLOYS_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
                return true;
            }
        }
        return false;
    }

}
