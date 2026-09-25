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
 * 断点④：模式存储机水晶判定（crystal_memory_compat_study §5.3 M4，0.1.1 双向化）。
 *
 * <p>目标：{@code BlockEntityPatternStorage.updateTileServer(Player, double)}
 * （jar 实证：单一定义、无重载；switch 内 case2 写水晶按钮 L194、case3 读水晶
 * 按钮 L202）。字节码逐条核实：方法内 4 处 {@code ItemStack.getItem()}
 * （SRG {@code m_41720_}）全部位于 case2/case3 的水晶判定路径（case2 偏移
 * 186 instanceof + 197 checkcast；case3 偏移 252 instanceof + 263 checkcast），
 * 无其他语义；1 处 {@code writecontentsTag}（case2 写按钮，偏移 224）、
 * 1 处 {@code readItemStack}（case3 读按钮，偏移 271）。
 *
 * <p>修复（0.1.0）：getItem 调用点重定向（同 M3）；writecontentsTag 重定向到
 * {@code CrystalMemoryBridge.writePattern}（写 Pattern + 删 UuTemplate）。
 *
 * <p>0.1.1 双向化（用户裁决：两边水晶互相可用）：
 * <ul>
 *   <li>writecontentsTag（case2）→ {@code writePattern}：写双键
 *       （Pattern + UuTemplate），0.1.0 的删 UuTemplate 守卫移除；
 *       IC2 模式存储机 import 按钮读 {@code UuTemplate} 天然生效。</li>
 *   <li>readItemStack（case3）→ {@code readPattern}：优先 Pattern，
 *       无则读 UuTemplate.ItemId 转换（count=1）——IC2 机器写过的水晶
 *       （仅 UuTemplate）IU 侧读按钮同样可用。</li>
 * </ul>
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

    @Redirect(method = "updateTileServer",
            at = @At(value = "INVOKE",
                    target = "Lcom/denfop/items/ItemCrystalMemory;readItemStack(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"),
            require = 1)
    private ItemStack uubridge$readPattern(ItemCrystalMemory receiver, ItemStack crystal) {
        return CrystalMemoryBridge.readPattern(crystal);
    }
}
