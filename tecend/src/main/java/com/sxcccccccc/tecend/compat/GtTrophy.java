package com.sxcccccccc.tecend.compat;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.GTValues;
import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyCharge;
import com.sxcccccccc.tecend.common.TrophyItem;
import com.sxcccccccc.tecend.config.TecendConfig;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import javax.annotation.Nullable;

/**
 * GT 集成的门禁层：引用 GT 类型的代码放这里，调用方先过 {@link Compat#GTCEU}。
 *
 * <p>GT 7.5.3 的电池箱是按<b>物品能力</b>找充电目标的（{@code getElectricItem(ItemStack)} /
 * {@code getForgeEnergyItem(ItemStack)}，javap 实证 —— 不是 8.0.0 源码里那种对 Item 的 instanceof），
 * 所以挂一份 {@code GTCapability.CAPABILITY_ELECTRIC_ITEM} 就能进 EU 那条路。</p>
 *
 * <p>实现照实现指南的骨架，唯一改动是<b>不检查 {@code chargerTier >= tier}</b>：
 * GT 自带的实现带这个门槛，会让所有低档充电器直接返回 0（不是"很慢"）。</p>
 */
public final class GtTrophy {
    private GtTrophy() {}

    /** 档位给 UV：速率天花板 = {@code V[UV] × 4} = 2,097,152 EU/t，跟 gtTarget 的 30 分钟口径一致 */
    private static final int TIER = GTValues.UV;

    /** 给「未充能」那档挂 GT 电动物品能力；别的档位不挂（免得机器往里写 NBT） */
    public static void onAttach(AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (!(stack.getItem() instanceof TrophyItem) || !TrophyItem.isChargeable(stack)) {
            return;
        }
        event.addCapability(new ResourceLocation(TecEnd.MOD_ID, "gt_electric_item"), new Provider(stack));
    }

    private static final class Provider implements ICapabilityProvider {
        private final ElectricItem item;

        Provider(ItemStack stack) {
            this.item = new ElectricItem(stack);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            return capability == GTCapability.CAPABILITY_ELECTRIC_ITEM
                    ? LazyOptional.of(() -> item).cast()
                    : LazyOptional.empty();
        }
    }

    private static final class ElectricItem implements IElectricItem {
        private final ItemStack stack;

        ElectricItem(ItemStack stack) {
            this.stack = stack;
        }

        /** 不是电池，不对外放电 */
        @Override
        public boolean canProvideChargeExternally() {
            return false;
        }

        @Override
        public boolean chargeable() {
            return true;
        }

        @Override
        public long getTransferLimit() {
            return GTValues.V[TIER];
        }

        @Override
        public long getMaxCharge() {
            return TecendConfig.gtChargeTarget();
        }

        @Override
        public int getTier() {
            return TIER;
        }

        @Override
        public long getCharge() {
            return TrophyCharge.get(stack, TrophyCharge.GT);
        }

        @Override
        public long charge(long amount, int chargerTier, boolean ignoreTransferLimit, boolean simulate) {
            if (stack.getCount() != 1) {
                return 0L;
            }
            if (simulate) {
                return Math.min(amount, TrophyCharge.remaining(stack, TrophyCharge.GT));
            }
            long accepted = TrophyCharge.add(stack, TrophyCharge.GT, amount);
            TrophyItem.checkCompletion(stack);
            return accepted;
        }

        @Override
        public long discharge(long amount, int dischargerTier, boolean ignoreTransferLimit, boolean externally,
                              boolean simulate) {
            return 0L;
        }
    }
}
