package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyBlockEntity;
import com.sxcccccccc.tecend.compat.Compat;
import net.mcreator.proofofhonor.init.ProofOfHonorModBlocks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;


/**
 * 本 mod 的创造栏条目 —— 全部塞进 proof_of_honor 自己的页签
 * （{@code proof_of_honor:proofofhonorbqy}，对应它的 lang 键 item_group.proof_of_honor.proofofhonorbqy），
 * 我们自己不另开页签。
 *
 * <p>放进来的东西：奖杯的五个阶段（远古 / 粗胚未处理 / 粗胚未喷砂 / 未充能 / 完成）、浇筑模具、金猪桶。</p>
 *
 * <p>做法照抄本工程里已验证的 creativetabfix：监听 {@link BuildCreativeModeTabContentsEvent}
 * （该事件在标签页自己的 DisplayItemsGenerator 跑完之后、条目刷入之前触发），按 tabKey 过滤后
 * {@code accept}。重复添加是安全的 —— 事件载体是 {@code MutableHashedLinkedMap<ItemStack, ...>}，
 * 同一 ItemStack 再放入只是覆盖。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class CreativeTabEntries {
    private CreativeTabEntries() {}

    /** proof_of_honor 的页签 id */
    private static final ResourceLocation TAB_ID = new ResourceLocation(Compat.PROOF_OF_HONOR, "proofofhonorbqy");

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (!Compat.isLoaded(Compat.PROOF_OF_HONOR)) {
            return;
        }
        if (!event.getTabKey().location().equals(TAB_ID)) {
            // 其它页签（GT 自己的那些）：「残片」族条目一律摘掉 —— 它们不在任何创造页签里之后，
            // JEI 的 showHiddenIngredients=false 就不会再显示（GTCA 那次的结论）。
            removeFragmentEntries(event);
            return;
        }

        // 奖杯五个阶段：同一个物品 + 不同 BlockEntityTag.type
        // 阶段 0 = 金奖杯（完成，默认）… 4 = 远古奖杯；从远古往完成列，符合"逐步推进"的阅读顺序
        for (int stage = TrophyBlockEntity.STAGE_MAX; stage >= TrophyBlockEntity.STAGE_FINISHED; stage--) {
            event.accept(trophyStack(stage));
        }

        // 浇筑模具三态 —— 现在是三个独立方块，直接放三个物品，不用 BlockStateTag
        event.accept(new ItemStack(ModBlocks.CASTING_MOLD_ITEM.get()));
        event.accept(new ItemStack(ModBlocks.FILLED_CASTING_MOLD_ITEM.get()));
        event.accept(new ItemStack(ModBlocks.COOLED_CASTING_MOLD_ITEM.get()));

        // 第 ⑧ 步：粗制完美模具水晶（我们自己那张，烧炼成带 NBT 的模式存储水晶）
        event.accept(new ItemStack(ModItems.RAW_CASTING_MOLD_CRYSTAL_MEMORY.get()));

        // 第 ⑦ 步的三个模具（材质刚到位）
        event.accept(new ItemStack(ModItems.ROUGH_MOLD_SAND.get()));
        event.accept(new ItemStack(ModItems.ROUGH_MOLD_WAX.get()));
        event.accept(new ItemStack(ModItems.PERFECT_MOLD.get()));

        event.accept(new ItemStack(ModItems.GOLD_PIG_BUCKET.get()));

        // 「残片」材质里定下来的八件（按用户给的 id 表，顺序即创造栏里的顺序）。
        // 其余同族部件（raw / crushed / purified / refined / small / tiny / gem_flawed… 等）
        // 不进创造栏，并由 kubejs 侧的 JEI 隐藏脚本一并从 JEI 里去掉。
        for (String id : FRAGMENT_ITEMS) {
            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));
            if (item != null) {
                event.accept(new ItemStack(item));
            }
        }
    }

    /**
     * 把「残片」族条目从当前页签里摘掉。
     *
     * <p>事件的载体是可变的 {@code MutableHashedLinkedMap<ItemStack, TabVisibility>}，而且在条目
     * 真正刷进页签之前触发，所以这里 remove 掉就不会进页签。<b>先收集再删</b>：边遍历边删会 CME。</p>
     *
     * <p>判定按 id 里含材质名（命名空间不敏感）—— 不点名，省得漏掉哪个部件。</p>
     */
    private static void removeFragmentEntries(BuildCreativeModeTabContentsEvent event) {
        List<ItemStack> toRemove = new ArrayList<>();
        for (var entry : event.getEntries()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(entry.getKey().getItem());
            if (id != null && id.getPath().contains("trophy_fragment")) {
                toRemove.add(entry.getKey());
            }
        }
        for (ItemStack stack : toRemove) {
            event.getEntries().remove(stack);
        }
    }

    /** 定下来的八件（顺序 = 流程顺序） */
    private static final String[] FRAGMENT_ITEMS = {
            "gtceu:trophy_fragment_clump",         // 远古奖杯残片
            "gtceu:trophy_fragment_shard",         // 粉碎的残片
            "gtceu:dirty_trophy_fragment_dust",    // 不那么干净的残片粉末
            "gtceu:impure_trophy_fragment_dust",   // 黏糊糊的残片粉末
            "gtceu:trophy_fragment_crystal",       // 小残片晶体
            "gtceu:trophy_fragment_dust",          // 残片粉末
            "gtceu:pure_trophy_fragment_dust",     // 干净残片粉末
            "gtceu:trophy_fragment_gem",           // 残片
    };

    /** 造一个带阶段 NBT 的奖杯物品；未充能（阶段 1）额外挂附魔光效。 */
    private static ItemStack trophyStack(int stage) {
        ItemStack stack = new ItemStack(ProofOfHonorModBlocks.CHAMPIONPLATFORM.get());

        // 名字：物品 id 只有一个、lang 键也只有一个，七个阶段靠 lang 分不开 —— 只能写自定义名。
        // 用可翻译组件，才会跟随客户端语言。完成态（阶段 0）也叫「金奖杯」（设计稿的产物名）。
        Component stageName = TrophyBlockEntity.stageName(stage);
        if (stageName != null) {
            stack.setHoverName(stageName);
        }

        CompoundTag beTag = new CompoundTag();
        beTag.putInt(TrophyBlockEntity.TAG_TYPE, stage);
        beTag.putLong(TrophyBlockEntity.TAG_CHARGE, 0L);
        if (ModBlockEntities.TROPHY != null && ModBlockEntities.TROPHY.isPresent()) {
            BlockItem.setBlockEntityData(stack, ModBlockEntities.TROPHY.get(), beTag);
        }

        if (stage == TrophyBlockEntity.STAGE_UNCHARGED) {
            applyUnchargedGlint(stack);
        }
        return stack;
    }

    /**
     * 未充能 = 原版外观 + 附魔光效。1.20.1 没有"只发光效"的 NBT 开关，
     * 惯例是挂一个隐藏的附魔（HideFlags=1 只藏附魔行，不影响光效）。
     */
    private static void applyUnchargedGlint(ItemStack stack) {
        ListTag enchantments = new ListTag();
        CompoundTag unbreaking = new CompoundTag();
        unbreaking.putString("id", "minecraft:unbreaking");
        unbreaking.putShort("lvl", (short) 1);
        enchantments.add(unbreaking);

        CompoundTag tag = stack.getOrCreateTag();
        tag.put("Enchantments", enchantments);
        tag.putInt("HideFlags", 1);
    }
}
