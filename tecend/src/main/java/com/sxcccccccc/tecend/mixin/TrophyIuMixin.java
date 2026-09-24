package com.sxcccccccc.tecend.mixin;

import com.denfop.api.item.energy.EnergyItem;
import com.sxcccccccc.tecend.common.TrophyCharge;
import com.sxcccccccc.tecend.common.TrophyItem;
import com.sxcccccccc.tecend.config.TecendConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 把 IU 的物品电量接口挂到奖杯物品上。IU 的格子过滤器和充电机（{@code BlockEntityElectricBlock}、
 * {@code InventoryCharge}）全是 {@code instanceof EnergyItem}，放不进去也充不了。
 *
 * <p>IU 自己那套记账走 {@code ElectricItem.manager}：它读物品根 NBT 的 {@code charge}、
 * 上限走这里的 {@link #getMaxEnergy}。所以奖杯的 IU 通道直接读根上那个键（见
 * {@code TrophyCharge.nbtKey}），不用我们再搬一次。</p>
 *
 * <p>IU 往根 NBT 写电时不会经过我们的代码，所以借它每次问上限的时机看一眼整套条满没满。</p>
 */
@Mixin(value = TrophyItem.class, remap = false)
public abstract class TrophyIuMixin implements EnergyItem {

    /** IU 的档位：给顶档，别让充电板挑档 */
    private static final short TIER = 11;
    /** 量子 MFSU 充电板的速率 */
    private static final double TRANSFER = 33_554_432.0D;

    /** 不是电池，不对外放电 */
    @Override
    public boolean canProvideEnergy(ItemStack stack) {
        return false;
    }

    @Override
    public double getMaxEnergy(ItemStack stack) {
        if (!TrophyItem.isChargeable(stack)) {
            // 非「未充能」档：上限给成当前值，IU 的充电就得不到空间，也就不会写它那个根 NBT 键
            return TrophyCharge.get(stack, TrophyCharge.EF);
        }
        TrophyItem.checkCompletion(stack);
        return TecendConfig.efChargeTarget();
    }

    @Override
    public short getTierItem(ItemStack stack) {
        return TIER;
    }

    @Override
    public double getTransferEnergy(ItemStack stack) {
        return TrophyItem.isChargeable(stack) ? TRANSFER : 0.0D;
    }
}
