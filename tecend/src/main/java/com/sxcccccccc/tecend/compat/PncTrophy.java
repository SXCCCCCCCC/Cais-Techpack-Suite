package com.sxcccccccc.tecend.compat;

import com.sxcccccccc.tecend.common.TrophyItem;
import me.desht.pneumaticcraft.api.PNCCapabilities;
import me.desht.pneumaticcraft.api.tileentity.IAirHandlerItem;
import me.desht.pneumaticcraft.common.capabilities.AirHandlerItemStack;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * PnC 集成的门禁层：引用 PnC 类型的代码放这里，调用方先过 {@link Compat#PNC}。
 *
 * <p>充电站的格子认的是物品能力 {@code PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY}，
 * 而那个能力由 PnC 自己的 {@link AirHandlerItemStack} 提供 —— 它的构造函数要求物品实现
 * {@code IPressurizableItem}，那部分由 {@code TrophyPncMixin} 挂在物品类上。</p>
 */
public final class PncTrophy {
    private PncTrophy() {}

    /** 空气能力；不是这个能力、或不是「未充能」那一档，都返回空 */
    @SuppressWarnings("unchecked")
    public static <T> LazyOptional<T> airHandler(ItemStack stack, Capability<T> capability) {
        if (capability == PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY && TrophyItem.isChargeable(stack)) {
            return LazyOptional.of(() -> (T) (IAirHandlerItem) new AirHandlerItemStack(stack));
        }
        return LazyOptional.empty();
    }
}
