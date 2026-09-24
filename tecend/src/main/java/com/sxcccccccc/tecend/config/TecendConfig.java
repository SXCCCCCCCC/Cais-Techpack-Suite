package com.sxcccccccc.tecend.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 充能数值（第 15 步）。六个通道各一份配额，数值按「各自充电机跑 30 分钟」推出来
 * （36,000 刻；速率表见 docs/充能调研.md）。
 *
 * <p>FE 那个通道另外两个参数是必需的：{@code feWindow} 因为 Forge Energy 全程 int、
 * 一次装不下总量；{@code feRateLimit} 因为 IF 的 infinity_charger 每刻把整块储量灌进来、
 * 而它的槽位过滤器只看 FE capability 不认 tag，速率只能在物品侧卡。</p>
 */
public final class TecendConfig {
    private TecendConfig() {}

    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.LongValue FE_TARGET;
    private static final ForgeConfigSpec.IntValue FE_WINDOW;
    private static final ForgeConfigSpec.IntValue FE_RATE_LIMIT;
    private static final ForgeConfigSpec.LongValue GT_TARGET;
    private static final ForgeConfigSpec.LongValue IC2_TARGET;
    private static final ForgeConfigSpec.LongValue AE_TARGET;
    private static final ForgeConfigSpec.IntValue AE_CHARGE_RATE;
    private static final ForgeConfigSpec.LongValue EF_TARGET;
    private static final ForgeConfigSpec.LongValue SU_TARGET;
    private static final ForgeConfigSpec.LongValue PNC_TARGET;
    private static final ForgeConfigSpec.DoubleValue QUARRY_FRAGMENT_CHANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("charge");

        FE_TARGET = builder.comment("FE 端口总量（Mek chargepad 409,600 FE/刻 × 36,000 刻）")
                .defineInRange("feTarget", 14_700_000_000L, 1L, Long.MAX_VALUE);
        FE_WINDOW = builder.comment("FE 端口一次能装的量（int 上限内，装满一格记进总量、端口从 0 重开）")
                .defineInRange("feWindow", 2_000_000_000, 1, Integer.MAX_VALUE);
        FE_RATE_LIMIT = builder.comment("FE 端口每刻接收上限（卡住没有速率控制的充电器）")
                .defineInRange("feRateLimit", 409_600, 1, Integer.MAX_VALUE);

        GT_TARGET = builder.comment("GT 通道总量（uv_charger_4x 2,097,152 EU/刻 × 36,000 刻）")
                .defineInRange("gtTarget", 75_500_000_000L, 1L, Long.MAX_VALUE);
        IC2_TARGET = builder.comment("IC2 通道总量（mfsu_chargepad 2,048 EU/刻 × 36,000 刻）")
                .defineInRange("ic2Target", 73_700_000L, 1L, Long.MAX_VALUE);
        AE_TARGET = builder.comment("AE 通道总量")
                .defineInRange("aeTarget", 7_350_000_000L, 1L, Long.MAX_VALUE);
        AE_CHARGE_RATE = builder.comment("AE 通道速率（AE2 的充电器按物品的 getChargeRate 给电）")
                .defineInRange("aeChargeRate", 204_167, 1, Integer.MAX_VALUE);
        EF_TARGET = builder.comment("IU 通道总量（取设计稿的 25G；量子 MFSU 充电板 33,554,432 EF/刻 下约 12 分钟）")
                .defineInRange("efTarget", 25_000_000_000L, 1L, Long.MAX_VALUE);
        SU_TARGET = builder.comment("Create 通道总值（应力充气）。满速 256 RPM 时每刻灌 总值/36000，",
                        "所以 360000 正好是 30 分钟；转速低于一半按一半算，时间最多翻倍。")
                .defineInRange("suTarget", 360_000L, 1L, Long.MAX_VALUE);
        PNC_TARGET = builder.comment("PnC 通道总值（空气，mL），= 容器容积 × 压强上限 = 36000 mL × 20 bar。",
                        "压强上限取 20 bar（再高 PnC 的机器会炸）；速率由充气站自己的加速卡决定，物品侧管不了，",
                        "所以容量按 10 张加速卡定 —— 约 6 分钟；不升速约 60 分钟。")
                .defineInRange("pncTarget", 720_000L, 1L, Long.MAX_VALUE);

        builder.pop();

        builder.push("quarry");
        QUARRY_FRAGMENT_CHANCE = builder.comment("第 1 步「从虚空中来」：IU 量子采石场每产出一个物品时，",
                        "追加一块远古奖杯残片的概率。按「产出一次」计，不是每刻。")
                .defineInRange("fragmentChance", 0.001D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    public static long feChargeTarget() {
        return FE_TARGET.get();
    }

    public static int feWindow() {
        return FE_WINDOW.get();
    }

    public static int feRateLimit() {
        return FE_RATE_LIMIT.get();
    }

    public static long gtChargeTarget() {
        return GT_TARGET.get();
    }

    public static long ic2ChargeTarget() {
        return IC2_TARGET.get();
    }

    public static long aeChargeTarget() {
        return AE_TARGET.get();
    }

    public static int aeChargeRate() {
        return AE_CHARGE_RATE.get();
    }

    public static long efChargeTarget() {
        return EF_TARGET.get();
    }

    public static long suChargeTarget() {
        return SU_TARGET.get();
    }

    public static long pncChargeTarget() {
        return PNC_TARGET.get();
    }

    public static double quarryFragmentChance() {
        return QUARRY_FRAGMENT_CHANCE.get();
    }
}
