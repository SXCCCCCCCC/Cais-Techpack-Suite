package com.sxcccccccc.iuunify.mixin;

import com.sxcccccccc.iuunify.compat.FluidBridge;
import com.sxcccccccc.iuunify.compat.FluidStorageItemBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 0.3.4 任务一分支一：补齐 fabric-transfer-api 的"物品侧 Forge 能力桥接"。
 *
 * <p>根因（反编译实证）：{@code TransferApiForgeCompat} 的方块侧桥接完整
 * （FluidStorage.SIDED → Forge FLUID_HANDLER，泵可用），但物品侧
 * {@code onAttachItemStackCapabilities} 是空方法体 → IC2R 的桶/流体单元
 * （fabric 物品，只注册 {@code FluidStorage.ITEM} 物品 API）不暴露 Forge
 * {@code FLUID_HANDLER_ITEM} → IU 机器 UI 槽位
 * （{@code InventoryFluid.m_7013_}: handler == null → false）一律拒收。
 *
 * <p>修复：在本方法 HEAD 补上官方方块侧同款逻辑——经
 * {@code ContainerItemContext.withInitial(stack).find(FluidStorage.ITEM)} 命中
 * fabric 流体存储的栈，注册 {@link FluidStorageItemBridge} 为 Forge 能力提供器。
 * 时序验证（Forge 字节码实证）：AttachCapabilitiesEvent 由 ItemStack 构造时
 * forgeInit → gatherCapabilities 触发，event.addCapability 只登记不查询，handler
 * 内不调用 getCapability，无重入；CapabilityDispatcher 的 caps 数组父提供器在前
 * （原生 initCapabilities 能力优先），本桥只填补无原生能力的缺口，不遮蔽任何
 * 原生 Forge 流体能力。
 *
 * <p>重入锁：与方块侧共用同一个 {@code COMPUTING_CAPABILITY_LOCK} ThreadLocal
 * （公共静态字段，反射读取），fabric 存储实现回查 Forge 能力期间跳过桥接，
 * 语义与官方方块侧逐字一致。
 */
@Mixin(targets = "net.fabricmc.fabric.impl.transfer.compat.TransferApiForgeCompat")
public abstract class TransferApiForgeCompatItemBridgeMixin {

    /**
     * 目标（反编译实证）：{@code private static void
     * onAttachItemStackCapabilities(AttachCapabilitiesEvent<ItemStack>)}；描述符
     * {@code (Lnet/minecraftforge/event/AttachCapabilitiesEvent;)V}。
     * remap=false：目标类是 fabric 成员（字符串原样进 jar）。
     * 目标方法为 static → handler 必须为 static，参数位 [事件] + [CallbackInfo]。
     */
    @Inject(
            method = "onAttachItemStackCapabilities(Lnet/minecraftforge/event/AttachCapabilitiesEvent;)V",
            at = @At("HEAD"),
            require = 1,
            remap = false
    )
    private static void iuunify$itemFluidBridge(AttachCapabilitiesEvent<ItemStack> event, CallbackInfo ci) {
        ThreadLocal<Boolean> lock = null;
        try {
            lock = FluidBridge.computingLock();
            if (lock != null && Boolean.TRUE.equals(lock.get())) {
                return; // fabric 存储回查 Forge 能力期间，与方块侧同锁同语义
            }
            ItemStack stack = event.getObject();
            if (stack == null || stack.isEmpty()) {
                return;
            }
            if (lock != null) {
                lock.set(true);
            }
            Object[] found;
            try {
                found = FluidBridge.findItemStorage(stack);
            } finally {
                if (lock != null) {
                    lock.set(false);
                }
            }
            if (found == null) {
                return; // 无 fabric 流体存储（原版桶/纯 Forge 物品等）→ 维持空方法体行为
            }
            event.addCapability(new ResourceLocation("iuunify", "fluid_handler_item"),
                    new FluidStorageItemBridge(found[0], found[1], stack));
        } catch (Throwable t) {
            // 桥接失败不阻塞原流程（原方法体为空，行为不变）
        }
    }
}
