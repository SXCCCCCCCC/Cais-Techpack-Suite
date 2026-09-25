package com.sxcccccccc.mekmmuufix.mixin;

import com.sxcccccccc.mekmmuufix.compat.UuCompat;
import com.jerry.mekmm.common.tile.factory.TileEntityReplicatingFactory;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 复制工厂（TileEntityReplicatingFactory，basic/advanced/elite/ultimate 四阶共享此类）：
 * <ul>
 *   <li>实例 {@code isValidInputItem(ItemStack)} —— 工厂输入槽校验；</li>
 *   <li>静态 {@code getRecipe(ItemStack, GasStack)} —— 工厂自己的配方咽喉点
 *       （与单体复制机的实现是两份拷贝，各自读自己的 {@code customRecipeMap}）。</li>
 * </ul>
 * 两处 HEAD 注入 {@code UuCompat.ensureItemMaps()}（工厂与单体复制机共用同一份
 * IC2 成本内容，填两个静态表）。
 */
@Mixin(value = TileEntityReplicatingFactory.class, remap = false)
public abstract class TileEntityReplicatingFactoryMixin {

    @Inject(method = "isValidInputItem(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), remap = false)
    private void mekmmuufix$ensureItemMapValid(CallbackInfoReturnable<Boolean> cir) {
        UuCompat.ensureItemMaps();
    }

    @Inject(method = "getRecipe(Lnet/minecraft/world/item/ItemStack;Lmekanism/api/chemical/gas/GasStack;)"
            + "Lmekanism/api/recipes/ItemStackGasToItemStackRecipe;",
            at = @At("HEAD"), remap = false)
    private static void mekmmuufix$ensureItemMapRecipe(CallbackInfoReturnable<ItemStackGasToItemStackRecipe> cir) {
        UuCompat.ensureItemMaps();
    }
}
