package com.sxcccccccc.tecend.compat;

import com.sxcccccccc.tecend.TecEnd;
import net.minecraftforge.fml.ModList;

/**
 * 上游门禁。上游全是 compileOnly 且 mods.toml 里 mandatory=false，所以引用任何上游类之前
 * 都要先过这里；把上游类只写在 if 分支里即可（Java 类加载是惰性的）：
 *
 * <pre>if (Compat.isLoaded(Compat.GTCEU)) { ... }</pre>
 */
public final class Compat {
    private Compat() {}

    public static final String GTCEU = "gtceu";
    public static final String GTCA = "gtca";
    public static final String GTSE = "gtse";
    public static final String IE = "immersiveengineering";
    public static final String IU = "industrialupgrade";
    public static final String IC2 = "ic2_120";
    public static final String MEK = "mekanism";
    public static final String CREATE = "create";
    public static final String CREATE_OE = "createoreexcavation";
    public static final String IF = "industrialforegoing";
    public static final String THERMAL = "thermal_expansion";
    public static final String PNC = "pneumaticcraft";
    public static final String AE2 = "ae2";
    public static final String MA = "mysticalagriculture";
    public static final String MA_ADDITIONS = "mysticalagradditions";
    public static final String BEES = "productivebees";
    public static final String BEYOND = "beyonddimensions";
    public static final String PYLONS = "pylons";
    public static final String CFB = "cookingforblockheads";
    /** 三档奖杯的方块/模型/材质来源；我们的奖杯 BE 与 mixin 都挂在它身上 */
    public static final String PROOF_OF_HONOR = "proof_of_honor";

    private static final String[] UPSTREAM = {
            GTCEU, GTCA, GTSE, IE, IU, IC2, MEK, CREATE, CREATE_OE, IF,
            THERMAL, PNC, AE2, MA, MA_ADDITIONS, BEES, BEYOND, PYLONS, CFB,
            PROOF_OF_HONOR,
    };

    public static boolean isLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    /** commonSetup 时打一行日志，确认哪些上游没装。 */
    public static void report() {
        StringBuilder missing = new StringBuilder();
        for (String modId : UPSTREAM) {
            if (!isLoaded(modId)) {
                missing.append(' ').append(modId);
            }
        }
        if (missing.length() == 0) {
            TecEnd.LOGGER.info("[compat] 上游全部就位（{} 个）", UPSTREAM.length);
        } else {
            TecEnd.LOGGER.warn("[compat] 缺失上游：{}（相关流程会失效）", missing);
        }
    }
}
