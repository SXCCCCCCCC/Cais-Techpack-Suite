package com.sxcccccccc.uubridge.mixin;

import com.denfop.blockentity.base.BlockEntityScanner;
import com.denfop.items.ItemCrystalMemory;
import com.sxcccccccc.uubridge.compat.CrystalMemoryBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 断点③：扫描机水晶判定（crystal_memory_compat_study §5.3 M3）。
 *
 * <p>目标：{@code BlockEntityScanner.isPatternRecorded(ItemStack)}（L171）与
 * {@code savetoDisk(ItemStack)}（L260）。两方法内各 2 处
 * {@code ItemStack.getItem()}（jar 实证 SRG 名 {@code m_41720_}）：
 * 第一处用于 {@code instanceof ItemCrystalMemory} 判定，第二处用于
 * {@code (ItemCrystalMemory) getItem()} 转型（checkcast）。字节码逐条核实
 * 两方法内 getItem 只出现在水晶判定处（@Redirect 语义范围安全）。
 *
 * <p>修复：getItem 调用点重定向到
 * {@code CrystalMemoryBridge.getItemForCrystalCheck}——IC2 水晶返回
 * {@code IUItem.crystalMemory.getItem()}，使 instanceof 与 checkcast 原样通过，
 * 随后 {@code readItemStack}/{@code writecontentsTag} 只操作 NBT（ModUtils.nbt），
 * 对 IC2 水晶同样成立。
 *
 * <p>写盘（savetoDisk 内 {@code writecontentsTag} 调用点）额外重定向到
 * {@code CrystalMemoryBridge.writePattern}：写 IU 格式 {@code "Pattern"} +
 * 删除 IC2 {@code "UuTemplate"} 键（防 IC2 复制机 0 成本免费复制漏洞）。
 */
@Mixin(value = BlockEntityScanner.class, remap = false)
public abstract class ScannerCrystalAccessMixin {

    @Redirect(method = {"isPatternRecorded", "savetoDisk"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;m_41720_()Lnet/minecraft/world/item/Item;"),
            require = 1)
    private Item uubridge$crystalForCheck(ItemStack stack) {
        return CrystalMemoryBridge.getItemForCrystalCheck(stack);
    }

    @Redirect(method = "savetoDisk",
            at = @At(value = "INVOKE",
                    target = "Lcom/denfop/items/ItemCrystalMemory;writecontentsTag(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"),
            require = 1)
    private void uubridge$writePattern(ItemCrystalMemory receiver, ItemStack crystal, ItemStack recorded) {
        CrystalMemoryBridge.writePattern(crystal, recorded);
    }
}
