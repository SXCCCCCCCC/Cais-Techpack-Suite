package com.sxcccccccc.macobaltfix.mixin;

import com.blakebr0.mysticalagriculture.api.crop.Crop;
import com.blakebr0.mysticalagriculture.api.lib.LazyIngredient;
import com.blakebr0.mysticalagriculture.lib.ModCrops;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 对 {@link ModCrops#withRequiredMods(Crop, String...)} 入口短路：本包缺失对应
 * 依赖 mod 的作物直接返回原 crop（不执行 {@code setEnabled(anyMatch(isModLoaded))}
 * 的禁用逻辑），保持构造默认 {@code enabled=true}。
 *
 * <p>被禁依赖不在包的 6 个 MA 主包作物：COBALT / BASALT / MARBLE / ROSE_GOLD /
 * URANINITE / REDSTONE_ALLOY。其中 URANINITE 与 REDSTONE_ALLOY
 * 的原材料是缺失 mod 的物品（powah:uraninite / enderio:redstone_alloy_ingot），
 * 同时把 crafting material 换成包内存在的 GTCEu 材料（LazyIngredient 为惰性解析，
 * 真正 getIngredient 发生在配方加载时，届时 GTCEu 物品已注册，无时序风险）。
 *
 * <p>材料按 tag 优先（钴先例：tag 由 GT 提供即可，不留空 tag 检查隐患）：
 * URANINITE→forge:dusts/uraninite（=gtceu:uraninite_dust）；REDSTONE_ALLOY 材料
 * 无对应 forge tag，用 gtceu:red_alloy_ingot 直接指物。
 *
 * <p>其余 mod 门作物（thermal/redstone_arsenal/immersiveengineering/enderio 等）
 * 走原逻辑，行为不变。
 *
 * <p>remap=false：目标为 Forge mod 类（不在原版映射集）；handler 只引用 MA 自有类，
 * 无原版成员引用，reobfJar 对 MA 类名无映射、原样通过。
 */
@Mixin(value = ModCrops.class, remap = false)
public abstract class ModCropsMixin {

    @Inject(method = "withRequiredMods", at = @At("HEAD"), cancellable = true)
    private static void macobaltfix$keepCropsEnabled(Crop crop, String[] mods, CallbackInfoReturnable<Crop> cir) {
        // 材料替换：URANINITE / REDSTONE_ALLOY（惰性 LazyIngredient，注册期只存字符串）
        if (crop == ModCrops.URANINITE) {
            crop.setCraftingMaterial(LazyIngredient.tag("forge:dusts/uraninite"));
        } else if (crop == ModCrops.REDSTONE_ALLOY) {
            crop.setCraftingMaterial(LazyIngredient.item("gtceu:red_alloy_ingot"));
        }

        // 解锁：缺失依赖 mod 的作物保持 enabled=true
        if (crop == ModCrops.COBALT
                || crop == ModCrops.BASALT
                || crop == ModCrops.MARBLE
                || crop == ModCrops.ROSE_GOLD
                || crop == ModCrops.URANINITE
                || crop == ModCrops.REDSTONE_ALLOY) {
            cir.setReturnValue(crop);
        }
    }
}
