package com.sxcccccccc.iuunify.compat;

import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;

/**
 * fabric-transfer-api 物品侧桥接的反射管道（0.3.4 任务一分支一）。
 *
 * <p>背景（反编译实证）：fabric-transfer-api 的 Forge 兼容层
 * {@code TransferApiForgeCompat} 只实现了方块侧桥接（FluidStorage.SIDED →
 * Forge FLUID_HANDLER），物品侧 {@code onAttachItemStackCapabilities} 是空体
 * → IC2 Refabricated 的桶/流体单元（fabric 物品，仅注册
 * {@code FluidStorage.ITEM} 物品 API）不暴露 Forge FLUID_HANDLER_ITEM →
 * IU 机器 UI 槽位（InventoryFluid.m_7013_ 经
 * {@code getCapability(FLUID_HANDLER_ITEM)} 取 handler）一律拒绝。
 *
 * <p>修复：在 {@code TransferApiForgeCompat.onAttachItemStackCapabilities} HEAD
 * 补桥接——{@code ContainerItemContext.withInitial(stack).find(FluidStorage.ITEM)}
 * 命中 fabric 流体存储则注册 Forge 能力提供器
 * {@link FluidStorageItemBridge}。fabric 类不在本 mod 编译 classpath，全部经
 * 本类反射访问（与 fluidunifyfix steamProbeVariant 同款手法）。
 *
 * <p>SOP 铁律：mixin 包内只许放 mixin 类——本 helper 放 compat 子包。
 */
public final class FluidBridge {

    // ---- 缓存的类 / 方法 / 字段（首次使用时经反射解析一次） ----
    private static volatile boolean initialized = false;
    private static Class<?> ctxClass;           // ContainerItemContext
    private static Class<?> storageClass;       // Storage
    private static Class<?> viewClass;          // StorageView
    private static Class<?> txClass;            // Transaction
    private static Class<?> txContextClass;     // TransactionContext
    private static Class<?> itemLookupClass;    // ItemApiLookup
    private static Class<?> fluidVariantClass;  // FluidVariant
    private static Class<?> itemVariantClass;   // ItemVariant

    private static Method ctxWithInitial;       // withInitial(ItemStack)
    private static Method ctxFind;              // find(ItemApiLookup)
    private static Method ctxGetMainSlot;       // getMainSlot()
    private static Method viewGetResource;      // getResource()
    private static Method viewGetAmount;        // getAmount()
    private static Method viewGetCapacity;      // getCapacity()
    private static Method itemVariantToStack;   // toStack(int)
    private static Method txOpenOuter;          // openOuter()
    private static Method txCommit;             // commit()
    private static Method txClose;              // close()
    private static Method storageInsert;        // insert(Object, long, TransactionContext)
    private static Method storageExtract;       // extract(Object, long, TransactionContext)
    private static Method storageSimulateInsert;// simulateInsert(Object, long, TransactionContext)
    private static Method storageIterator;      // iterator()
    private static Method storageNonEmptyViews; // nonEmptyViews()
    private static Method utilToFluidView;      // ForgeCompatUtil.toFluidStorageView(FluidStack)
    private static Method utilToFabricBucket;   // toFabricBucket(int)
    private static Method utilToForgeBucket;    // toForgeBucket(int)
    private static Method utilToForgeStackView; // toForgeFluidStack(StorageView)
    private static Method utilToForgeStackVar;  // toForgeFluidStack(FluidVariant, int)
    private static Field fluidStorageItemField; // FluidStorage.ITEM
    private static Field computingLockField;    // TransferApiForgeCompat.COMPUTING_CAPABILITY_LOCK

    private FluidBridge() {
    }

