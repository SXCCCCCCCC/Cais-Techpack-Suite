package com.sxcccccccc.uubridge.compat;

import com.denfop.IUItem;
import com.denfop.items.ItemCrystalMemory;
import com.denfop.utils.ModUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 模式水晶桥接 helper（mixin 包外的普通类，mixin 包内禁止放非 mixin 类）。
 *
 * <p>目标：让 IU 机器把 IC2 水晶（{@code ic2_120:crystal_memory}）当作合法
 * 模式载体。IC2 水晶是普通 Item，数据全在 NBT；IU 侧对水晶的唯一依赖是
 * 两处 item 判定（槽位按 item 实例过滤、5 处 {@code instanceof ItemCrystalMemory}
 * 后调 {@code readItemStack}/{@code writecontentsTag}——这两个方法只读写
 * NBT 键 {@code "Pattern"}，与 item 类无关）。
 *
 * <p>关键技巧：{@link #getItemForCrystalCheck} 把 IC2 水晶伪装成
 * {@code IUItem.crystalMemory.getItem()}，使原始方法内的
 * {@code instanceof ItemCrystalMemory} 与 {@code (ItemCrystalMemory) getItem()}
 * 转型原样通过，随后 {@code readItemStack}/{@code writecontentsTag} 只操作
 * NBT（{@code ModUtils.nbt}），对 IC2 水晶同样成立——不重写方法体。
 *
 * <p>单向数据策略（用户裁决）：IU 机器读写水晶上的 IU 格式 {@code "Pattern"}
 * 键（完整序列化 ItemStack）；写入时删除 IC2 {@code "UuTemplate"} 键——
 * IC2 复制机/模式存储机不校验模板是否在白名单内，带非白名单 ItemId 的水晶
 * 模板进入 IC2 复制机 = 成本 0 免费复制（漏洞），删除该键即封死。
 * 不做双向桥（IC2 机器读 IU 模式），IC2 机器读写的是各自格式，互不冲突。
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

    /** 镜像 {@code ItemCrystalMemory.readItemStack}：读 {@code "Pattern"}（IU 格式，完整序列化 ItemStack）。 */
    public static ItemStack readPattern(ItemStack crystal) {
        CompoundTag nbt = ModUtils.nbt(crystal);
        CompoundTag contentTag = nbt.getCompound("Pattern");
        return ItemStack.of(contentTag);
    }

    /**
     * 镜像 {@code ItemCrystalMemory.writecontentsTag}：写 {@code "Pattern"}，并删除
     * IC2 {@code "UuTemplate"} 键（防 IC2 复制机 0 成本免费复制漏洞）。
     */
    public static void writePattern(ItemStack crystal, ItemStack recorded) {
        CompoundTag nbt = ModUtils.nbt(crystal);
        CompoundTag contentTag = new CompoundTag();
        recorded.save(contentTag);
        nbt.put("Pattern", contentTag);
        nbt.remove("UuTemplate");
    }
}
