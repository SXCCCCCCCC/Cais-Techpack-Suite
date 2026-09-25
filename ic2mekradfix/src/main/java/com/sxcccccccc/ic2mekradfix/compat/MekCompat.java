package com.sxcccccccc.ic2mekradfix.compat;

import mekanism.api.radiation.capability.IRadiationShielding;
import mekanism.common.capabilities.Capabilities;
import mekanism.common.util.EnumUtils;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Mek 侧判定工具：穿着是否达到 Mek 定义的「全辐射防护」（屏蔽值之和 ≥ 1.0）。
 *
 * <p>逻辑复刻 {@code RadiationManager.getRadiationResistance} 的护甲槽部分
 * （本包无 Curios，无需饰品槽；屏蔽值本身由 Mek 的 capability 提供——MekaSuit
 * 的屏蔽 handler 自带 {@code isModuleEnabled(RADIATION_SHIELDING_UNIT)} 检查，
 * 未装模块的部件返回 0）。</p>
 *
 * <p>注：本类不是 mixin，只是 handler 复用的纯 Java 辅助类；reobfJar 会像处理
 * mixin 类一样把它对 MC 的引用重映射为运行时 SRG 名。</p>
 */
public final class MekCompat {

    /** Mek 全套 hazmat / 带屏蔽单元全套 MekaSuit 的屏蔽值和恰为 1.0，以此作为全防护阈值。 */
    public static final double FULL_SHIELDING = 1.0;

    private MekCompat() {
    }

    /**
     * 4 个护甲槽上 {@code RADIATION_SHIELDING} capability 求和是否 ≥ 1.0。
     * 槽位数组直接用 Mek 的 {@code EnumUtils.ARMOR_SLOTS}，与 Mek 自身逻辑保持同步。
     */
    public static boolean hasFullShielding(LivingEntity entity) {
        double sum = 0.0;
        for (EquipmentSlot slot : EnumUtils.ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            Optional<IRadiationShielding> shielding =
                    stack.getCapability(Capabilities.RADIATION_SHIELDING, null).resolve();
            if (shielding.isPresent()) {
                sum += shielding.get().getRadiationShielding();
                if (sum >= FULL_SHIELDING) {
                    return true;
                }
            }
        }
        return false;
    }
}
