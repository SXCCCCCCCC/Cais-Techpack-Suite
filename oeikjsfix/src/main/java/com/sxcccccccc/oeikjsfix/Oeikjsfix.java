package com.sxcccccccc.oeikjsfix;

import net.minecraftforge.fml.common.Mod;

/**
 * OneEnoughItem 1.0.8 的 GUI（游戏内 Replacement Editor / 文件浏览）只读写
 * 世界存档的 datapacks（saves/&lt;world&gt;/datapacks/&lt;name&gt;/data/oei/replacements），
 * 而运行时加载走的是携带 kubejs/data 的 KubeJS 生成数据包（资源包队列最后加入 =
 * 优先级最高）。于是 GUI 编辑器与 kubejs data 是双数据源，且 kubejs 规则总是赢，
 * GUI 保存的规则"改了像没改"。
 *
 * <p>本 mod 是纯路径重定向 mixin：把 GUI 的"文件列表 / 新建 / 保存 / 浏览"全部改指
 * {@code kubejs/data/&lt;domain&gt;/replacements}（domain 来自 OEI 自身的
 * DomainRegistry，当前物品编辑器为 oei），让游戏内编辑器直接编辑运行时生效的
 * KubeJS 数据包，实现单一数据源。不改变任何替换规则的加载、校验、应用逻辑；
 * 不做什么配置开关。
 *
 * <p>依赖与防失效：mods.toml 对 oneenoughitem 声明 mandatory（[1.0.8,)）；
 * 每个 mixin 显式 {@code require = 1}（两目标方法不存在时启动即报 InjectionError，
 * 杜绝静默失效——本包 mixin 铁律）。
 *
 * <p>边界（不做）：WebEditorServer（127.0.0.1 本地 Web 编辑器）自己用
 * {@code Paths.get(String)} 拼路径，不经过 PathUtils，不在本 mod 覆盖范围；
 * GUI 的"提示缓存" .dat 文件与运行时加载完全无关，未触碰。
 */
@Mod("oeikjsfix")
public class Oeikjsfix {

    public Oeikjsfix() {
    }
}
