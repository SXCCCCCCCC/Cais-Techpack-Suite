package com.sxcccccccc.tecend.common;

import com.sxcccccccc.tecend.compat.Compat;
import com.sxcccccccc.tecend.compat.PncTrophy;
import com.sxcccccccc.tecend.config.TecendConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

/**
 * 奖杯物品。来源见 {@code registry/TrophyItemOverride}：我们在 RegisterEvent 阶段以同名条目抢注了
 * {@code proof_of_honor:championplatform}。拿回物品类是为了第 15 步充能 —— AE2 的
 * {@code IAEItemPowerStorage}、IC2 的 {@code IBatteryItem}、IU 的 {@code EnergyItem}、
 * GT 的 {@code IElectricItem} 都是 {@code instanceof} 判定，没有能力通道，接口只能长在物品类上。
 *
 * <p>进度按通道分别记在 {@link TrophyCharge} 里（六个键，同一根条），存物品 NBT 的 {@code BlockEntityTag}，
 * 放下 / 挖回来都不丢。</p>
 *
 * <p>FE 端口：{@code IEnergyStorage} 全程 int，装不下总量，所以端口按"格"走 —— 一格
 * {@code charge.feWindow}（默认 2G），装满一格记进总进度、端口从 0 重开；另外卡每刻接收上限
 * {@code charge.feRateLimit}（IF 的 infinity_charger 无速率控制，只能在这一层卡）。</p>
 */
public class TrophyItem extends BlockItem {

    /** 充满后的名字键（与 BE 的完成态同名） */
    private static final String FINISHED_NAME_KEY = "tecend.trophy.finished";

    public TrophyItem(Block block, Properties properties) {
        super(block, properties);
    }

    // ---------------- 阶段 ----------------

    /** 当前阶段。没有 NBT 的物品按完成态算，跟方块状态的默认值一致。 */
    public static int getType(ItemStack stack) {
        CompoundTag tag = getBlockEntityData(stack);
        return tag != null && tag.contains(TrophyBlockEntity.TAG_TYPE)
                ? tag.getInt(TrophyBlockEntity.TAG_TYPE)
                : TrophyBlockEntity.STAGE_FINISHED;
    }

    /** 只有「金奖杯（未充能）」那一档能充。 */
    public static boolean isChargeable(ItemStack stack) {
        return getType(stack) == TrophyBlockEntity.STAGE_UNCHARGED;
    }

    // ---------------- 进度 ----------------

    /** FE 通道已积累的量 */
    public static long getCharge(ItemStack stack) {
        return TrophyCharge.get(stack, TrophyCharge.FE);
    }

    /**
     * 往某个通道灌进度；整套条满 1 就升成完成态。
     *
     * @return 实际记入的量
     */
    public static long addCharge(ItemStack stack, String route, long amount) {
        if (amount <= 0 || !isChargeable(stack)) {
            return 0L;
        }
        long accepted = TrophyCharge.add(stack, route, amount);
        if (accepted > 0 && TrophyCharge.fraction(stack) >= 1.0D) {
            finish(stack);
        }
        return accepted;
    }

    /**
     * 任何一路灌进来之后都调它：整套条满了就升成完成态。
     *
     * <p>判定口径跟 tooltip 的显示一致（四舍五入到 100%）：各路配额是整数、几个通道混合灌的时候
     * 最后那零点几个百分点可能永远凑不齐，用 1.0 严格判定会卡在"显示 100% 却不升级"。</p>
     */
    public static void checkCompletion(ItemStack stack) {
        if (isChargeable(stack) && TrophyCharge.fraction(stack) >= 0.995D) {
            finish(stack);
        }
    }

    /**
     * 充满：变成完成态（金奖杯）—— 阶段写回 NBT、各路通道清零、名字换成完成态、
     * 并去掉未充能那档挂的附魔光效（附魔 + HideFlags）。
     */
    private static void finish(ItemStack stack) {
        for (String route : TrophyCharge.routes()) {
            TrophyCharge.set(stack, route, 0L);
        }
        TrophyCharge.edit(stack, tag -> tag.putInt(TrophyBlockEntity.TAG_TYPE, TrophyBlockEntity.STAGE_FINISHED));
        CompoundTag tag = stack.getOrCreateTag();
        CompoundTag display = tag.getCompound("display");
        display.putString("Name", Component.Serializer.toJson(Component.translatable(FINISHED_NAME_KEY)));
        tag.put("display", display);
        tag.remove("Enchantments");
        tag.remove("HideFlags");
    }

    // ---------------- FE 端口 ----------------

    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new Provider(stack);
    }

    /** 每个物品栈一个 provider，能力对象建一次复用。 */
    private static final class Provider implements ICapabilityProvider {
        private final ItemStack stack;
        private final EnergyStorage storage;

        Provider(ItemStack stack) {
            this.stack = stack;
            this.storage = new EnergyStorage(stack);
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
            // 只有「金奖杯（未充能）」那一档对外暴露能量能力。别的档位一旦被机器写进 NBT，
            // kubejs 那些按 NBT 精确匹配的配方（forge:nbt 是整棵树比对）就全对不上了。
            if (!isChargeable(stack)) {
                return LazyOptional.empty();
            }
            if (capability == ForgeCapabilities.ENERGY) {
                return LazyOptional.of(() -> storage).cast();
            }
            // PnC 的空气能力。引用 PnC 类型的代码在 PncTrophy 里，只有装了 PnC 才会解析那个类。
            if (Compat.isLoaded(Compat.PNC)) {
                return PncTrophy.airHandler(stack, capability);
            }
            return LazyOptional.empty();
        }
    }

    /** 只进不出的 FE 存储。一次只装一格，装满记进总进度、从 0 重开一格；另有每刻接收上限。 */
    private static final class EnergyStorage implements IEnergyStorage {
        private final ItemStack stack;
        /** 速率限制用的分桶：按 50ms 近似"一刻"，累计本桶已收的量 */
        private long rateBucket = Long.MIN_VALUE;
        private long givenInBucket;

        EnergyStorage(ItemStack stack) {
            this.stack = stack;
        }

        /** 当前这一格已装的量：FE 通道进度除以格宽取余。 */
        private int stored() {
            return (int) (getCharge(stack) % TecendConfig.feWindow());
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0) {
                return 0;
            }
            int room = Math.min(maxReceive, TecendConfig.feWindow() - stored());
            room = (int) Math.min(room, TrophyCharge.remaining(stack, TrophyCharge.FE));
            if (room <= 0) {
                return 0;
            }
            long now = System.currentTimeMillis() / 50L;
            if (now != rateBucket) {
                rateBucket = now;
                givenInBucket = 0L;
            }
            int accepted = (int) Math.min(room, Math.max(0L, TecendConfig.feRateLimit() - givenInBucket));
            if (accepted <= 0) {
                return 0;
            }
            if (simulate) {
                return accepted;
            }
            long recorded = addCharge(stack, TrophyCharge.FE, accepted);
            givenInBucket += recorded;
            return (int) Math.min(recorded, Integer.MAX_VALUE);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return TecendConfig.feWindow();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return isChargeable(stack);
        }
    }
}
