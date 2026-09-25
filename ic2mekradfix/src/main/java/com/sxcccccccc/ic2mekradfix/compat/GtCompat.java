package com.sxcccccccc.ic2mekradfix.compat;

import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GT 侧判定工具：穿着是否达到 GT 定义的「全套 PPE 辐射防护」。
 *
 * <p>主路径直接调 {@code HazardProperty.ProtectionType.FULL.isProtected(LivingEntity)}——
 * GT 原生判定（javap 实证 gtceu-1.20.1-7.5.3.jar）：四护甲槽每件为
 * {@code ArmorComponentItem} 且 {@code getArmorLogic().isPPE()}，或带
 * {@code gtceu:ppe_armor} tag（jar 内 data/gtceu/tags/items/ppe_armor.json，12 项）。
 * 复用原生判定与 GT 自身口径完全一致，且自动覆盖未来新增的 GT PPE 套装。</p>
 *
 * <p>回退路径：GTCEu 加载失败（类缺失，LinkageError）时手查 {@code gtceu:ppe_armor}
 * tag 覆盖 4 个护甲槽，作最小兜底——此时 GtProtectionTypeMixin 无法应用，但 IC2/Mek
 * 两个方向的修复仍可工作。</p>
 *
 * <p>注：本类不是 mixin，只是 handler 复用的纯 Java 辅助类；对 MC 的引用由 reobfJar
 * 重映射为运行时 SRG 名，GT 自己的类名/方法名在 SRG 命名空间保持不变。</p>
 */
public final class GtCompat {

    /** GT 全套 PPE 使用的物品 tag（回退路径手查用）。 */
    private static final TagKey<Item> PPE_ARMOR =
            TagKey.create(Registries.ITEM, new ResourceLocation("gtceu", "ppe_armor"));

    /** 回退路径使用的 4 个护甲槽（与 GT FULL 的 equipmentTypes 对应）。 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private GtCompat() {
    }

    /**
     * GT 是否会认为该实体受到全套 PPE 保护（防致癌辐射）。
     * 主路径调 GT 原生判定；GTCEu 类缺失（加载失败）时回退到 tag 手查。
     */
    public static boolean isGtPpeProtected(LivingEntity entity) {
        try {
            return HazardProperty.ProtectionType.FULL.isProtected(entity);
        } catch (LinkageError e) {
            return hasFullPpeArmorTag(entity);
        }
    }

    /**
     * 回退实现：4 个护甲槽每件均在 {@code gtceu:ppe_armor} tag 中
     * （复刻 GT FULL 判定的 tag 分支；不覆盖 ArmorComponentItem 分支——非 GT 物品
     * 无该能力，tag 即足够）。
     */
    private static boolean hasFullPpeArmorTag(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.is(PPE_ARMOR)) {
                return false;
            }
        }
        return true;
    }
}
