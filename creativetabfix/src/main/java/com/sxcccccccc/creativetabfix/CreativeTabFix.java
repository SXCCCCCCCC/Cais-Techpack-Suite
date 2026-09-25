package com.sxcccccccc.creativetabfix;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 把「没有进任何创造模式标签页」的物品补进指定标签页（1.20.1-Tec 包自用）。
 *
 * <p><b>症状</b>：GTCA 的物品在 JEI 里搜不到，创造模式里也拿不出来（只能 /give）。
 * 打开 JEI 的 {@code showHiddenIngredients} 只能让它们被搜到，创造模式依旧拿不出来，
 * 所以那不是修法。
 *
 * <p><b>根因</b>（GTCA 2.2.0，逐条对照反编译字节码验证，非推测）：
 * <ol>
 *   <li>GTCA 的 {@code gtca:main} 标签页由 {@code GTRegistrate.defaultCreativeTab}
 *       → {@code createCreativeModeTab} 创建，而该方法结尾只做
 *       {@code return builder.m_257652_()}（{@code m_257652_} = {@code CreativeModeTab.Builder.build}），
 *       <b>从不调用 {@code m_257501_}</b>（{@code m_257501_} = {@code displayItems}）。
 *       GTCA 自己的配置 lambda 也只设了 {@code m_257737_}(icon) 与 {@code m_257941_}(title)。
 *       对比 GTSE：{@code GTSECreativeModeTabs} 明确把
 *       {@code new RegistrateDisplayItemsGenerator("gtse", ...)} 传给了 {@code m_257501_}，所以 GTSE 正常。
 *       → {@code gtca:main} 的 DisplayItemsGenerator 是默认空实现，<b>标签页恒为空</b>。
 *   <li>空标签页连画都不画：{@code CreativeModeTab.m_257497_()} 对 {@code Type.CATEGORY}
 *       返回 {@code !displayItems.isEmpty()}。所以玩家连一个空的 GTCA 页签都看不到。
 *   <li>JEI 的 {@code showHiddenIngredients=false}（默认）含义是「不显示不在创造模式菜单中的
 *       原料」，于是 GTCA 物品在 JEI 里集体消失。
 * </ol>
 *
 * <p><b>副作用澄清</b>：{@code GTRegistrate.accept(...)} 只在注册那一刻 {@code currentTab != null}
 * 时才登记 {@code TAB_LOOKUP}，而各类的 {@code creativeModeTab(...)} 写在类末尾的 {@code static {}}
 * 块里。实际结果是只有 {@code GTCAItems} 的条目漏登记（{@code GTCA.init()} 先加载它，
 * 它的 static 块跑完才轮到 {@code GTCABlocks}/{@code GTCAMachines} 的字段初始化，那时
 * {@code currentTab} 已就位）。但这个差异不影响结论 —— 反正 {@code gtca:main} 没有
 * DisplayItemsGenerator，{@code TAB_LOOKUP} 记了也不会被渲染。
 *
 * <p><b>修法</b>：监听 Forge 的 {@code BuildCreativeModeTabContentsEvent}，把配置里声明的
 * 命名空间下的全部物品 accept 进对应标签页。该事件在 DisplayItemsGenerator 跑完之后、
 * 条目刷进标签页之前触发（见 {@code ForgeHooks.onCreativeModeTabBuildContents}），
 * 因此外部模组补进去的条目会正常渲染。
 * <b>不加 mixin、不引用任何 GTCEu/GTCA 类型</b>，只依赖 Forge 公开事件与原生注册表，
 * 不随上游内部结构变动而失效。
 *
 * <p>另见 {@link CreativeTabFixEvents}（补齐逻辑）、{@link CreativeTabFixConfig}（配置）。
 */
@Mod(CreativeTabFix.MOD_ID)
public class CreativeTabFix {

    public static final String MOD_ID = "creativetabfix";
    public static final Logger LOGGER = LoggerFactory.getLogger("creativetabfix");

    public CreativeTabFix() {
        // 构造器里读配置：早于注册表事件，保证标签页开始构建时 mappings 已就绪
        CreativeTabFixConfig.load();
    }
}
