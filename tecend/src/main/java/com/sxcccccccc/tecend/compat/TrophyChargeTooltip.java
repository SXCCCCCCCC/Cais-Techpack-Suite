package com.sxcccccccc.tecend.compat;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyCharge;
import com.sxcccccccc.tecend.common.TrophyItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 「金奖杯（未充能）」的 tooltip：第 15 步的总条 + 各通道自己的进度。
 *
 * <p>用 Forge 的 {@link ItemTooltipEvent}（forge 总线），跟 {@link FragmentTooltipFilter} 一个路子。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TrophyChargeTooltip {
    private TrophyChargeTooltip() {}

    /** 进度条的格数 */
    private static final int CELLS = 20;

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!TrophyItem.isChargeable(stack)) {
            return;
        }
        double fraction = Math.min(1.0D, TrophyCharge.fraction(stack));
        event.getToolTip().add(Component.translatable("tecend.trophy.charge", (int) Math.round(fraction * 100.0D)));
        event.getToolTip().add(bar((int) Math.round(fraction * CELLS)));

        for (String route : TrophyCharge.routes()) {
            long value = TrophyCharge.get(stack, route);
            if (value <= 0L) {
                continue;
            }
            int percent = (int) Math.min(100L, Math.round(value * 100.0D / Math.max(1L, TrophyCharge.quota(route))));
            event.getToolTip().add(Component.translatable(langKey(route), percent).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static MutableComponent bar(int filled) {
        MutableComponent bar = Component.literal("[");
        bar.append(Component.literal("|".repeat(filled)).withStyle(ChatFormatting.GREEN));
        bar.append(Component.literal("|".repeat(CELLS - filled)).withStyle(ChatFormatting.DARK_GRAY));
        return bar.append(Component.literal("]"));
    }

    private static String langKey(String route) {
        return switch (route) {
            case TrophyCharge.FE -> "tecend.trophy.route.fe";
            case TrophyCharge.GT -> "tecend.trophy.route.gt";
            case TrophyCharge.IC2 -> "tecend.trophy.route.ic2";
            case TrophyCharge.AE -> "tecend.trophy.route.ae";
            case TrophyCharge.EF -> "tecend.trophy.route.ef";
            case TrophyCharge.SU -> "tecend.trophy.route.su";
            case TrophyCharge.PNC -> "tecend.trophy.route.pnc";
            default -> throw new IllegalArgumentException("未知充能通道: " + route);
        };
    }
}
