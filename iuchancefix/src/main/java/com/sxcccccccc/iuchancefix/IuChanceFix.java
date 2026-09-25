package com.sxcccccccc.iuchancefix;

import net.minecraftforge.fml.common.Mod;

/**
 * 矿物分离器（handler_ho，含电动 6 款 + 蒸汽款）"煤粉做合金"配方的概率输出改造：
 * 含杂煤粉(crafting_498) → 合金用煤粉(crafting_499)，原 chance=75（per-machine
 * 实际 70/75/82/90/95，受 coef 与 95 钳制/减 5 惩罚），AE2 样板因非确定性不可用。
 * <p>
 * 双通道修复：
 * 1. 配方元数据（JEI 及任何配方读口的 percent）→ 100（ChanceFixEvent）；
 * 2. 运行时 machines 的 chance 数组 col → 100（HandlerHoChanceMixin /
 * SteamHandlerHoChanceMixin 在 setRecipeOutput TAIL 强置，绕过 95 钳制/
 * coef/蒸汽 −5）。其余全部 handlerho 配方（矿块/矿物/钙粉等）零改动。
 * <p>
 * 验证：JEI 矿物分离器类别该配方显示 100%；机器每次运行必出 1 个合金用煤粉；
 * 六款电动 + 蒸汽款全部确定性；/reload 不倒退（IU 的 register 静态旗标使其
 * 配方只重建一次，被本 mod 事件跟随重写）。
 */
@Mod(IuChanceFix.MODID)
public class IuChanceFix {

    public static final String MODID = "iuchancefix";

    public IuChanceFix() {
    }

}
