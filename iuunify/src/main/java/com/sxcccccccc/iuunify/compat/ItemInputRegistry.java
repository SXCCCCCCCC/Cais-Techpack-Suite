package com.sxcccccccc.iuunify.compat;

import com.denfop.recipe.IInputItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IU 机器物品输入的 tag 宽限注册表（阶段 0.4.0：物品输入侧 tag 化，流体 C 线
 * 流体输入 tag 注册表（旧统一栈，0.5.0 退役并迁移 fluidunifyapi）的物品推广；
 * 阶段三 A 方案）。
 *
 * <p>背景（源码实证，IndustrialUpgrade-3.4.0.10 dev 树）：IU 机器对物品的判定
 * 全部是<b>裸比对</b>，不走 OEI 替换层（`ItemStack.is(Item)` 钩子），也不走原版
 * Ingredient：
 * <ul>
 *   <li>配方匹配 —— {@code com.denfop.recipe.InputItemStack.matches(ItemStack)}
 *       （src/main/java/com/denfop/recipe/InputItemStack.java:59-62）：
 *       {@code subject.getItem() == this.input.getItem() && ModUtils.checkItemEquality(...)}
 *       （checkItemEquality 也是 getItem()== + NBT 相等，ModUtils.java:261-266）；</li>
 *   <li>槽位接受 —— {@code com.denfop.api.recipe.RecipeInputStack.matched(ItemStack)}
 *       （api/recipe/RecipeInputStack.java:33-44）：遍历 {@code getItemStack()}
 *       逐项 {@code input.getItem() == stack.getItem()}；由
 *       {@code InventoryRecipes.canPlaceItem}（api/recipe/InventoryRecipes.java:186
 *       {@code list.contains(itemStack)}）经 {@code RecipeArrayList.contains}
 *       调用，是 GUI 放置（ContainerMenuBase.java:47 {@code slot.mayPlace}）
 *       与 shift 转移的全部闸口；</li>
 *   <li>右键入口 —— {@code BlockEntityPrimalSiliconCrystalHandler.onActivated}
 *       （blockentity/mechanism/BlockEntityPrimalSiliconCrystalHandler.java:120）
 *       {@code instanceof ItemDust && IUItem.iudust.getMeta(...) == 60}。</li>
 * </ul>
 * 统一后同族物品（本包证据：{@code forge:dusts/silicon} =
 * gtceu:silicon_dust + industrialupgrade:itemdust/silicon_dust，kubejs 导出
 * tags/minecraft/item/forge/dusts/silicon.json；OEI 替换规则
 * kubejs/data/oei/replacements/silicon_dust.json：IU 硅粉 → GT 硅粉）在裸比对下
 * 互不相认——本类按「(基准输入 item → 统一 tag)」登记规则，判定 mixin 命中规则且
 * {@code subject.is(tag)} 即放行，否则回落到机器原始精确判定——输出侧/非规则配方
 * 全程零影响。
 *
 * <p>时序安全：规则表以 ResourceLocation 存键、判定时经
 * {@code BuiltInRegistries.ITEM.getKey(...)} 现取——本类不参与任何注册期前缀，且
 * 三个 mixin 都在运行时判定（机器交互/配方匹配时刻，tag 与注册表均已就绪）。
 *
 * <p>SOP 铁律（mc-bugfix-sop §5）：mixin 包内只许放 mixin 类——本 helper 放
 * compat 子包（与 InputTagRegistry 同构）。
 */
public final class ItemInputRegistry {

    // ---- forge: 命名空间 tag（数据由 GTCEu 自身发布，打包无需新增；导出实证成员） ----
    /** 硅粉族：gtceu:silicon_dust + industrialupgrade:itemdust/silicon_dust。 */
    public static final TagKey<Item> SILICON = itemTag("forge:dusts/silicon");

    // ---- 规则表：(基准输入 item 的 registry key) → 统一 tag ----
    // 键用 ResourceLocation：避免静态初始化时注册表未就绪（BuiltInRegistries.ITEM 的
    // RegistryObject.get() 可能为空），判定时经 getKey 惰性解析。
    private static final Map<ResourceLocation, TagKey<Item>> RULES = new LinkedHashMap<>();

    static {
        // 原始晶体生长室/晶体生长室 (silicon_recipe) 配方输入：IU 硅粉 → forge:dusts/silicon
        RULES.put(new ResourceLocation("industrialupgrade", "itemdust/silicon_dust"), SILICON);
    }

    private ItemInputRegistry() {
    }

    /**
     * 判定放宽：IInputItemStack（recipe 的输入谓词）的任一基准输入 item 命中规则、
     * 且传入 subject 命中该规则的统一 tag → 放行。
     *
     * <p>mixin handler 铁律：`this` 不占参数位，调用方把目标对象经 Object 参数上转
     * 传入（RecipeInputStackMatchedMixin 传 getInput()、ItemInputStackMatchMixin
     * 传自身强转 IInputItemStack），本方法签名保持纯数据参数。
     *
     * @param input   配方输入谓词（getInputs() 至少含一个有效栈）
     * @param subject 玩家/槽位中待判定的物品
     * @return 命中规则且 subject 属于统一 tag 时为 true（放行）；否则 false（回落原判定）
     */
    public static boolean widen(IInputItemStack input, ItemStack subject) {
        if (input == null || subject == null || subject.isEmpty()) {
            return false;
        }
        for (ItemStack base : input.getInputs()) {
            if (base == null || base.isEmpty()) {
                continue;
            }
            TagKey<Item> tag = RULES.get(BuiltInRegistries.ITEM.getKey(base.getItem()));
            if (tag != null && subject.is(tag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 手持物是否命中任一规则 tag（onActivated 入口守卫用）。
     * 只判「属于统一族」，不判具体规则——入口守卫随后按原方法语义自己复刻装入。
     */
    public static boolean isUnified(ItemStack subject) {
        if (subject == null || subject.isEmpty()) {
            return false;
        }
        for (TagKey<Item> tag : RULES.values()) {
            if (subject.is(tag)) {
                return true;
            }
        }
        return false;
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(path));
    }
}
