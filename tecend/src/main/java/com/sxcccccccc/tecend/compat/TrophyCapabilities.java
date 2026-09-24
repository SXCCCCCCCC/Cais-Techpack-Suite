package com.sxcccccccc.tecend.compat;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 往奖杯物品上挂各家的物品能力（目前只有 GT 的电动物品）。
 *
 * <p>这个类本身不引用任何上游类型 —— 真正引用的那段在 {@link GtTrophy} 里，
 * 只有 GT 在场时才会被执行到、也才会解析那个类。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrophyCapabilities {
    private TrophyCapabilities() {}

    @SubscribeEvent
    public static void onAttach(AttachCapabilitiesEvent<ItemStack> event) {
        if (!Compat.isLoaded(Compat.GTCEU) || !(event.getObject().getItem() instanceof TrophyItem)) {
            return;
        }
        GtTrophy.onAttach(event);
    }
}
