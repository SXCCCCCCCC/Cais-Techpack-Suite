package com.sxcccccccc.tecend.client;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.compat.Compat;
import me.desht.pneumaticcraft.api.PneumaticRegistry;
import me.desht.pneumaticcraft.api.item.ISpawnerCoreStats;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 刷怪笼核心：装满 100% 猪时换成专属材质（小猪图案）。
 *
 * <p><b>为什么需要这个类</b>：PNC 对刷怪笼核心只有染色、没有换形状的机制 ——
 * {@code SpawnerCoreItem} 实现 {@code ColorHandlers.ITintableItem}，{@code layer1} 的形状
 * 对所有核心是同一张图，tint 只改颜色（色相固定 71/360，饱和度跟填充度，亮度带 20 tick
 * 呼吸动画）。而且 PNC 没给这个物品注册任何 {@code ItemProperties}。所以"按 NBT 换图案"
 * 必须我们自己补一个物品属性，再配一条模型 {@code overrides}。</p>
 *
 * <p>本类注册的属性 id 是 {@code tecend:pig_core}，由 kubejs 侧的
 * {@code assets/pneumaticcraft/models/item/spawner_core.json} 的 overrides 引用
 * （那份文件覆盖 PNC 自带的同名模型，机制与 {@code proof_of_honor} 那套一致）。</p>
 *
 * <p>判据与 KubeJS 第 ⑨ 步的 {@code forge:partial_nbt} 严格对应 —— 那边匹配
 * {@code {"pneumaticcraft:SpawnerCoreStats":{"minecraft:pig":100}}}，这边用 PNC 官方 API
 * 做同一个判断：不留任何污染物、且正好是猪、且占满。两处对得上，才不会出现
 * "能炼金猪但图标不换"的错位。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpawnerCoreItemProperties {
    private SpawnerCoreItemProperties() {}

    /** 物品属性 id：predicate 名写成 tecend:pig_core */
    public static final ResourceLocation PIG_CORE = new ResourceLocation(TecEnd.MOD_ID, "pig_core");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!Compat.isLoaded(Compat.PNC)) {
            return;
        }
        Item spawnerCore = ForgeRegistries.ITEMS.getValue(new ResourceLocation(Compat.PNC, "spawner_core"));
        if (spawnerCore == null) {
            return;
        }
        event.enqueueWork(() -> ItemProperties.register(spawnerCore, PIG_CORE,
                (stack, level, entity, seed) -> isFullOfPig(stack) ? 1.0F : 0.0F));
    }

    /**
     * 判据：没剩空间、只有一种生物、且那一种是猪。
     *
     * <p>{@code getSpawnerCoreStats} 对非刷怪笼核心会抛 IllegalArgumentException，
     * 但这个属性只注册在 spawner_core 上，进来的必然是核心。</p>
     */
    private static boolean isFullOfPig(ItemStack stack) {
        ISpawnerCoreStats stats = PneumaticRegistry.getInstance().getItemRegistry().getSpawnerCoreStats(stack);
        var entities = stats.getEntities();
        return stats.getUnusedPercentage() == 0
                && entities.size() == 1
                && entities.contains(EntityType.PIG);
    }
}
