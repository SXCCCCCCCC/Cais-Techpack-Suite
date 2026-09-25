package com.sxcccccccc.tecend.mixin;

import com.sxcccccccc.tecend.common.TrophyCharge;
import com.sxcccccccc.tecend.common.TrophyItem;
import com.sxcccccccc.tecend.config.TecendConfig;
import ic2_120.content.item.energy.IBatteryItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 把 IC2 的电池接口挂到奖杯物品上。IC2 的充电机（{@code BatteryChargerComponent}）是硬
 * {@code instanceof IBatteryItem} / {@code instanceof IElectricTool}，不认 Forge 能力也不查
 * TR Energy 的物品 lookup，所以接口只能长在物品类上 —— 而物品类已经是我们抢注来的
 * {@link TrophyItem}，混进来即可。
 *
 * <p>{@code charge}/{@code discharge}/{@code isFullyCharged}/{@code getChargeRatio} 在接口里是默认实现，
 * 只要给全下面五个就行。{@link #getTier()} 取 MFSU 档（4）：IC2 的充电板按档位收物品，
 * 给顶档所有板都肯收，速率也正是 2048 EU/刻 —— 跟 ic2Target 的 30 分钟口径一致。</p>
 */
@Mixin(value = TrophyItem.class, remap = false)
public abstract class TrophyIc2Mixin implements IBatteryItem {

    /** IC2 的档位：1=BatBox 2=CESU 3=MFE 4=MFSU */
    private static final int TIER = 4;

    @Override
    public long getMaxCapacity() {
        return TecendConfig.ic2ChargeTarget();
    }

    @Override
    public int getTier() {
        return TIER;
    }

    /** 站在充电板上（不是放进格子）也肯收 */
    @Override
    public boolean getCanChargeWireless() {
        return true;
    }

    /** 非「未充能」档一律报满：IC2 的 {@code charge()} 就算不出可充空间，一个字节都不会写进 NBT */
    @Override
    public long getCurrentCharge(ItemStack stack) {
        return TrophyItem.isChargeable(stack)
                ? TrophyCharge.get(stack, TrophyCharge.IC2)
                : getMaxCapacity();
    }

    @Override
    public void setCurrentCharge(ItemStack stack, long amount) {
        if (!TrophyItem.isChargeable(stack)) {
            return;
        }
        TrophyCharge.set(stack, TrophyCharge.IC2, amount);
        TrophyItem.checkCompletion(stack);
    }
}
