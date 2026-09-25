package com.sxcccccccc.gtjeimbfix;

import net.minecraftforge.fml.common.Mod;

/**
 * GTCEu 7.5.3 多方块信息页（JEI {@code gtceu:multiblock_info} 分类）悬停报错修复
 * （1.20.1-Tec 包自用）。
 *
 * <p>崩溃：{@code IndexOutOfBoundsException: Index 17 out of bounds for length 17}，
 * 发生于 JEI 渲染 {@code MultiblockInfoCategory$1ProxyRecipeWidget.getSlotUnderMouse}
 * （2026-09-08 实测 electric_blast_furnace 页，三次同型）。
 *
 * <p>根因：LDLib {@code ModularUIRecipeCategory.setRecipe} 给每个物品槽起名
 * {@code "slot_<i>"}，i = 该 widget 构建期在扁平 widget 列表中的下标；
 * {@code getSlotUnderMouse} 渲染期把槽名 substring(5) 反解回下标再去
 * {@code modularUI.getFlatWidgetCollection().get(下标)} —— 而
 * {@code PatternPreviewWidget} 的 widget 树可变（点 3D 结构预览会经
 * {@code onPosSelected} 增删候选槽、{@code setPage} 整体重建部件槽），
 * 构建期烤死的下标过期 → 越界。
 *
 * <p>修法：保留 7.5.3 原始语义（面板守卫 + 位置同步 + isMouseOver 命中检测），
 * 仅把会崩的"槽名反查下标"替换为按序遍历映射（{@code val$slots[i]} ↔ 扁平列表
 * 第 i 个 IRecipeIngredientSlot widget）。
 *
 * <p>上游 8.x 的"恒返回第一槽"修法（旧实现整体注释掉，见 8.0.0 源码
 * MultiblockInfoJeiCategory.java:72-92）在本包实测导致页面交互全灭（旋转/缩放/
 * 点选）：JEI 配料交互管线以 getSlotUnderMouse 返回值为唯一判定，恒报"有槽"时
 * 整个页面的鼠标事件被当作配料交互消费，LDLib 的 guiEventListener（widget 树
 * 唯一输入通道）收不到任何事件——故采用按序映射方案，行为与原版一致且不崩。
 *
 * <p>见 {@link com.sxcccccccc.gtjeimbfix.mixin.MultiblockInfoSlotFixMixin}。
 */
@Mod("gtjeimbfix")
public class GtJeiMbFix {
}
