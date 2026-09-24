package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyItem;
import com.sxcccccccc.tecend.compat.Compat;
import net.mcreator.proofofhonor.init.ProofOfHonorModBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

/**
 * 抢注 {@code proof_of_honor:championplatform} 的**物品**，换成我们自己的 {@link TrophyItem}。
 *
 * <p>Forge 的注册表支持覆盖：同名条目后注册者胜出（{@code ForgeRegistry} 的 allowOverrides，
 * 默认开，可用 common config 的 {@code disableOverrides} 关掉）。注册事件按 mod 加载顺序同步派发，
 * 所以 mods.toml 里给 proof_of_honor 标了 {@code ordering="AFTER"}，保证我们在它之后注册。</p>
 *
 * <p>属性照抄上游（默认 + {@code Rarity.RARE}），物品 id 与方块都不变。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TrophyItemOverride {
    private TrophyItemOverride() {}

    public static final ResourceLocation TROPHY_ID =
            new ResourceLocation(Compat.PROOF_OF_HONOR, "championplatform");

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!Compat.isLoaded(Compat.PROOF_OF_HONOR)) {
            return;
        }
        event.register(ForgeRegistries.Keys.ITEMS, TROPHY_ID, () -> new TrophyItem(
                ProofOfHonorModBlocks.CHAMPIONPLATFORM.get(),
                new Item.Properties().rarity(Rarity.RARE)));
    }

    /** 抢注自检：覆盖被关掉时注册表里会留回上游物品，那种情况在这里报出来。 */
    public static void verify() {
        if (!Compat.isLoaded(Compat.PROOF_OF_HONOR)) {
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(TROPHY_ID);
        if (item instanceof TrophyItem) {
            TecEnd.LOGGER.info("[trophy] 已接管 {}", TROPHY_ID);
        } else {
            TecEnd.LOGGER.warn("[trophy] 抢注未生效，{} 是 {}（Forge common config 的 disableOverrides 被打开了？）",
                    TROPHY_ID, item);
        }
    }
}
