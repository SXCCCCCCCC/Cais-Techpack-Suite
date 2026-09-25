package com.sxcccccccc.ic2mekradfix.compat;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * IC2 侧判定工具：穿着是否达到 IC2 定义的「辐射免疫」。
 *
 * <p>主路径反射调用 {@code ic2_120.content.effect.RadiationHandler.INSTANCE.isImmune(LivingEntity)}
 * （量子甲或全套 hazmat——与 IC2 自身口径一致）。Connector 运行时只重映射 IC2 对 MC 类的
 * 引用，IC2 自身的类名/方法名保持不变；运行时 isImmune 的形参即真实的 LivingEntity 类，
 * 与 Forge 侧 {@code LivingEntity.class} 为同一对象，反射签名匹配。</p>
 *
 * <p>回退路径：反射失败（ic2 版本变化等）时复刻 IC2 的 hazmat 谓词——4 个护甲槽每件的
 * 物品 id path 含 "hazmat" 或等于 "rubber_boots"（javap 实证，
 * {@code RadiationHandler.hasFullHazmat} / {@code hasFullHazmat$isHazmat}）。
 * 回退路径不覆盖量子甲（保持简单，仅兜底核心需求）。</p>
 */
public final class Ic2Compat {

    /** IC2 判定使用的 4 个护甲槽（javap + Connector mappings.tsrg 实证：HEAD/CHEST/LEGS/FEET）。 */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private Ic2Compat() {
    }

    /**
     * IC2 是否会认为该实体辐射免疫。反射失败时回退到复刻的 hazmat 谓词。
     */
    public static boolean isIc2RadiationImmune(LivingEntity entity) {
        Boolean viaReflection = isImmuneViaReflection(entity);
        if (viaReflection != null) {
            return viaReflection;
        }
        return hasFullIc2Hazmat(entity);
    }

    /** IC2 辐射免疫单例 {@code RadiationHandler.INSTANCE}。解析失败时为 null（调用方回退）。 */
    private static final Object IC2_INSTANCE;
    /** 反射句柄 {@code RadiationHandler.isImmune}。null = 解析失败，永久走 hazmat 回退。 */
    private static final Method IC2_IS_IMMUNE;

    static {
        Object inst = null;
        Method m = null;
        try {
            Class<?> cls = Class.forName("ic2_120.content.effect.RadiationHandler");
            inst = cls.getField("INSTANCE").get(null);
            m = cls.getDeclaredMethod("isImmune", LivingEntity.class);
            m.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            // 解析失败：保持原语义——isImmuneViaReflection 恒返回 null，调用方走 hasFullIc2Hazmat
        }
        IC2_INSTANCE = inst;
        IC2_IS_IMMUNE = m;
    }

    /**
     * 反射调用 IC2 {@code RadiationHandler.isImmune}。解析失败返回 null（调用方回退）。
     *
     * <p>类/单例/方法在静态初始化时解析一次并缓存——IC2 类名与成员运行期不会变化，
     * 每 tick 重复 Class.forName / getDeclaredMethod / setAccessible 只会反复付
     * 元数据查找与安全校验的成本（spark profile 热点所在）。热路径仅剩 invoke。</p>
     */
    private static Boolean isImmuneViaReflection(LivingEntity entity) {
        if (IC2_IS_IMMUNE == null) {
            return null;
        }
        try {
            return (Boolean) IC2_IS_IMMUNE.invoke(IC2_INSTANCE, entity);
        } catch (IllegalAccessException | InvocationTargetException | ClassCastException e) {
            return null;
        }
    }

    /**
     * 复刻 IC2 hazmat 全套谓词：4 个护甲槽每件均为 ArmorItem，且物品 id path
     * 含 "hazmat" 或等于 "rubber_boots"。
     */
    private static boolean hasFullIc2Hazmat(LivingEntity entity) {
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = entity.getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
                return false;
            }
            String path = ForgeRegistries.ITEMS.getKey(stack.getItem()).getPath();
            if (!path.contains("hazmat") && !path.equals("rubber_boots")) {
                return false;
            }
        }
        return true;
    }
}
