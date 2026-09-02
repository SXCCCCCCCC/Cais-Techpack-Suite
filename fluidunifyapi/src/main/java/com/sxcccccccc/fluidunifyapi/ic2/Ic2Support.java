package com.sxcccccccc.fluidunifyapi.ic2;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IC2 目标侧支撑工具（Connector 环境下 ic2 jar 是 intermediary 引用，mixin 用
 * targets 字符串 + @Coerce + 反射——fluidunifyfix 旧 M2/M5/M8/M11 实证手法）。
 *
 * <p>关键认知：流体实例本身在运行时是同一注册表对象——ForgeRegistries 解析到的
 * "ic2_120:steam" 与 ic2 jar 里 ModFluids.STEAM_STILL 是同一个 Fluid 实例，
 * 身份比较直接可用，无需反射拿流体。需要反射的只有两处：从 intermediary
 * {@code FluidVariant} 取流体（getFluid），从 ic2 桶物品反查流体
 * （ModFluids.INSTANCE.getFluidFromModBucket，参数类型是 intermediary Item，
 * 反射规避编译期类型差）。</p>
 */
public final class Ic2Support {

    private static final Map<String, Method> VARIANT_GET_FLUID = new ConcurrentHashMap<>();
    private static volatile Method modFluidsBucketFluid;
    private static volatile Object modFluidsInstance;

    private Ic2Support() {
    }

    // ==================== 流体实例（运行时同一注册表对象） ====================

