package com.sxcccccccc.fluidunifyapi.ic2;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

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
}
