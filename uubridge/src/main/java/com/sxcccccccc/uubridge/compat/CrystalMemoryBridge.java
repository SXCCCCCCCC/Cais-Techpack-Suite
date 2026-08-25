package com.sxcccccccc.uubridge.compat;

import com.denfop.IUItem;
import com.denfop.items.ItemCrystalMemory;
import com.denfop.utils.ModUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 模式水晶桥接 helper（mixin 包外的普通类，mixin 包内禁止放非 mixin 类）。
 *
 * <p>目标：让 IU 机器把 IC2 水晶（{@code ic2_120:crystal_memory}）当作合法
 * 模式载体，并让两种格式<strong>双向互通</strong>（用户裁决 0.1.1：成本已统一
 * 用 IC2 计算，两边水晶应该互相可用；撤销 0.1.0 的单向设计，移除写时删除
 * UuTemplate 的守卫）。IC2 水晶是普通 Item，数据全在 NBT；IU 侧对水晶的
 * 唯一依赖是两处 item 判定（槽位按 item 实例过滤、
 * {@code instanceof ItemCrystalMemory} 后调 {@code readItemStack}/
 * {@code writecontentsTag}——这两个方法只读写 NBT 键 {@code "Pattern"}，
 * 与 item 类无关）。
 *
 * <p>两种 NBT 格式（两侧源码逐行取证，互不冲突、可共存于一键）：
 * <ul>
 *   <li>IU {@code "Pattern"}：{@code recorded.save(contentTag)} 的<strong>完整
 *       序列化 ItemStack</strong>（含 id/Count/tag），{@code ItemStack.of} 读回
 *       （ItemCrystalMemory.writecontentsTag/readItemStack 实读实写，dev 源码
 *       33-58 行实证）。</li>
 *   <li>IC2 {@code "UuTemplate"}：{@code {ItemId: "modid:itemid"}}——仅 itemId
 *       一个字段（UuTemplateData.kt：{@code UuTemplateEntry.toNbt} 只写
 *       {@code putString("ItemId", itemId)}，{@code fromNbt} 只读
 *       {@code getString("ItemId")}；成本 {@code uuCostUb} 是 getter 实时查
 *       {@code Ic2Config.getReplicationCostUb}，不存水晶上）。</li>
 * </ul>
 *
 * <p>双向桥（{@link #writePattern} / {@link #readPattern}）：
 * <ul>
 *   <li><strong>IU 写</strong>：一次调用同写两键——{@code "Pattern"}（IU 完整栈）
 *       与 {@code "UuTemplate"}（IC2 格式，itemId = Pattern 栈的注册 id），
 *       保证两键同步一致；IC2 机器读它自己的格式天然生效，零 IC2 侧改动。</li>
 *   <li><strong>IU 读</strong>：优先读 {@code "Pattern"}（同一次写入的完整栈）；
 *       没有则读 {@code "UuTemplate"} 的 {@code ItemId} 并转成 ItemStack
 *       （count=1——IC2 模板不存数量，IC2 复制机产出恒 1 个，语义对齐）。</li>
 * </ul>
 *
 * <p>关键技巧：{@link #getItemForCrystalCheck} 把 IC2 水晶伪装成
 * {@code IUItem.crystalMemory.getItem()}，使原始方法内的
 * {@code instanceof ItemCrystalMemory} 与 {@code (ItemCrystalMemory) getItem()}
 * 转型原样通过，随后 read/write 落点被重定向到本桥（M3/M4 的 @Redirect）。
 */
public final class CrystalMemoryBridge {

    private static final ResourceLocation IC2_CRYSTAL_ID = new ResourceLocation("ic2_120", "crystal_memory");

    private CrystalMemoryBridge() {
    }

    /** IC2 水晶（按注册 id 识别，与 IC2 侧 {@code isCrystalMemory} 同语义，不关心 NBT）。 */
    public static boolean isIc2Crystal(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        if (item == null) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && IC2_CRYSTAL_ID.equals(id);
    }

    /** IU 水晶 || IC2 水晶。 */
    public static boolean isCrystalMemory(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return stack.getItem() instanceof ItemCrystalMemory || isIc2Crystal(stack);
    }

    /**
     * 把 IC2 水晶伪装成 IU 水晶（供 {@code instanceof} 判定与 {@code checkcast} 通过）。
     * 非 IC2 水晶原样返回。
     */
    public static Item getItemForCrystalCheck(ItemStack stack) {
        if (isIc2Crystal(stack)) {
            // IUItem.crystalMemory 是 DataSimpleItem 注册包装；运行时可能尚未注册
            // （注册时序）或注册被取消（iudebug banned_registrations），null 守卫回退。
            if (IUItem.crystalMemory != null && IUItem.crystalMemory.getItem() != null) {
                return IUItem.crystalMemory.getItem();
            }
        }
        return stack.getItem();
    }

    /**
     * 双向读：优先 {@code "Pattern"}（IU 完整序列化 ItemStack）；为空/缺失则回落
     * IC2 {@code "UuTemplate"}.{@code ItemId} 并转换为 ItemStack（count=1）。
     * 两者都无 → {@code ItemStack.EMPTY}（原方法空分支安全处理）。
     */
    public static ItemStack readPattern(ItemStack crystal) {
        CompoundTag nbt = ModUtils.nbt(crystal);

        // IU 格式优先（同一次写入的完整栈，含 NBT）
        CompoundTag patternTag = nbt.getCompound("Pattern");
        if (!patternTag.isEmpty()) {
            ItemStack out = ItemStack.of(patternTag);
            if (!out.isEmpty()) {
                return out;
            }
        }

        // 回落 IC2 格式：UuTemplate -> {ItemId: "modid:itemid"}
        CompoundTag uuTemplate = nbt.getCompound("UuTemplate");
        String itemId = uuTemplate.getString("ItemId");
        if (!itemId.isBlank()) {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id != null) {
                Item item = ForgeRegistries.ITEMS.getValue(id);
                if (item != null && item != Items.AIR) {
                    return new ItemStack(item, 1);
                }
            }
        }

        return ItemStack.EMPTY;
    }

    /**
     * 双向写：一次调用同写两键，保证同步一致——
     * ① IU {@code "Pattern"}：{@code recorded.save()} 完整序列化 ItemStack；
     * ② IC2 {@code "UuTemplate"}：{@code {ItemId: <pattern 栈注册 id>}}，
     *    与 {@code UuTemplateData.UuTemplateEntry.toNbt} 完全同构（键名
     *    {@code "UuTemplate"} / 字段 {@code "ItemId"}），IC2 机器
     *    {@code getUuTemplate()} 直接可读。
     * 0.1.1 起移除 0.1.0 的"写时删除 UuTemplate"单向守卫（用户裁决双向）。
     */
    public static void writePattern(ItemStack crystal, ItemStack recorded) {
        CompoundTag nbt = ModUtils.nbt(crystal);

        // IU 格式（完整序列化 ItemStack）
        CompoundTag contentTag = new CompoundTag();
        recorded.save(contentTag);
        nbt.put("Pattern", contentTag);

        // IC2 格式（仅 itemId；IC2 模板语义不存 count/NBT）
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(recorded.getItem());
        if (id != null) {
            CompoundTag uuTemplate = new CompoundTag();
            uuTemplate.putString("ItemId", id.toString());
            nbt.put("UuTemplate", uuTemplate);
        }
    }
}
