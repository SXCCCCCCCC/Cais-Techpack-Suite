package com.sxcccccccc.macobaltfix;

import net.minecraftforge.fml.common.Mod;

/**
 * MA 作物启用修复（1.20.1-Tec 包自用 Feature fix mod）。
 *
 * <p>包内缺依赖 mod 的 MysticalAgriculture 作物被 {@code ModCrops.onRegisterCrops()}
 * 的 {@code withRequiredMods(...)} 置为 {@code setEnabled(false)}，导致该作物全部配方
 * （种子工作台/灌注/精华 farming 链）加载时被 {@code mysticalagriculture:crop_enabled}
 * 条件丢弃——即使 GTCEu 或原版已提供对应材料。
 *
 * <p>修复（v1.1.0）：mixin 在 {@code withRequiredMods} 入口短路以下作物，保持构造默认
 * enabled=true，行为与无条件作物（如 IRIDIUM）完全一致：
 *
 * <ul>
 *   <li>MA 主包：COBALT（原 gate: tconstruct）、BASALT（chisel）、MARBLE（chisel/astralsorcery）、
 *       ROSE_GOLD（tconstruct）、URANINITE（powah）、REDSTONE_ALLOY（enderio）</li>
 *   <li>MysticalAgradditions：NEUTRONIUM（avaritia）</li>
 * </ul>
 *
 * <p>材料替换（原材料指向缺失 mod 的物品/tag）：URANINITE→{@code forge:dusts/uraninite}、
 * REDSTONE_ALLOY→{@code gtceu:red_alloy_ingot}、NEUTRONIUM→{@code gtceu:neutronium_ingot}。
 *
 * <p>补充（kubejs/server_scripts/mysticalagriculture_crops.js）：原版 essence→材料配方
 * 在缺失 mod 的专属目录（essence/tconstruct|chisel|powah|enderio... ），带
 * {@code forge:mod_loaded xxx} 条件永远不加载，由 KubeJS 按 MA 原版配方比例与形状
 * 逐一补齐（输出物品换为包内材料）；并补 chisel:basalt /
 * mysticalagriculture:material/marble / mysticalagradditions:neutronium_ingot
 * 三个材料 tag 的包内内容（种子配方对 tag 有写死的 tag_empty 检查）。
 *
 * <p>其余 mod 门作物（thermal/redstone_arsenal/immersiveengineering/enderio 等）不受影响。
 */
@Mod("macobaltfix")
public class MaCobaltFix {
}
