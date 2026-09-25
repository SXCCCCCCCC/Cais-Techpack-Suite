package com.sxcccccccc.iuunify.mixin;

import com.denfop.IUItem;
import com.denfop.blockentity.mechanism.blastfurnace.api.InventoryBlastFurnace;
import com.denfop.blockentity.mechanism.blastfurnace.block.BlockEntityBlastFurnaceMain;
import com.denfop.inventory.Inventory;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阶段 0.4.3：IU 高炉输入槽（blast_furnace_input 路线）的硬编码物品身份换源，
 * 只重写 {@link InventoryBlastFurnace} 的两个方法（用户裁决最小修复——无 tag
 * 宽限、无规则表、无通用 helper；onLoaded()/BlastFHandler/其余一概不碰）。
 *
 * <p>背景（3.4.0.10 正式 jar javap 实证 + 桌面源码）：
 * {@code InventoryBlastFurnace.canPlaceItem}（源码 api/InventoryBlastFurnace.java:43-47）
 * 是高炉输入槽的唯一接收闸口（GUI 点击/SHIFT 转移经 {@code SlotInvSlot.mayPlace}
 * → {@code container.canPlaceItem}；自动化经 {@code BlockEntityInventory.canPlaceItem}
 * 同闸），原实现三支裸比对：铁锭、IU 钨锭（{@code instanceof ItemIngots &&
 * iuingot.getMeta==3}，meta3={@code ItemIngotsTypes.tungsten_ingot}）、IU 塑料板
 * （{@code equals(IUItem.plastic_plate.getItem())}）。OEI 统一后（kubejs/data/oei/
 * replacements/tungsten_ingot.json：IU 钨锭→gtceu:tungsten_ingot；plastic.json：
 * IU 塑料板→industrialforegoing:plastic），玩家手持统一品被逐支拒绝——即本 bug。
 * 按裁决把两处身份比较<b>直接换源</b>：{@code gtceu:tungsten_ingot} /
 * {@code industrialforegoing:plastic}。
 *
 * <p>输出选择 {@code set()}（源码 :20-38）同步换源：空→空、铁锭→
 * {@code IUItem.advIronIngot}（= iuingot meta23 钢锭，Register.java:1885）、
 * 钨→craftingelements 480、塑料→479。编号经 zh_cn.json 核对：
 * crafting_480=碳化钨锭（钨支）、crafting_479=碳纤维增强塑料（塑料支），与
 * 裁决一致。
 *
 * <p>实现方式（均 HEAD cancellable 完整复刻原方法结构、仅换身份比较）：
 * <ul>
 *   <li>{@code canPlaceItem}：纯谓词，复刻三支比较；</li>
 *   <li>{@code set()}：原方法首行 {@code super.set(index, content)}（=
 *       {@code Inventory.set}：{@code contents.set + setChanged()}，源码
 *       inventory/Inventory.java:321-324；setChanged 为空实现）——本混入类
 *       <b>extends {@code Inventory}</b>（utility superclass 标准手法：目标类
 *       的直接父类，invokespecial 在合并后类中被 JVM 验证器接受），handler 内
 *       {@code super.set(...)} 由编译器生成对父类实现的调用；输出选择分支按
 *       原结构（empty/iron/else 嵌套）重写。不选 @Shadow——父类字段
 *       {@code contents} 经 APT 静态校验不通过（只见目标类自身），且继承字段
 *       的 shadow 语义对读者不透明。</li>
 * </ul>
 *
 * <p>remap 说明（逐成员精确，混用 remap=true/false）：
 * <ul>
 *   <li>{@code canPlaceItem} 覆写 vanilla {@code Container.canPlaceItem}，运行时
 *       为 SRG 真名 {@code m_7013_}（official↔SRG）——该 @Inject 不带 remap
 *       属性，走类级默认 remap=true，经 refmap 标准映射（MixinGradle 生成
 *       canPlaceItem→m_7013_ 条目，APT 实证能找到映射）；</li>
 *   <li>{@code set} 是 IU 自有方法（dev=prod 同名），remap=true 时 APT 报
 *       "Unable to locate obfuscation mapping"（官方↔SRG 表无此方法，ERROR
 *       级）——该 @Inject 显式 remap=false，字面名命中；</li>
 *   <li>@Mixin 目标类名与方法体内原版类引用无映射问题。</li>
 * </ul>
 *
 * <p>物品引用经 {@link BuiltInRegistries} 按 id 惰性解析：编译期无 GT/IF jar
 * 依赖，且静态初始化（mixin 类加载）可能早于注册期；判定发生在玩家交互时刻
 * （注册表已完整），故每次调用现取。取不到时回落 {@link Items#AIR}——恒不匹配，
 * 语义等于该支失效，不会误放行。
 *
 * <p>mixin 铁律：handler 参数 = [目标方法参数] + [CallbackInfo]（this 不占参数位）；
 * require=1 防静默失效；handler 体内对原版类的引用由 reobfJar 自动重映射。
 */
@Mixin(value = InventoryBlastFurnace.class)
public abstract class InventoryBlastFurnaceSwapMixin extends Inventory {

    public InventoryBlastFurnaceSwapMixin() {
        super(0);
    }

    @Inject(
            method = "canPlaceItem(ILnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1
    )
    private void iuunify$canPlaceItem(int index, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Item item = stack.getItem();
        cir.setReturnValue(
                item.equals(Items.IRON_INGOT)              // 铁锭分支：原样保留
                        || item.equals(gtTungstenIngot())  // 原：instanceof ItemIngots && iuingot.getMeta==3
                        || item.equals(ifPlastic())        // 原：item.equals(IUItem.plastic_plate.getItem())
        );
    }

    @Inject(
            method = "set(ILnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 1
    )
    private void iuunify$set(int index, ItemStack content, CallbackInfoReturnable<ItemStack> cir) {
        // 复刻原方法首行 super.set(index, content)（Inventory.set 的全部逻辑）
        super.set(index, content);
        // 输出选择：按原方法结构复刻，仅换身份比较
        if (content.isEmpty()) {
            ((BlockEntityBlastFurnaceMain) this.base).outputStack = ItemStack.EMPTY;
        } else {
            if (content.getItem().equals(Items.IRON_INGOT)) {
                ((BlockEntityBlastFurnaceMain) this.base).outputStack = IUItem.advIronIngot;
            } else {
                if (content.getItem().equals(gtTungstenIngot())) {
                    ((BlockEntityBlastFurnaceMain) this.base).outputStack = new ItemStack(IUItem.crafting_elements.getStack(480));
                } else {
                    ((BlockEntityBlastFurnaceMain) this.base).outputStack = new ItemStack(IUItem.crafting_elements.getStack(479));
                }
            }
        }
        cir.setReturnValue(content);
    }

    private static Item gtTungstenIngot() {
        return BuiltInRegistries.ITEM.getOptional(new ResourceLocation("gtceu", "tungsten_ingot")).orElse(Items.AIR);
    }

    private static Item ifPlastic() {
        return BuiltInRegistries.ITEM.getOptional(new ResourceLocation("industrialforegoing", "plastic")).orElse(Items.AIR);
    }
}
