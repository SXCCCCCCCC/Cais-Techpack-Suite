package com.sxcccccccc.tecend.client;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyBlockEntity;
import com.sxcccccccc.tecend.compat.Compat;
import net.mcreator.proofofhonor.init.ProofOfHonorModBlocks;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 物品形态按 NBT 换模型。
 *
 * <p>方块靠 blockstate 的 {@code age} 属性分阶段；物品没有方块状态，走的是模型 {@code overrides}
 * + 一个自定义物品属性 —— 这个属性读的正是物品 NBT 里 {@code BlockEntityTag.type}
 * （挖下来时由战利品表的 copy_nbt 写进去的同一个值），所以物品和方块天然同阶段。</p>
 *
 * <p>注册的物品属性 id 是 {@code tecend:type}，被 kubejs 侧的
 * {@code assets/proof_of_honor/models/item/championplatform.json} 的 overrides 引用。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class TrophyItemProperties {
    private TrophyItemProperties() {}

    /** 物品属性 id：predicate 名写成 tecend:type */
    public static final ResourceLocation TYPE = new ResourceLocation(TecEnd.MOD_ID, "type");

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!Compat.isLoaded(Compat.PROOF_OF_HONOR)) {
            return;
        }
        event.enqueueWork(() -> ItemProperties.register(
                ProofOfHonorModBlocks.CHAMPIONPLATFORM.get().asItem(),
                TYPE,
                (stack, level, entity, seed) -> {
                    CompoundTag tag = BlockItem.getBlockEntityData(stack);
                    return tag != null ? tag.getInt(TrophyBlockEntity.TAG_TYPE) : 0;
                }));
    }
}
