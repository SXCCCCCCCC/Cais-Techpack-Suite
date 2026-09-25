package com.sxcccccccc.tecend.compat;

import com.sxcccccccc.tecend.TecEnd;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 摘掉「残片」这一族物品的**炼药锅清洗逻辑**。
 *
 * <p>它不是配方，是 GT 自己注册的物品行为：{@code GTMaterialItems.purifyMap} 定义"哪个前缀洗成哪个"，
 * {@code GTItems.cauldronInteraction(Item)} 把回调注册进<b>原版</b>的 {@code CauldronInteraction.WATER}
 * （javap 实证：那个 lambda 的签名是 {@code (BlockState, Level, BlockPos, Player, InteractionHand, ItemStack)}）。
 * 所以删配方拦不住它（配方删除已做，见 kubejs 侧脚本）。</p>
 *
 * <p>做法：原版炼药锅的 {@code use} 之前 Forge 会先派发 {@link PlayerInteractEvent.RightClickBlock}，
 * 在这里把方块交互 DENY 掉，那个回调就永远不会被调用 —— 零 mixin、不碰 GT 也不碰原版类。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FragmentCauldronDisable {
    private FragmentCauldronDisable() {}

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) {
            return; // 只在服务端拦，避免两端判断不一致
        }
        ItemStack stack = event.getItemStack();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getPath().contains("trophy_fragment")) {
            return;
        }
        BlockState state = event.getLevel().getBlockState(event.getPos());
        if (state.getBlock() instanceof AbstractCauldronBlock) {
            event.setUseBlock(Event.Result.DENY);
            event.setCanceled(true);
        }
    }
}
