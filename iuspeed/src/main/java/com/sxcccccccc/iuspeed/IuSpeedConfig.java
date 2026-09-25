package com.sxcccccccc.iuspeed;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * iuspeed 配置（config/iuspeed-common.toml，COMMON 型）。
 *
 * <p>只有一项 {@code speed_multiplier}：倍速（默认 5 = 5 倍速，操作时长除以 5；
 * 1 = 关闭）。倍率在各组件/机器类的<b>构造器</b>里被消耗（组件
 * {@code ComponentProcess} 等、自带 operationLength 的机器、ComponentProgress
 * 机器，以及晶体生长室两台 Timer 机器——直接改写构造出的 Timer 时长），
 * 因此改配置后需重新启动游戏（或卸载/重载对应区块让方块实体重建）才生效。
 * <b>高炉除外</b>：{@code BlastFurnaceDurationMixin} 每 tick 实时读取倍率
 * （阈值 3600/倍率 即读即用），改配置瞬时生效，无需重启。
 */
public final class IuSpeedConfig {

    public static final ForgeConfigSpec.IntValue SPEED_MULTIPLIER;

    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        SPEED_MULTIPLIER = builder
                .comment(
                        "IU 机器操作速度倍率：N = N 倍速（operationLength 除以 N）。",
                        "作用类：com.denfop.componets.ComponentProcess（电）、ComponentSteamProcess（蒸汽）、"
                                + "ProcessMultiComponent / BioProcessMultiComponent / SteamProcessMultiComponent（多胞多机）、"
                                + "ComponentProgress（冶炼炉/AutoCrafter）、自带 operationLength 的 45 台机器，"
                                + "以及 Timer 时长机：晶体生长室（原始 3min、GUI 45s，构造时直接缩短为 1/N）、"
                                + "编程台·电（2min → 1/N，v1.3.2）"
                                + "与高炉（BlastFurnaceMain，进度阀值 3600 直接改写为 1/N，GUI 进度条同步换算）。",
                        "只改时间，每 tick 能耗不变。原始机（手动机器：粉碎机/压缩机/晒机等 progress->100 系）不受影响。",
                        "1 = 关闭加速。改配置后需重启游戏生效。"
                )
                .defineInRange("speed_multiplier", 5, 1, 100);
        SPEC = builder.build();
    }

    private IuSpeedConfig() {
    }
}
