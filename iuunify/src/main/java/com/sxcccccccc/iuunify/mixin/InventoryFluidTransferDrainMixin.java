package com.sxcccccccc.iuunify.mixin;

import com.denfop.inventory.InventoryFluid;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 0.3.7 修复二：InventoryFluid.transferToTank 槽侧 drain 的流体选择（UI 槽喂入路径）。
 *
 * <p>背景（与 FluidTankFillUnifiedMixin 同一断点链的槽侧半边）：updateEntityServer
 * 的槽→罐转移走 {@code InventoryFluid.transferToTank}（javap 实证），其内部
 * {@code this.drain(Fluid, int, MutableObject, boolean)} 的请求流体 = 罐内现有流体
 * （罐空时通配 Fluids.WATER）。罐非空且已含 ic2_120:creosote 时，槽内别家杂酚油
 * 物品按"请求流体 = ic2_120:creosote"去 drain——物品侧（IFluidHandlerItem.drain
 * 标准实现）按请求流体 isFluidEqual 过滤 → 空栈 → 转移失败，别家杂酚油在罐侧
 * 防混液（修复一）之前就被槽侧挡掉。
 *
 * <p>修复：@Redirect 该 drain 调用（transferToTank 内唯一一处）——请求流体为
 * 统一族且槽内物品流体与之同统一 tag 时，改按槽内自身流体 drain（物品侧放行，
 * 得到的正是槽内流体），灌入罐后由 FluidTankFillUnifiedMixin 的同 tag 宽限完成
 * 合并。其余路径（罐空通配、非统一流体、槽内非同族流体）原样回落。
 *
 * <p>目标（jar javap 实证）：{@code InventoryFluid.transferToTank(
 * net.minecraftforge.fluids.IFluidTank,
 * org.apache.commons.lang3.mutable.MutableObject, boolean)} 描述符
 * {@code (Lnet/minecraftforge/fluids/IFluidTank;Lorg/apache/commons/lang3/mutable/MutableObject;Z)Z}；
 * 内部唯一一处 {@code drain} 调用描述符
 * {@code (Lnet/minecraft/world/level/material/Fluid;ILorg/apache/commons/lang3/mutable/MutableObject;Z)Lnet/minecraftforge/fluids/FluidStack;}。
 * remap=false：目标类/方法是 mod 成员。
 *
 * <p>访问器：get(int) 是 Inventory 的 public 方法（drain 自身字节码即用
 * get:(I)ItemStack），无需 @Shadow（0.3.3 铁律）。
 */
@Mixin(value = InventoryFluid.class)
public abstract class InventoryFluidTransferDrainMixin {

    @Redirect(
            method = "transferToTank(Lnet/minecraftforge/fluids/IFluidTank;Lorg/apache/commons/lang3/mutable/MutableObject;Z)Z",
            at = @At(value = "INVOKE",
                    target = "Lcom/denfop/inventory/InventoryFluid;drain(Lnet/minecraft/world/level/material/Fluid;ILorg/apache/commons/lang3/mutable/MutableObject;Z)Lnet/minecraftforge/fluids/FluidStack;"),
            require = 1,
            remap = false
    )
    private FluidStack iuunify$drainUnified(InventoryFluid receiver, Fluid requested, int amount,
                                             MutableObject<ItemStack> obj, boolean simulate) {
        // 罐空（WATER 通配）或请求流体非统一族 → 原路径（槽侧本就可放行任意流体）
        if (requested == Fluids.WATER || !InputTagRegistry.isUnified(requested)) {
            return receiver.drain(requested, amount, obj, simulate);
        }
        ItemStack stack = receiver.get(0);
        if (stack != null && !stack.isEmpty()) {
            IFluidHandlerItem handler = stack.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM)
                    .resolve().orElse(null);
            if (handler != null) {
                FluidStack inItem = handler.getFluidInTank(0);
                // 槽内物品流体与罐内流体同统一 tag → 按槽内自身流体 drain（物品侧才放行）
                if (!inItem.isEmpty() && InputTagRegistry.sameUnifiedTag(inItem.getFluid(), requested)) {
                    return receiver.drain(inItem.getFluid(), amount, obj, simulate);
                }
            }
        }
        return receiver.drain(requested, amount, obj, simulate);
    }
}