    /** 幂等初始化（首次调用时加载 fabric-transfer-api 类与方法）。 */
    public static void init() {
        if (initialized) {
            return;
        }
        synchronized (FluidBridge.class) {
            if (initialized) {
                return;
            }
            try {
                ctxClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext");
                storageClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.storage.Storage");
                viewClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.storage.StorageView");
                txClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.transaction.Transaction");
                txContextClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext");
                itemLookupClass = Class.forName("net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup");
                fluidVariantClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant");
                itemVariantClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.item.ItemVariant");
                Class<?> fluidStorageClass = Class.forName("net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage");
                Class<?> forgeCompatUtilClass = Class.forName("net.fabricmc.fabric.impl.transfer.compat.ForgeCompatUtil");
                Class<?> transferApiForgeCompatClass = Class.forName("net.fabricmc.fabric.impl.transfer.compat.TransferApiForgeCompat");

                ctxWithInitial = ctxClass.getMethod("withInitial", ItemStack.class);
                ctxFind = ctxClass.getMethod("find", itemLookupClass);
                ctxGetMainSlot = ctxClass.getMethod("getMainSlot");
                viewGetResource = viewClass.getMethod("getResource");
                viewGetAmount = viewClass.getMethod("getAmount");
                viewGetCapacity = viewClass.getMethod("getCapacity");
                itemVariantToStack = itemVariantClass.getMethod("toStack", int.class);
                txOpenOuter = txClass.getMethod("openOuter");
                txCommit = txClass.getMethod("commit");
                txClose = txClass.getMethod("close");
                storageInsert = storageClass.getMethod("insert", Object.class, long.class, txContextClass);
                storageExtract = storageClass.getMethod("extract", Object.class, long.class, txContextClass);
                storageSimulateInsert = storageClass.getMethod("simulateInsert", Object.class, long.class, txContextClass);
                storageIterator = storageClass.getMethod("iterator");
                storageNonEmptyViews = storageClass.getMethod("nonEmptyViews");
                utilToFluidView = forgeCompatUtilClass.getMethod("toFluidStorageView", net.minecraftforge.fluids.FluidStack.class);
                utilToFabricBucket = forgeCompatUtilClass.getMethod("toFabricBucket", int.class);
                utilToForgeBucket = forgeCompatUtilClass.getMethod("toForgeBucket", int.class);
                utilToForgeStackView = forgeCompatUtilClass.getMethod("toForgeFluidStack", viewClass);
                utilToForgeStackVar = forgeCompatUtilClass.getMethod("toForgeFluidStack", fluidVariantClass, int.class);
                fluidStorageItemField = fluidStorageClass.getField("ITEM");
                computingLockField = transferApiForgeCompatClass.getField("COMPUTING_CAPABILITY_LOCK");
                initialized = true;
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("FluidBridge: fabric-transfer-api reflection init failed", e);
            }
        }
    }

