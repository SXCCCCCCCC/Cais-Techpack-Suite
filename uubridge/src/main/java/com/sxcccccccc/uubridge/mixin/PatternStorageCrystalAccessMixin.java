package com.sxcccccccc.uubridge.mixin;

import com.denfop.blockentity.mechanism.BlockEntityPatternStorage;
import com.denfop.items.ItemCrystalMemory;
import com.sxcccccccc.uubridge.compat.CrystalMemoryBridge;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 断点④：模式存储机水晶判定（crystal_memory_compat_study §5.3 M4）。
 *
 * <p>目标：{@code BlockEntityPatternStorage.updateTileServer(Player, double)}
 * （jar 实证：单一定义、无重载；switch 内 case2 写水晶按钮 L194、case3 读水晶
 * 按钮 L202）。字节码逐条核实：方法内 4 处 {@code ItemStack.getItem()}
 * （SRG {@code m_41720_}）全部位于 case2/case3 的水晶判定路径（case2 偏移
 * 186 instanceof + 197 checkcast；case3 偏移 252 instanceof + 263 checkcast），
 * 无其他语义；1 处 {@code writecontentsTag}（case2 写按钮，偏移 224）。
 *
 * <p>修复：getItem 调用点重定向（同 M3）；writecontentsTag 重定向到
 * {@code CrystalMemoryBridge.writePattern}（写 Pattern + 删 UuTemplate）。
 * case3 的 {@code readItemStack} 保持原样——只读 {@code "Pattern"} 键，IC2
 * 机器写过的水晶（只有 UuTemplate）读为空、原方法 null 分支安全跳过。
 */
@Mixin(value = BlockEntityPatternStorage.class, remap = false)
public abstract class PatternStorageCrystalAccessMixin {

    @Redirect(method = "updateTileServer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;m_41720_()Lnet/minecraft/world/item/Item;"),
            require = 1)
    private Item uubridge$crystalForCheck(ItemStack stack) {
        return CrystalMemoryBridge.getItemForCrystalCheck(stack);
    }

    @Redirect(method = "updateTileServer",
            at = @At(value = "INVOKE",
                    target = "Lcom/denfop/items/ItemCrystalMemory;writecontentsTag(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"),
            require = 1)
    private void uubridge$writePattern(ItemCrystalMemory receiver, ItemStack crystal, ItemStack recorded) {
        CrystalMemoryBridge.writePattern(crystal, recorded);
    }
}
