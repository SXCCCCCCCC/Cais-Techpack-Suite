package com.sxcccccccc.gtjeimbfix.mixin;

import com.sxcccccccc.gtjeimbfix.util.ProxySlotsAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * 修 GTCEu 7.5.3 多方块信息页 JEI 悬停越界崩溃（IndexOutOfBoundsException）。
 *
 * <p>目标 = {@code MultiblockInfoCategory.createRecipeExtras} 里的匿名内部类
 * {@code MultiblockInfoCategory$1ProxyRecipeWidget}（javap 核实该名字）。
 * 崩溃路径：其 {@code getSlotUnderMouse} 把 JEI 槽名（LDLib 构建期烤死的
 * {@code "slot_<扁平下标>"}）解析回下标 → {@code modularUI
 * .getFlatWidgetCollection().get(下标)}；而 {@code PatternPreviewWidget} 的
 * widget 树会变（点 3D 结构预览经 {@code onPosSelected} 增删候选槽——
 * 线圈位置候选≈8 个、外壳/舱口位置 1 个，{@code setPage} 整体重建部件槽），
 * 渲染期列表缩水 → 下标越界（实测 17/17：构建期 ≥18 有 slot_17，
 * 渲染期仅剩 17 个 widget）。
 *
 * <p>修法：保留 7.5.3 原始语义（面板守卫 + 位置同步 + isMouseOver 命中检测），
 * 仅把会崩的"槽名反查下标"替换为<b>按序遍历映射</b>（{@code val$slots[i]} ↔
 * 扁平列表第 i 个 IRecipeIngredientSlot widget），逻辑在
 * {@link ProxySlotsAccessor#getSlotUnderMouse(Object, double, double)}。
 *
 * <p>为什么不用上游 8.x 的"恒返回第一槽"（实踩，交互全灭）：JEI 的配料交互
 * 管线以 {@code getSlotUnderMouse} 的返回值为唯一判定——代理恒报"有槽"时，
 * JEI 把整个页面的按下/拖拽/滚动全部当作配料交互消费掉，LDLib 的
 * {@code ModularUIGuiEventListener}（页面 widget 树唯一输入通道，SceneWidget
 * 的旋转/缩放/点选、按钮全走它）收不到任何事件。详见
 * {@link ProxySlotsAccessor}。
 *
 * <p>写法注意（实踩教训）：
 * <ol>
 *   <li>@Inject(HEAD, cancellable) 必须 {@code ci.cancel()} +
 *       {@code ci.setReturnValue(...)} 双写，只 setReturnValue 不 cancel 的话
 *       返回值被忽略、原方法体照跑（Mixin 0.8.5 CallbackInjector 生成
 *       {@code if(ci.isCancelled()) return ci.getReturnValue();}）。</li>
 *   <li>目标类捕获的合成字段 {@code val$slots} 不能用 @Shadow 取——AP 别名
 *       回退路径解析字段描述符必崩（详见 {@link ProxySlotsAccessor}），
 *       改走 mixin 包外工具类反射。</li>
 *   <li>handler 不得引用本 mixin 类的非常量静态成员（IllegalClassLoadError），
 *       需要共享状态一律放 mixin 包外（SOP 规则）。</li>
 * </ol>
 *
 * <p>remap=false：目标为 GT mod 类（不在原版映射集）；handler 不引用原版成员。
 */
@Mixin(targets = "com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoCategory$1ProxyRecipeWidget", remap = false)
public abstract class MultiblockInfoSlotFixMixin {

    @Inject(method = "getSlotUnderMouse", at = @At("HEAD"), cancellable = true, remap = false)
    private void gtjeimbfix$getSlotUnderMouse(double mouseX, double mouseY,
                                              CallbackInfoReturnable<Optional> cir) {
        cir.cancel();
        cir.setReturnValue(ProxySlotsAccessor.getSlotUnderMouse(this, mouseX, mouseY));
    }
}
