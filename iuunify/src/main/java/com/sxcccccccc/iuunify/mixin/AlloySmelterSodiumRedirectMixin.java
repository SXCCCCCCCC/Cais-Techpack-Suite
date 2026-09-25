package com.sxcccccccc.iuunify.mixin;

import com.denfop.blockentity.mechanism.dual.heat.BlockEntityAlloySmelter;
import com.denfop.recipe.IInputHandler;
import com.denfop.recipe.IInputItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 合金炉钠线换源（OEI 统一适配，2026-09-09 用户怀疑、排查实证）。
 *
 * <p>问题：init() 的钠配方（钠粉 + 铅粉 → 铅钠合金粉，温度 1000）硬编码
 * {@code input.getInput(new ItemStack(IUItem.iudust.getItemFromMeta(64), 1))}
 * （iudust[64]=sodium），OEI 统一后（IU 钠粉 → gtceu:sodium_dust）GT 钠粉不被接受。
 *
 * <p>修法（照抄 SolidFluidMixerSodiumRedirectMixin / 成熟先例
 * fluidunifyapi:IuRefrigeratorCoolantMixin 形态）：@Redirect init() 里
 * {@code IInputHandler.getInput(ItemStack)} 单参调用，钠 = ordinal 2（init() 行序
 * 实证：alloysingot13 0 / iuingot3 1 / 钠粉 2 / 铅粉 3 / …；
 * (ItemStack,int)/(String,…) 重载不匹配此描述符）。handler 返回 GT 钠粉的
 * getInput 替身；GT 钠取不到时降级原样回调
 * {@code Recipes.inputFactory.getInput(original)}（ordinal 单点、无过滤）。
 *
 * <p>mixin 铁律：handler 非 static（修饰符匹配注入点方法 init 的实例性，this
 * 不占参数位）；remap=false（IU 自有名）；handler 内对 {@code Recipes.inputFactory}
 * 的访问用限定名（mixin 类编译期不继承目标类）。
 */
@Mixin(value = BlockEntityAlloySmelter.class, remap = false)
public abstract class AlloySmelterSodiumRedirectMixin {

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/denfop/recipe/IInputHandler;getInput(Lnet/minecraft/world/item/ItemStack;)"
                            + "Lcom/denfop/recipe/IInputItemStack;",
                    ordinal = 2
            ),
            require = 1,
            remap = false
    )
    private IInputItemStack iuunify$sodiumToGt(IInputHandler input, ItemStack original) {
        Item gtSodium = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "sodium_dust"));
        if (gtSodium == null) {
            return input.getInput(original);
        }
        return input.getInput(new ItemStack(gtSodium, original.getCount()));
    }
}
