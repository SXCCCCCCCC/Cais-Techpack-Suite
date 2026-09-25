package com.sxcccccccc.iufix.mixin;

import com.denfop.items.ItemStackUpgradeModules;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 空锚 mixin：只为让 {@code IufixTransformPlugin.preApply} 在
 * {@code ItemStackUpgradeModules} 类上触发（preApply 仅对有 mixin 应用的
 * 目标类调用）。真正的字节码修复（构造器 checkcast CapabilityFluidHandlerItem
 * → IFluidHandlerItem）由 plugin 的 ASM 完成——构造器内部 mixin 三重手段均被禁
 * （@Inject 仅 RETURN、@Redirect 禁构造器目标、@Overwrite 构造器被 AP 拒绝，
 * 1.9.7 实崩复盘），用户裁决走 ASM。
 */
@Mixin(value = ItemStackUpgradeModules.class, remap = false)
public abstract class ItemStackUpgradeModulesCtorFixMixin {
}
