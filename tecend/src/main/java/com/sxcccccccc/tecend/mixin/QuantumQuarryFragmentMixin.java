package com.sxcccccccc.tecend.mixin;

import com.denfop.api.recipe.InventoryOutput;
import com.denfop.blockentity.mechanism.quarry.BlockEntityBaseQuantumQuarry;
import com.sxcccccccc.tecend.config.TecendConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 第 1 步「从虚空中来」的 IU 量子采石场路线：产出物品时按配置概率追加一块远古奖杯残片。
 *
 * <p><b>为什么钩 {@code InventoryOutput#add} 而不是在 {@code updateEntityServer} 的 TAIL 掷骰</b>：
 * 那个方法每刻都跑（它第一句就是超类 {@code BlockEntityInventory.updateEntityServer} 的调用），
 * 在 TAIL 掷骰等于每刻一次 —— 0.1% × 20/秒，比设计值高三个数量级。真正的「产出时刻」是
 * 这个方法里的那几次 {@code outputSlot.add(...)}。</p>
 *
 * <p><b>字节码实证</b>（{@code updateEntityServer} 里 {@code InventoryOutput.add(ItemStack)} 共 4 处）：</p>
 * <ul>
 *   <li>offset 152 —— 矿脉路线（{@code analyzer && vein.get() && type == VEIN && can_dig_vein}）</li>
 *   <li>offset 484 —— 通用池路线、{@code original} 为真、普通矿石</li>
 *   <li>offset 530 —— 同路线、宝石/碎片/四种原版物品的循环里（一次产出 1..k 个，会掷 k 次）</li>
 *   <li>offset 552 —— 通用池路线、{@code original} 为假</li>
 * </ul>
 * <p>一次挖掘只走其中一条分支，所以「产出一次 = 判定一次」。</p>
 *
 * <p><b>注入用 {@code require = 0}</b>：IU 在本包 mods.toml 里是可选依赖，插件
 * （{@link TecendMixinPlugin}）已按 mod 在场与否整个滤掉；万一将来 IU 改了方法签名，
 * 宁可这条功能静默失效，也不要在启动阶段把整合包带崩。</p>
 */
@Mixin(value = BlockEntityBaseQuantumQuarry.class, remap = false)
public abstract class QuantumQuarryFragmentMixin {

    /** 残片物品 id。GTCEu 给 tecend 这个材质派生的物品都落在 gtceu 命名空间下。 */
    @Unique
    private static final ResourceLocation TECEND_FRAGMENT =
            new ResourceLocation("gtceu", "trophy_fragment_clump");

    @Redirect(method = "updateEntityServer",
            at = @At(value = "INVOKE",
                    target = "Lcom/denfop/api/recipe/InventoryOutput;add(Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 0)
    private boolean tecend$fragmentWithOre(InventoryOutput out, ItemStack stack) {
        boolean added = out.add(stack);
        if (!added) {
            return false;
        }
        Item fragment = ForgeRegistries.ITEMS.getValue(TECEND_FRAGMENT);
        if (fragment == null || stack.is(fragment)) {
            return true;
        }
        BlockEntityBaseQuantumQuarry self = (BlockEntityBaseQuantumQuarry) (Object) this;
        if (self.rand.nextDouble() < TecendConfig.quarryFragmentChance()) {
            out.add(new ItemStack(fragment));
        }
        return true;
    }
}
