package com.sxcccccccc.iuunify.mixin;

import com.denfop.blockentity.mechanism.BlockEntitySolidFluidMixer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 固液反应釜钠线换源（OEI 统一适配，2026-09-09 用户报告）。
 *
 * <p>问题：OEI 把 IU 钠粉统一为 GT 钠粉（kubejs/data/oei/replacements/
 * sodium_dust.json：industrialupgrade:itemdust/sodium_dust → gtceu:sodium_dust），
 * 但 {@code BlockEntitySolidFluidMixer.init()} 钠配方硬编码
 * {@code new ItemStack(IUItem.iudust.getStack(64))}（iudust[64]=sodium，
 * ItemDust.ItemDustTypes 实证），IU 裸比对不认 GT 钠粉 → 配方不工作。
 *
 * <p>修法（用户裁决：直接硬编码 GT sodium，只修这一个配方）：@Redirect
 * init() 里钠配方那处 {@code addRecipe(ItemStack,…)} 调用（ItemStack 版共 6 处，
 * 钠 = ordinal 4，init() 行序实证：铜锭 0 / 氯化铁 1 / 硫小粉 2 / 硫粉 3 / 钠 4 /
 * crafting_elements 5），把输入栈换成 gtceu:sodium_dust 后回调 addRecipe。
 *
 * <p>写法照抄成熟先例 fluidunifyapi:IuRefrigeratorCoolantMixin（IU 目标类、
 * 注入实例方法）：handler 非 static（修饰符必须与注入点方法 init 的实例性一致，
 * this 不占参数位——0.5.5 初版写成 static 被 Mixin 拒绝，实崩复盘：
 * InvalidInjectionException "static modifier of handler method does not match
 * target"）、ordinal 单点定位、require=1、remap=false；mixin 类调目标类静态方法
 * 用限定名 {@code BlockEntitySolidFluidMixer.addRecipe(...)}（iudebug
 * MolecularInitOverwriteMixin 同款形态）。
 */
@Mixin(value = BlockEntitySolidFluidMixer.class, remap = false)
public abstract class SolidFluidMixerSodiumRedirectMixin {

    @Redirect(
            method = "init",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/denfop/blockentity/mechanism/BlockEntitySolidFluidMixer;"
                            + "addRecipe(Lnet/minecraft/world/item/ItemStack;"
                            + "Lnet/minecraftforge/fluids/FluidStack;"
                            + "Lnet/minecraftforge/fluids/FluidStack;"
                            + "Lnet/minecraftforge/fluids/FluidStack;)V",
                    ordinal = 4
            ),
            require = 1,
            remap = false
    )
    private void iuunify$sodiumToGt(ItemStack container, FluidStack in, FluidStack out1, FluidStack out2) {
        Item gtSodium = BuiltInRegistries.ITEM.get(new ResourceLocation("gtceu", "sodium_dust"));
        if (gtSodium == null) {
            BlockEntitySolidFluidMixer.addRecipe(container, in, out1, out2);
            return;
        }
        BlockEntitySolidFluidMixer.addRecipe(new ItemStack(gtSodium, container.getCount()), in, out1, out2);
    }
}
