package com.sxcccccccc.mekmmuufix.mixin;

import com.sxcccccccc.mekmmuufix.compat.UuCompat;
import com.jerry.mekmm.common.tile.machine.TileEntityReplicator;
import mekanism.api.recipes.ItemStackGasToItemStackRecipe;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 物品复制机（TileEntityReplicator）配方查询点：
 * <ul>
 *   <li>静态 {@code isValidItemInput(ItemStack)} —— 物品槽校验（slot validator，插入/每 tick 都会调）；</li>
 *   <li>静态 {@code getRecipe(ItemStack, GasStack)} —— 咽喉点：实例 getRecipe(int)（recipeCacheLookupMonitor
 *       每 tick）与 JEI 都汇聚到这里。原逻辑读 config 生成的 {@code customRecipeMap}。</li>
 * </ul>
 * 两处 HEAD 注入 {@code UuCompat.ensureItemMaps()}：懒填充 + 有状态重填
 * （IC2 索引 ready 状态翻转时重填全量版），把配方表换成 IC2 UU 成本体系内容。
 *
 * <p>mekmm 是 Forge mod：类名/方法名运行时不改（仅 MC 成员引用 SRG 化），
 * remap=false + 精确描述符即可；handler 体内对 MC/mekanism 的引用由 reobfJar 重映射。</p>
 */
@Mixin(value = TileEntityReplicator.class, remap = false)
public abstract class TileEntityReplicatorMixin {

    @Inject(method = "isValidItemInput(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"), remap = false)
    private static void mekmmuufix$ensureItemMapValid(CallbackInfoReturnable<Boolean> cir) {
        UuCompat.ensureItemMaps();
    }

    @Inject(method = "getRecipe(Lnet/minecraft/world/item/ItemStack;Lmekanism/api/chemical/gas/GasStack;)"
            + "Lmekanism/api/recipes/ItemStackGasToItemStackRecipe;",
            at = @At("HEAD"), remap = false)
    private static void mekmmuufix$ensureItemMapRecipe(CallbackInfoReturnable<ItemStackGasToItemStackRecipe> cir) {
        UuCompat.ensureItemMaps();
    }
}
