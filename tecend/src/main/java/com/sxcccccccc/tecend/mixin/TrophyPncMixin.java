package com.sxcccccccc.tecend.mixin;

import com.sxcccccccc.tecend.common.TrophyCharge;
import com.sxcccccccc.tecend.common.TrophyItem;
import com.sxcccccccc.tecend.config.TecendConfig;
import me.desht.pneumaticcraft.api.pressure.IPressurizableItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 把 PnC 的"可充压物品"接口挂到奖杯物品上 —— 充电站的格子认能力
 * （{@code PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY}），而那个能力要由 PnC 自己的
 * {@code AirHandlerItemStack} 提供，它的构造函数里有一句
 * {@code Validate.isTrue(container.getItem() instanceof IPressurizableItem)} ——
 * 所以这个接口是必需品，没有它连能力都建不起来（会直接抛异常）。
 *
 * <p>空气存在物品根 NBT 的 {@code pneumaticcraft:air}（PnC 自己的键），奖杯的 PnC 通道就读那里
 * （见 {@code TrophyCharge.nbtKey}）。</p>
 *
 * <p>容积 × 压强上限 = 总容量：36000 mL × 20 bar = 720000 mL。压强上限取 20 bar ——
 * 再高 PnC 的机器会炸。充气站的速率由它自己的加速卡决定（物品侧管不了），
 * 所以容量按"10 张加速卡"定：约 6 分钟；不升速则约 60 分钟。</p>
 */
@Mixin(value = TrophyItem.class, remap = false)
public abstract class TrophyPncMixin implements IPressurizableItem {

    /** 总容量 = 这个 × {@link #getMaxPressure()} */
    private static final int BASE_VOLUME = 36_000;
    /** 20 bar 是安全上限（再高 PnC 的机器会炸） */
    private static final float MAX_PRESSURE = 20.0F;

    @Override
    public int getBaseVolume() {
        return BASE_VOLUME;
    }

    /** 没有容积升级 */
    @Override
    public int getVolumeUpgrades(ItemStack stack) {
        return 0;
    }

    /** 空气量（mL）。PnC 写它自己的键，这里借机看一眼整套条满没满；非「未充能」档报满 */
    @Override
    public int getAir(ItemStack stack) {
        if (!TrophyItem.isChargeable(stack)) {
            return (int) (BASE_VOLUME * MAX_PRESSURE);
        }
        TrophyItem.checkCompletion(stack);
        return (int) Math.min(Integer.MAX_VALUE, TrophyCharge.get(stack, TrophyCharge.PNC));
    }

    @Override
    public float getMaxPressure() {
        return MAX_PRESSURE;
    }
}
