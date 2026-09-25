package com.sxcccccccc.oeffix;

import net.minecraftforge.fml.common.Mod;

/**
 * OneEnoughFluid 1.1.1-hotfix 的单个 NPE 修复（用户裁决：只修这一个，不节外生枝）。
 *
 * <p>背景（代码层面研究结论）：oelib DataManager 在资源重载时调用
 * {@code FluidReplacementCache.beginReloadOverride(currentFluidMap)}。OEF 原实现
 * 是"先打日志后赋值"：
 * {@code LOGGER.info(..., ReloadOverrideFluidMap.size())} 在读日志参数时
 * {@code ReloadOverrideFluidMap} 仍为 null（尚未 putstatic），当 map 非空、
 * 方法走到 else 分支即 NPE（空 map 时早退路径不触发）。同一函数 OEI 的
 * {@code ItemReplacementCache.beginReloadOverride} 是"先赋值后打日志"，无此问题；
 * 但 OEI 的 mixin 因 "recipe" 单复数笔误从不触发，故此前从未暴露。
 *
 * <p>修复：HEAD 注入 + cancel 整个重写 beginReloadOverride，按 OEI 语义
 * （先 {@code ReloadOverrideFluidMap = new HashMap<>(currentFluidMap)} 再打日志，
 * 日志读到的 size() 是新值）。注入点/描述符按 javap 反编译证据精确写：
 * 方法名 {@code beginReloadOverride} + 描述符 {@code (Ljava/util/Map;)V}，
 * remap=false（mod 类保持运行时名），require=1 防静默失效。
 */
@Mod("oeffix")
public class Oeffix {

    public Oeffix() {
    }
}
