package com.sxcccccccc.tecend.mixin;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.implementations.items.IAEItemPowerStorage;
import com.sxcccccccc.tecend.common.TrophyCharge;
import com.sxcccccccc.tecend.common.TrophyItem;
import com.sxcccccccc.tecend.config.TecendConfig;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 把 AE2 的物品电量接口挂到奖杯物品上。AE2 的充电器是硬 {@code instanceof IAEItemPowerStorage}
 * （{@code Platform.isChargeable} + {@code ChargerBlockEntity} 里直接强转），AE2 只注册了
 * MEStorage / ICraftingMachine / GenericInternalInventory 三个能力，所以没有能力通道。
 *
 * <p>速率由物品的 {@link #getChargeRate} 决定，AE2 的充电器按它从网格里取电 —— 默认
 * {@code charge.aeChargeRate} 正好让总量走完要 30 分钟。</p>
 */
@Mixin(value = TrophyItem.class, remap = false)
public abstract class TrophyAeMixin implements IAEItemPowerStorage {

    /** @return 没装进去的量。非「未充能」档什么都不收，免得往 NBT 里写东西 */
    @Override
    public double injectAEPower(ItemStack stack, double amount, Actionable mode) {
        if (!TrophyItem.isChargeable(stack)) {
            return amount;
        }
        if (mode == Actionable.SIMULATE) {
            return amount - Math.min((long) amount, TrophyCharge.remaining(stack, TrophyCharge.AE));
        }
        long accepted = TrophyCharge.add(stack, TrophyCharge.AE, (long) amount);
        TrophyItem.checkCompletion(stack);
        return amount - accepted;
    }

    /** 只进不出 */
    @Override
    public double extractAEPower(ItemStack stack, double amount, Actionable mode) {
        return 0.0D;
    }

    @Override
    public double getAEMaxPower(ItemStack stack) {
        return TecendConfig.aeChargeTarget();
    }

    /** 非「未充能」档报满 */
    @Override
    public double getAECurrentPower(ItemStack stack) {
        return TrophyItem.isChargeable(stack)
                ? TrophyCharge.get(stack, TrophyCharge.AE)
                : TecendConfig.aeChargeTarget();
    }

    /** 只允许网络往里写（{@code isChargeable} 会挡掉 READ） */
    @Override
    public AccessRestriction getPowerFlow(ItemStack stack) {
        return AccessRestriction.WRITE;
    }

    @Override
    public double getChargeRate(ItemStack stack) {
        return TecendConfig.aeChargeRate();
    }
}
