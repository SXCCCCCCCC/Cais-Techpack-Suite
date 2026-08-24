package com.sxcccccccc.iuunify.compat;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * fabric 物品流体存储 → Forge {@code IFluidHandlerItem} 桥接（0.3.4 任务一分支一）。
 *
 * <p>镜像 fabric 官方 {@code FluidStorageFluidHandler}（fill/drain 的事务与换算
 * 语义逐行一致），关键差异在 {@link #getContainer()}：官方实现返回构造时传入的
 * 原始栈——而 IC2R 的桶/流体单元存储是纯 exchange 存储（
 * {@code FluidCellStorage}/{@code ModFluids$BucketFluidStorage} 字节码实证：
 * 状态只存在于 {@code ContainerItemContext} 主槽位，原始 ItemStack 永不改写），
 * 官方写法会让 IU 机器 UI（InventoryFluid.drain/fill → getContainer → output）把
 * "未消耗的原栈"当结果交出 → 流体复制。
 *
 * <p>本实现 {@link #getContainer()} 改从 ctx 主槽位读取提交后的容器形态
 * （drain 后 = 空单元/空桶，fill 后 = 满单元/满桶），与 IC2R 纯 exchange 语义
 * 对齐，槽位扣减 + 结果输出各得其所，无复制无吞损。
 *
 * <p>fabric 类不在本 mod 编译 classpath，全部经 {@link FluidBridge} 反射访问。
 */
public class FluidStorageItemBridge implements IFluidHandlerItem, ICapabilityProvider {

    private final Object storage;
    private final Object ctx;
    private final ItemStack original;
    /** 构造时快照的视图（官方实现同款缓存；罐内容变更后 getFluidInTank 可能陈旧，行为与官方一致）。 */
    private final Object[] views;

    public FluidStorageItemBridge(Object storage, Object ctx, ItemStack original) {
        this.storage = storage;
        this.ctx = ctx;
        this.original = original;
        List<Object> list = new ArrayList<>();
        Iterator<?> it = (Iterator<?>) FluidBridge.storageIterate(storage);
        while (it.hasNext()) {
            list.add(it.next());
        }
        this.views = list.toArray();
    }

    // ---- ICapabilityProvider（本桥自身即能力提供器，事件注册时直接挂上） ----

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            return (LazyOptional<T>) LazyOptional.of(() -> this);
        }
        return LazyOptional.empty();
    }

    // ---- IFluidHandlerItem ----

    @Override
    public int getTanks() {
        return this.views.length;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (tank < 0 || tank >= this.views.length) {
            return FluidStack.EMPTY;
        }
        return FluidBridge.toForgeFluidStack(this.views[tank]);
    }

    @Override
    public int getTankCapacity(int tank) {
        if (tank < 0 || tank >= this.views.length) {
            return 0;
        }
        return (int) FluidBridge.viewGetCapacity(this.views[tank]);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        // 官方同款：simulateInsert(null tx)。fabric 存储对 null tx 的兼容性由
        // 官方桥接承担，此处任何异常都保守返回 false（反射层已兜底）。
        return FluidBridge.storageSimulateInsert(this.storage,
                FluidBridge.toFluidVariant(stack),
                FluidBridge.toFabricBucket(stack.getAmount())) > 0L;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return 0;
        }
        Object tx = null;
        long filled;
        try {
            tx = FluidBridge.openTransaction();
            filled = (Long) FluidBridge.storageInsert(this.storage,
                    FluidBridge.toFluidVariant(resource),
                    FluidBridge.toFabricBucket(resource.getAmount()),
                    tx);
            if (action.execute()) {
                FluidBridge.commitTransaction(tx);
                tx = null;
            }
        } finally {
            if (tx != null) {
                FluidBridge.closeTransaction(tx);
            }
        }
        return FluidBridge.toForgeBucket((int) filled);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) {
            return FluidStack.EMPTY;
        }
        Object tx = null;
        Object variant = FluidBridge.toFluidVariant(resource);
        long drained;
        try {
            tx = FluidBridge.openTransaction();
            drained = (Long) FluidBridge.storageExtract(this.storage, variant,
                    FluidBridge.toFabricBucket(resource.getAmount()), tx);
            if (action.execute()) {
                FluidBridge.commitTransaction(tx);
                tx = null;
            }
        } finally {
            if (tx != null) {
                FluidBridge.closeTransaction(tx);
            }
        }
        return FluidBridge.toForgeFluidStack(variant, (int) drained);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        Iterator<?> it = ((Iterable<?>) FluidBridge.storageNonEmptyViews(this.storage)).iterator();
        if (!it.hasNext()) {
            return FluidStack.EMPTY;
        }
        Object view = it.next();
        Object variant = FluidBridge.viewGetResource(view);
        Object tx = null;
        long drained;
        try {
            tx = FluidBridge.openTransaction();
            drained = (Long) FluidBridge.storageExtract(this.storage, variant,
                    FluidBridge.toFabricBucket(maxDrain), tx);
            if (action.execute()) {
                FluidBridge.commitTransaction(tx);
                tx = null;
            }
        } finally {
            if (tx != null) {
                FluidBridge.closeTransaction(tx);
            }
        }
        return FluidBridge.toForgeFluidStack(variant, (int) drained);
    }

    /**
     * 关键修复点：返回 ctx 主槽位在事务提交后的当前形态，而非构造时快照的原始栈。
     * 纯 exchange 存储下，只有 ctx 槽位反映"消耗后容器长什么样"。
     */
    @Override
    public ItemStack getContainer() {
        return FluidBridge.containerFromCtx(this.ctx, this.original);
    }
}