    public static Fluid water() {
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation("minecraft", "water"));
    }

    public static Fluid flowingWater() {
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation("minecraft", "flowing_water"));
    }

    public static Fluid lava() {
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation("minecraft", "lava"));
    }

    public static Fluid ic2Fluid(String name) {
        return ForgeRegistries.FLUIDS.getValue(new ResourceLocation("ic2_120", name));
    }

    public static Fluid hotCoolant() {
        return ic2Fluid("hot_coolant");
    }

    public static Fluid coolant() {
        return ic2Fluid("coolant");
    }

    public static Fluid pahoehoe() {
        return ic2Fluid("pahoehoe_lava");
    }

    public static Fluid biomass() {
        return ic2Fluid("biomass");
    }

    public static Fluid steam() {
        return ic2Fluid("steam");
    }

    public static Fluid superheatedSteam() {
        return ic2Fluid("superheated_steam");
    }

    /** flowing_xxx → xxx 归一化（配置里只写 still id；机器原生两种都认）。 */
    public static Fluid normalize(Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        var loc = ForgeRegistries.FLUIDS.getKey(fluid);
        if (loc != null && loc.getNamespace().equals("ic2_120") && loc.getPath().startsWith("flowing_")) {
            return ic2Fluid(loc.getPath().substring("flowing_".length()));
        }
        return fluid;
    }

    // ==================== FluidVariant → Fluid（intermediary 反射，按类缓存） ====================

    /** 从 @Coerce 的 FluidVariant 对象取流体；失败返回 null（调用方回落原生逻辑）。 */
    public static Fluid fluidOf(Object variant) {
        if (variant == null) {
            return null;
        }
        try {
            Method m = VARIANT_GET_FLUID.computeIfAbsent(variant.getClass().getName(), name -> {
                try {
                    return variant.getClass().getMethod("getFluid");
                } catch (NoSuchMethodException e) {
                    throw new IllegalStateException("[fluidunifyapi] FluidVariant.getFluid not found on " + name, e);
                }
            });
            return (Fluid) m.invoke(variant);
        } catch (Throwable t) {
            return null;
        }
    }

    // ==================== ItemStack（桶/单元）→ Fluid ====================

    /**
     * 物品容器反查流体：ic2 桶走 ModFluids.INSTANCE.getFluidFromModBucket 反射；
     * 原版水/岩浆桶直接映射；流体单元走 CellsAndBucketsKt.getFluidCellVariant 反射。
     * 失败返回 null。
     */
    public static Fluid fluidOfItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() == Items.WATER_BUCKET) {
            return water();
        }
        if (stack.getItem() == Items.LAVA_BUCKET) {
            return lava();
        }
        try {
            Method m = modFluidsBucketFluid;
            if (m == null) {
                Class<?> cls = Class.forName("ic2_120.content.fluid.ModFluids");
                Object inst = cls.getField("INSTANCE").get(null);
                m = cls.getMethod("getFluidFromModBucket", Class.forName("net.minecraft.world.item.Item"));
                modFluidsInstance = inst;
                modFluidsBucketFluid = m;
            }
            Fluid f = (Fluid) m.invoke(modFluidsInstance, stack.getItem());
            if (f != null) {
                return f;
            }
        } catch (Throwable ignored) {
            // 落入流体单元反射路径
        }
        return fluidOfCellVariant(stack);
    }

    /** 流体单元（FluidCell）路径：CellsAndBucketsKt.getFluidCellVariant 静态扩展。 */
    private static Fluid fluidOfCellVariant(ItemStack stack) {
        try {
            Class<?> kt = Class.forName("ic2_120.content.item.CellsAndBucketsKt");
            Method m = kt.getMethod("getFluidCellVariant", Class.forName("net.minecraft.world.item.ItemStack"));
            Object variant = m.invoke(null, stack);
            return fluidOf(variant);
        } catch (Throwable t) {
            return null;
        }
    }

    // ==================== 流体加热机第三方桶消耗（自 fluidunifyfix M2 迁移） ====================

    /**
     * 反射调机器自身例程完成"真插 + 空容器入槽"（SOP 真事务手法：直接调 fabric
     * Storage.insert 传 null 事务会静默返 0）。任一环节不满足/异常返回 false。
     */
    public static boolean fireboxConsumeContainer(Object owner, Fluid fluid, ItemStack emptyContainer) {
        try {
            Class<?> cls = owner.getClass();
            Method canInsert = cls.getDeclaredMethod("canInsertEmptyContainer", ItemStack.class);
            Method tryInsert = cls.getDeclaredMethod("tryInsertEmptyContainer", ItemStack.class);
            canInsert.setAccessible(true);
            tryInsert.setAccessible(true);
            Field f = cls.getDeclaredField("fuelTankInternal");
            f.setAccessible(true);
            Object tank = f.get(owner);
            Method insertFuel = tank.getClass().getDeclaredMethod("tryInsertFuel", Fluid.class, long.class);
            insertFuel.setAccessible(true);
            if (!(canInsert.invoke(owner, emptyContainer) instanceof Boolean b) || !b) {
                return false;
            }
            long inserted = insertFuel.invoke(tank, fluid, 1000L) instanceof Long l ? l : 0L;
            if (inserted < 1000L) {
                return false;
            }
            return tryInsert.invoke(owner, emptyContainer) instanceof Boolean ok && ok;
        } catch (Throwable t) {
            return false;
        }
    }

    // ==================== 槽位常量（反射一次缓存） ====================

    private static volatile int geoFuelSlot = -1;

    /** GeoGeneratorBlockEntity.FUEL_SLOT；取不到返回 -2（调用方守卫恒不命中，安全失效）。 */
    public static int geoFuelSlotIndex() {
        int v = geoFuelSlot;
        if (v < 0) {
            try {
                Class<?> cls = Class.forName("ic2_120.content.block.machines.GeoGeneratorBlockEntity");
                v = cls.getField("FUEL_SLOT").getInt(null);
            } catch (Throwable t) {
                v = -2;
            }
            geoFuelSlot = v;
        }
        return v;
    }

    // ==================== ioStorage 内联判定绕过（身份保持直插） ====================
    // 多台 IC2 机器的 ioStorage.insert 有内联硬判定（非原生流体直接 return 0L），
    // 门禁 mixin 的"放行落原生体"会撞上内联判定——ACCEPT 时改为直接调用所属机器的
    // 目标罐 insert（罐的 canInsert 已由对应 TankGate 加宽），variant 原样存入
    // （身份保持）。手法与 fluidqinshihuangdi 的 Ic2RuleLookup.directInsert 一致。

    private static final Map<String, java.lang.reflect.Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Method> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * ioStorage 匿名对象 → 所属机器 BE 的目标罐字段做身份保持插入。
     *
     * @param ioInner       ioStorage 匿名对象（synthetic this$0 指回 BE）
     * @param tankFieldName 目标罐字段名（如 "waterTank"、"inputTankInternal"）
     * @return 插入结果（droplets）；反射失败返回 -1（调用方回退原生路径）
     */
    public static long directInsert(Object ioInner, String tankFieldName, Object variant,
                                    long maxAmount, Object tx) {
        try {
            Object owner = ownerOf(ioInner);
            if (owner == null) {
                return -1L;
            }
            Object tank = fieldValue(owner, tankFieldName);
            if (tank == null) {
                return -1L;
            }
            Method insert = findMethod(tank.getClass(), "insert", 3);
            if (insert == null) {
                return -1L;
            }
            Object ret = insert.invoke(tank, variant, maxAmount, tx);
            return ret instanceof Long l ? l : -1L;
        } catch (ReflectiveOperationException e) {
            return -1L;
        }
    }

    /** 匿名内部类 → 外层 BE 实例（this$0 链，找不到返回 null）。 */
    public static Object ownerOf(Object inner) {
        if (inner == null) {
            return null;
        }
        for (Class<?> c = inner.getClass(); c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField("this$0");
                f.setAccessible(true);
                return f.get(inner);
            } catch (NoSuchFieldException e) {
                // 继续向父类找
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private static Object fieldValue(Object target, String name) {
        if (target == null) {
            return null;
        }
        try {
            java.lang.reflect.Field f = field(target.getClass(), name);
            if (f == null) {
                return null;
            }
            f.setAccessible(true);
            return f.get(target);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static java.lang.reflect.Field field(Class<?> start, String name) {
        String key = start.getName() + '#' + name;
        java.lang.reflect.Field cached = FIELD_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        for (Class<?> c = start; c != null; c = c.getSuperclass()) {
            try {
                java.lang.reflect.Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                FIELD_CACHE.put(key, f);
                return f;
            } catch (NoSuchFieldException ignored) {
                // 向父类找
            }
        }
        return null;
    }

    private static Method findMethod(Class<?> start, String name, int paramCount) {
        String key = start.getName() + '#' + name + '#' + paramCount;
        Method cached = METHOD_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        for (Class<?> c = start; c != null; c = c.getSuperclass()) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == paramCount) {
                    m.setAccessible(true);
                    METHOD_CACHE.put(key, m);
                    return m;
                }
            }
        }
        return null;
    }
}
