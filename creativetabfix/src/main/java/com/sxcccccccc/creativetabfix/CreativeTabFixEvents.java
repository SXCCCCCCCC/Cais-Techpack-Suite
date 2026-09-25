package com.sxcccccccc.creativetabfix;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 创造标签页补齐。
 *
 * <p>重复添加是安全的：{@link BuildCreativeModeTabContentsEvent} 的载体是
 * {@code MutableHashedLinkedMap<ItemStack, TabVisibility>}，{@code accept} 落到
 * {@code entries.put(...)}，同一 ItemStack 重复放入只是覆盖，不会产生重复条目。
 * 因此即使目标标签页本来就有内容也不会重复。
 *
 * <p>物品一律以 {@code new ItemStack(item)} 补入。这对 GTCA 是正确的：GTCA 没有注册任何
 * GTCEu 特殊物品（{@code IComponentItem} / {@code IGTTool} / {@code LampBlockItem} —— 全源码
 * grep 零命中），全都是普通 {@code Item} / {@code BlockItem}。而 GTCEu 自己的
 * {@code RegistrateDisplayItemsGenerator} 对这三类之外也正是走 {@code output.accept(item)}，
 * 所以口径一致。
 */
@Mod.EventBusSubscriber(modid = CreativeTabFix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabFixEvents {

    private CreativeTabFixEvents() {
    }

    /**
     * 默认优先级：标签页构建时把配置声明的命名空间补齐。
     *
     * <p>触发时机由 {@code ForgeHooks.onCreativeModeTabBuildContents} 决定：
     * 先跑该标签页自己的 DisplayItemsGenerator 填 entries，再 post 本事件，最后才把 entries
     * 刷进标签页。所以这里补的条目一定会被渲染（GTCA 的生成器是空实现，补入前 entries 为空）。
     */
    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceLocation tabId = event.getTabKey().location();

        for (CreativeTabFixConfig.Mapping mapping : CreativeTabFixConfig.parsed) {
            if (!tabId.equals(mapping.tab())) {
                continue;
            }
            // 读 entries 是安全的：Map 在生成器跑完之后、本处理器之前的状态
            boolean wasEmpty = event.getEntries().isEmpty();
            int added = fill(event, mapping.namespace());
            CreativeTabFix.LOGGER.info("[{}] 向创造标签页 {} 补入命名空间 {} 的物品 {} 个（补入前该标签页{}）",
                    CreativeTabFix.MOD_ID, tabId, mapping.namespace(), added,
                    wasEmpty ? "为空——上游没给它装 DisplayItemsGenerator" : "已有条目");
        }
    }

    /**
     * 加载完成后校验映射目标：目标标签页必须真的被某个模组注册过，否则整条映射会静默失效。
     * 此时创造标签页可能还没构建（构建是懒的、客户端资源重载时才做），但
     * {@code CREATIVE_MODE_TAB} 注册表早已填满，所以这个校验与时机构建无关。
     */
    @SubscribeEvent
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        for (CreativeTabFixConfig.Mapping mapping : CreativeTabFixConfig.parsed) {
            if (!BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(mapping.tab())) {
                CreativeTabFix.LOGGER.error("[{}] 映射目标标签页 {} 不存在（没有模组注册它），命名空间 {} 不会被补入任何标签页",
                        CreativeTabFix.MOD_ID, mapping.tab(), mapping.namespace());
            }
        }
    }

    private static int fill(BuildCreativeModeTabContentsEvent event, String namespace) {
        int added = 0;
        for (Item item : ForgeRegistries.ITEMS) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || item == Items.AIR) {
                continue;
            }
            if (!namespace.equals(id.getNamespace())) {
                continue;
            }
            event.accept(new ItemStack(item));
            added++;
        }
        return added;
    }
}
