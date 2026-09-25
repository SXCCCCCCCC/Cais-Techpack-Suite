package com.sxcccccccc.gtjeimbfix.util;

import com.gregtechceu.gtceu.integration.jei.multipage.MultiblockInfoWrapper;
import com.lowdragmc.lowdraglib.gui.ingredient.IRecipeIngredientSlot;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.gui.inputs.RecipeSlotUnderMouse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 反射读取 {@code MultiblockInfoCategory$1ProxyRecipeWidget} 的合成捕获字段
 * {@code val$recipe} / {@code val$slots}，并按 7.5.3 原始语义做槽位命中检测。
 *
 * <p>为什么不用 @Shadow（实踩教训，Mixin AP 0.8.5 潜伏 bug）：
 * <ol>
 *   <li>{@code val$slots} 是匿名类捕获的合成字段，名字带 {@code $}，且目标类里
 *       声明为裸 {@code List}（与带泛型的 shadow 声明对不上）→ AP 的
 *       {@code validateTargetField} 元素匹配失败，落入别名回退路径
 *       {@code TypeHandle.findField(name, desc)}；</li>
 *   <li>该路径遍历目标类每个字段调 {@code TypeUtils.getJavaSignature(字段描述符)}，
 *       其内部 {@code SignaturePrinter(String, String)} → ASM
 *       {@code Type.getReturnType} 按<b>方法描述符</b>解析（找 {@code ')'}）——
 *       字段描述符没有括号，必然 {@code StringIndexOutOfBoundsException}
 *       （实测：任意字段描述符都炸，异常下标 = 描述符长度；本类首个字段
 *       {@code position} 描述符恰好 52 字符 → "index out of range: 52"）。</li>
 * </ol>
 *
 * <p>本类放 mixin 包外的 {@code .util} 子包（SOP 规则：mixin 包内只许放 mixin 类；
 * handler 引用 mixin 类自身非常量静态成员会 IllegalClassLoadError——命中检测逻辑
 * 全部放这里，mixin 里只留一行转发）。
 */
public final class ProxySlotsAccessor {

    private static volatile Field SLOTS_FIELD;
    private static volatile Field RECIPE_FIELD;

    private ProxySlotsAccessor() {
    }

    /**
     * 7.5.3 原始语义的槽位命中检测，仅把会崩的下标反查替换为按序遍历映射。
     *
     * <p>为什么不用上游 8.x 的"恒返回第一槽"（实踩，交互全灭）：JEI 的配料交互
     * 管线以 {@code getSlotUnderMouse} 的返回值为唯一判定——代理恒报"有槽"时，
     * JEI 把整个页面的按下/拖拽/滚动全部当作配料交互消费掉，LDLib 的
     * {@code ModularUIGuiEventListener}（页面 widget 树唯一输入通道，SceneWidget
     * 的旋转/缩放/点选、按钮全走它）收不到任何事件。必须保留原始契约：
     * <b>只有鼠标真的落在某个槽上才返回槽</b>。
     *
     * <p>映射关系：LDLib {@code setRecipe} 按扁平 widget 列表顺序给每个
     * {@code IRecipeIngredientSlot} 建槽（本页部件槽/候选槽全是 INPUT，1 widget = 1
     * 槽），故 {@code val$slots[i]} ↔ 扁平列表第 i 个 IRecipeIngredientSlot widget。
     * 每次调用实时重排（树可变：点 3D 结构增删候选槽、翻页重建部件槽），候选槽
     * 无对应 JEI 槽（超出 {@code slots.size()} 的部分被守卫跳过）。
     *
     * @return 命中的槽；鼠标不在面板/槽上时返回 empty（JEI 回退原生命中检测并
     *         放行事件给 guiEventListeners——场景交互依赖此放行）
     */
    public static Optional<RecipeSlotUnderMouse> getSlotUnderMouse(Object proxyRecipeWidget,
                                                                   double mouseX, double mouseY) {
        MultiblockInfoWrapper wrapper = (MultiblockInfoWrapper) getRecipe(proxyRecipeWidget);
        Widget panel = wrapper.getWidget();
        Position pos = panel.getSelfPosition();
        Size size = panel.getSize();
        if (!Widget.isMouseOver(pos.x, pos.y, size.width, size.height, mouseX, mouseY)) {
            return Optional.empty();
        }

        List<IRecipeIngredientSlot> ordered = new ArrayList<>();
        for (Widget widget : wrapper.modularUI.getFlatWidgetCollection()) {
            if (widget instanceof IRecipeIngredientSlot) {
                ordered.add((IRecipeIngredientSlot) widget);
            }
        }

        List<?> slots = getSlots(proxyRecipeWidget);
        for (int i = 0; i < slots.size() && i < ordered.size(); i++) {
            IRecipeSlotDrawable slot = (IRecipeSlotDrawable) slots.get(i);
            Widget widget = (Widget) ordered.get(i);
            slot.setPosition(widget.getPositionX(), widget.getPositionY());
            if (slot.isMouseOver(mouseX, mouseY)) {
                return Optional.of(new RecipeSlotUnderMouse(slot, 0, 0));
            }
        }
        return Optional.empty();
    }

    /** 取目标实例的 val$slots 列表（Field 懒缓存；失败抛错而非静默，便于暴露问题）。 */
    public static List<?> getSlots(Object proxyRecipeWidget) {
        try {
            Field field = SLOTS_FIELD;
            if (field == null) {
                field = proxyRecipeWidget.getClass().getDeclaredField("val$slots");
                field.setAccessible(true);
                SLOTS_FIELD = field;
            }
            return (List<?>) field.get(proxyRecipeWidget);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("gtjeimbfix: cannot read val$slots of " +
                    proxyRecipeWidget.getClass().getName(), e);
        }
    }

    /** 取目标实例的 val$recipe（MultiblockInfoWrapper，即 ModularWrapper 本体）。 */
    public static Object getRecipe(Object proxyRecipeWidget) {
        try {
            Field field = RECIPE_FIELD;
            if (field == null) {
                field = proxyRecipeWidget.getClass().getDeclaredField("val$recipe");
                field.setAccessible(true);
                RECIPE_FIELD = field;
            }
            return field.get(proxyRecipeWidget);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("gtjeimbfix: cannot read val$recipe of " +
                    proxyRecipeWidget.getClass().getName(), e);
        }
    }
}
