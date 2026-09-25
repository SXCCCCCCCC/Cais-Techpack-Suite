package com.sxcccccccc.macobaltfix.mixin;

import com.blakebr0.mysticalagriculture.api.crop.Crop;
import com.blakebr0.mysticalagriculture.api.lib.LazyIngredient;
import com.blakebr0.mysticalagradditions.init.ModCrops;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 对 MysticalAgradditions {@link ModCrops#withRequiredMods(Crop, String...)} 入口
 * 短路：本包无 Avaritia，其 NEUTRONIUM 作物被 {@code setEnabled(false)} 禁用。
 * 与 MA 主包 {@link ModCropsMixin} 同一手法——照 Agradditions 源码实际结构
 * （init/ModCrops.java 第 59-64 行：与 MA 主包完全相同的私有 withRequiredMods 实现），
 * 单独 target 一个 mixin 类。
 *
 * <p>材料：原 {@code mysticalagradditions:neutronium_ingot} tag 内容为
 * {@code avaritia:neutronium_ingot}（required:false，缺失即空 tag），换为
 * gtceu:neutronium_ingot 直接指物。注意 seed/crafting/neutronium.json 里写死的
 * tag_empty 检查（mysticalagradditions:neutronium_ingot）仍会拦截配方，
 * 由 KubeJS 向该 tag 添加 gtceu:neutronium_ingot 互补解决。
 */
@Mixin(value = ModCrops.class, remap = false)
public abstract class AgradditionsModCropsMixin {

    @Inject(method = "withRequiredMods", at = @At("HEAD"), cancellable = true)
    private static void macobaltfix$keepNeutroniumEnabled(Crop crop, String[] mods, CallbackInfoReturnable<Crop> cir) {
        if (crop == ModCrops.NEUTRONIUM) {
            crop.setCraftingMaterial(LazyIngredient.item("gtceu:neutronium_ingot"));
            cir.setReturnValue(crop);
        }
    }
}
