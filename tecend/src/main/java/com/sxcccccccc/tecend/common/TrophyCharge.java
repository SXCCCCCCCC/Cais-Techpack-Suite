package com.sxcccccccc.tecend.common;

import com.sxcccccccc.tecend.config.TecendConfig;
import com.sxcccccccc.tecend.registry.ModBlockEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/**
 * 充能进度（第 15 步）。六个通道各存一份原始进度，共用一根条：
 * 条 = 各路「进度 ÷ 该路配额」之和，满 1 即完成 —— 单走一路能灌满，几路同时灌也累计。
 *
 * <p>进度存在物品 NBT 的 {@code BlockEntityTag} 里，方块形态的 BE 用同一批键存，
 * 所以放下、挖回来都不丢。FE 那个键沿用 {@code charge}：kubejs 侧写的 SNBT 就是它。</p>
 */
public final class TrophyCharge {
    private TrophyCharge() {}

    /** FE 端口（含 RF / J），沿用旧键 */
    public static final String FE = "charge";
    /** GT 充电器（EU，long） */
    public static final String GT = "gtCharge";
    /** IC2 充电板（EU，long） */
    public static final String IC2 = "ic2Charge";
    /** AE2 充电器（AE） */
    public static final String AE = "aeCharge";
    /** IU 电方块（EF） */
    public static final String EF = "efCharge";
    /** Create 应力充气（单位 = 该路配额） */
    public static final String SU = "suCharge";
    /** PneumaticCraft:R 充气站（空气，mL） */
    public static final String PNC = "pncCharge";

    private static final String[] ROUTES = {FE, GT, IC2, AE, EF, SU, PNC};

    public static String[] routes() {
        return ROUTES.clone();
    }

    /** 该通道的配额（config） */
    public static long quota(String route) {
        return switch (route) {
            case FE -> TecendConfig.feChargeTarget();
            case GT -> TecendConfig.gtChargeTarget();
            case IC2 -> TecendConfig.ic2ChargeTarget();
            case AE -> TecendConfig.aeChargeTarget();
            case EF -> TecendConfig.efChargeTarget();
            case SU -> TecendConfig.suChargeTarget();
            case PNC -> TecendConfig.pncChargeTarget();
            default -> throw new IllegalArgumentException("未知充能通道: " + route);
        };
    }

    // ---------------- CompoundTag 层（BE 与物品共用） ----------------

    public static long get(CompoundTag tag, String route) {
        return tag.getLong(route);
    }

    public static long remaining(CompoundTag tag, String route) {
        return Math.max(0L, quota(route) - get(tag, route));
    }

    /**
     * 往某通道灌进度。
     *
     * @return 实际记入的量
     */
    public static long add(CompoundTag tag, String route, long amount) {
        if (amount <= 0) {
            return 0L;
        }
        long accepted = Math.min(amount, remaining(tag, route));
        if (accepted > 0) {
            tag.putLong(route, get(tag, route) + accepted);
        }
        return accepted;
    }

    /** 条的当前进度：各路占配额的比例之和；1 = 满。 */
    public static double fraction(CompoundTag tag) {
        double sum = 0.0D;
        for (String route : ROUTES) {
            sum += (double) get(tag, route) / Math.max(1L, quota(route));
        }
        return sum;
    }

    /** 把六个通道的值从 source 抄进 target */
    public static void copyInto(CompoundTag source, CompoundTag target) {
        for (String route : ROUTES) {
            if (source.contains(route)) {
                target.putLong(route, source.getLong(route));
            }
        }
    }

    // ---------------- ItemStack 层 ----------------

    /**
     * IU 与 PnC 是"自己按物品根 NBT 记账"的：IU 的 ElectricItemManager 写根上的 {@code charge}，
     * PnC 的 AirHandlerItemStack 写 {@code pneumaticcraft:air}。这两条通道直接读写它们自己的键，
     * 别处（放下方块再挖回来）不保证跟着走 —— 那两家本来就只管物品形态。
     */
    private static String nbtKey(String route) {
        return switch (route) {
            case EF -> "charge";
            case PNC -> "pneumaticcraft:air";
            default -> route;
        };
    }

    private static CompoundTag storage(ItemStack stack, String route) {
        return switch (route) {
            case EF, PNC -> stack.getTag();
            default -> BlockItem.getBlockEntityData(stack);
        };
    }

    public static long get(ItemStack stack, String route) {
        CompoundTag tag = storage(stack, route);
        return tag != null ? tag.getLong(nbtKey(route)) : 0L;
    }

    public static void set(ItemStack stack, String route, long value) {
        if (value == get(stack, route)) {
            return;   // 值没变就不写：免得给物品凭空添一个 0 键，把按 NBT 精确匹配的配方顶掉
        }
        switch (route) {
            case EF, PNC -> stack.getOrCreateTag().putLong(nbtKey(route), Math.max(0L, value));
            default -> edit(stack, tag -> tag.putLong(route, Math.max(0L, value)));
        }
    }

    public static long remaining(ItemStack stack, String route) {
        return Math.max(0L, quota(route) - get(stack, route));
    }

    public static long add(ItemStack stack, String route, long amount) {
        if (amount <= 0L) {
            return 0L;
        }
        long accepted = Math.min(amount, remaining(stack, route));
        if (accepted > 0L) {
            set(stack, route, get(stack, route) + accepted);
        }
        return accepted;
    }

    public static double fraction(ItemStack stack) {
        double sum = 0.0D;
        for (String route : ROUTES) {
            sum += (double) get(stack, route) / Math.max(1L, quota(route));
        }
        return sum;
    }

    /** 读改写 {@code BlockEntityTag}：拿副本改，再通过 {@link BlockItem#setBlockEntityData} 放回去。 */
    public static void edit(ItemStack stack, Consumer<CompoundTag> action) {
        if (ModBlockEntities.TROPHY == null || !ModBlockEntities.TROPHY.isPresent()) {
            return;
        }
        CompoundTag tag = BlockItem.getBlockEntityData(stack);
        tag = tag != null ? tag.copy() : new CompoundTag();
        action.accept(tag);
        BlockItem.setBlockEntityData(stack, ModBlockEntities.TROPHY.get(), tag);
    }
}