    /**
     * 查找物品栈的 fabric 流体存储。
     *
     * @return {storage, ctx} 二元组；无 fabric 流体 API 的栈返回 null。
     */
    public static Object[] findItemStorage(ItemStack stack) {
        init();
        try {
            Object ctx = ctxWithInitial.invoke(null, stack);
            Object lookup = fluidStorageItemField.get(null);
            Object storage = ctxFind.invoke(ctx, lookup);
            if (storage == null) {
                return null;
            }
            return new Object[]{storage, ctx};
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    /**
     * 方块侧桥接同款重入锁（TransferApiForgeCompat.COMPUTING_CAPABILITY_LOCK，
     * 与方块侧共用同一 ThreadLocal——fabric 存储内部再查 Forge 能力时不重入）。
     */
    public static ThreadLocal<Boolean> computingLock() {
        init();
        try {
            return (ThreadLocal<Boolean>) computingLockField.get(null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    // ---- 事务助手（镜像 ForgeCompat 的 try-with-resources 生命周期） ----

    public static Object openTransaction() {
        init();
        try {
            return txOpenOuter.invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: openOuter failed", e);
        }
    }

    public static void commitTransaction(Object tx) {
        init();
        try {
            txCommit.invoke(tx);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: commit failed", e);
        }
    }

    /** 未提交事务一律 abort（close 语义与 AutoCloseable 一致）。 */
    public static void closeTransaction(Object tx) {
        init();
        try {
            txClose.invoke(tx);
        } catch (ReflectiveOperationException e) {
            // 关闭失败不影响主流程
        }
    }

    // ---- 供 FluidStorageItemBridge 使用的公共反射入口 ----

    public static Object storageIterate(Object storage) {
        init();
        try {
            return storageIterator.invoke(storage);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: storage iterator failed", e);
        }
    }

    public static Object storageNonEmptyViews(Object storage) {
        init();
        try {
            return storageNonEmptyViews.invoke(storage);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: nonEmptyViews failed", e);
        }
    }

    public static Object storageInsert(Object storage, Object variant, long amount, Object tx) {
        init();
        try {
            return storageInsert.invoke(storage, variant, amount, tx);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: storage.insert failed", e);
        }
    }

    public static Object storageExtract(Object storage, Object variant, long amount, Object tx) {
        init();
        try {
            return storageExtract.invoke(storage, variant, amount, tx);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: storage.extract failed", e);
        }
    }

    public static long storageSimulateInsert(Object storage, Object variant, long amount) {
        init();
        try {
            return (Long) storageSimulateInsert.invoke(storage, variant, amount, null);
        } catch (ReflectiveOperationException e) {
            return 0L;
        }
    }

    public static Object toFluidVariant(net.minecraftforge.fluids.FluidStack stack) {
        init();
        try {
            return utilToFluidView.invoke(null, stack);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: toFluidStorageView failed", e);
        }
    }

    public static int toFabricBucket(int mb) {
        init();
        try {
            return (Integer) utilToFabricBucket.invoke(null, mb);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: toFabricBucket failed", e);
        }
    }

    public static int toForgeBucket(int units) {
        init();
        try {
            return (Integer) utilToForgeBucket.invoke(null, units);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("FluidBridge: toForgeBucket failed", e);
        }
    }

    public static net.minecraftforge.fluids.FluidStack toForgeFluidStack(Object view) {
        init();
        try {
            return (net.minecraftforge.fluids.FluidStack) utilToForgeStackView.invoke(null, view);
        } catch (ReflectiveOperationException e) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
    }

    public static net.minecraftforge.fluids.FluidStack toForgeFluidStack(Object variant, int units) {
        init();
        try {
            return (net.minecraftforge.fluids.FluidStack) utilToForgeStackVar.invoke(null, variant, units);
        } catch (ReflectiveOperationException e) {
            return net.minecraftforge.fluids.FluidStack.EMPTY;
        }
    }

    public static Object viewGetResource(Object view) {
        init();
        try {
            return viewGetResource.invoke(view);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public static long viewGetAmount(Object view) {
        init();
        try {
            return (Long) viewGetAmount.invoke(view);
        } catch (ReflectiveOperationException e) {
            return 0L;
        }
    }

    public static long viewGetCapacity(Object view) {
        init();
        try {
            return (Long) viewGetCapacity.invoke(view);
        } catch (ReflectiveOperationException e) {
            return 0L;
        }
    }

    /**
     * 排空/装满提交后容器的当前形态（fabric 纯变体存储：exchange 只更新 ctx
     * 槽位内容、不改写原栈——Forge 侧必须从这里取"空桶/空单元"）。
     */
    public static ItemStack containerFromCtx(Object ctx, ItemStack fallback) {
        init();
        try {
            Object slot = ctxGetMainSlot.invoke(ctx);
            Object variant = viewGetResource.invoke(slot);
            long amount = (Long) viewGetAmount.invoke(slot);
            return (ItemStack) itemVariantToStack.invoke(variant, (int) amount);
        } catch (ReflectiveOperationException e) {
            return fallback;
        }
    }

    /**
     * 静态一致性校验：反射解析出的方法必须为非 null 且签名合理（防 fabric
     * 版本升级后静默错位）。仅日志用途，不抛出。
     */
    static void sanityCheck() {
        Objects.requireNonNull(ctxWithInitial, "withInitial");
        Objects.requireNonNull(ctxFind, "find");
        Objects.requireNonNull(storageInsert, "Storage.insert");
        Objects.requireNonNull(storageExtract, "Storage.extract");
        Objects.requireNonNull(txOpenOuter, "Transaction.openOuter");
        Modifier.isStatic(ctxWithInitial.getModifiers());
    }
}
