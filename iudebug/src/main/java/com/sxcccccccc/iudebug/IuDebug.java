package com.sxcccccccc.iudebug;

import com.sxcccccccc.iudebug.config.IudebugConfig;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/**
 * IU（Industrial Upgrade 3.4.0.10）破坏性调试工具。
 *
 * <p>功能1：注册阶段取消 IU 注册项。配置 {@code config/iudebug-common.toml} 的
 * {@code banned_registrations} 平铺列表（namespace:path 格式，默认空 = 完全无操作），
 * 列表中的 id 在 Forge 注册事件（RegisterEvent）期间被移除（v1.1.0：纯事件 + 反射
 * 删除 IU 的 DeferredRegister.entries 条目，见 {@code IuRegistrationBlocker}；
 * mods.toml ordering="BEFORE" 保证本 mod 的 RegisterEvent 处理先于 IU 的 addEntries），
 * 不会进入游戏注册表。
 *
 * <p>功能2：配方不完整检查崩溃（见 {@code recipecheck.RecipeCheck}）。配置同文件的
 * {@code [recipecheck]} 段（默认全关）：每次服务端数据加载时机（TagsUpdatedEvent，
 * 照 kjsexportcmd 用反射查 RecipeManager 内部 recipes map 类型区分服务端/客户端）
 * 枚举 Denfop 物品机/流体机配方与原生 RecipeManager 配方，判定输入空/输出空等
 * 不完整情况，发现即抛异常崩溃，crash 报告含完整清单。
 */
@Mod("iudebug")
public class IuDebug {

    public IuDebug() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IudebugConfig.SPEC);
        // 注意：这里【不能】读配置值做快照——Forge 此时还没读配置文件
        // （真正读取在 CONFIG_LOAD 阶段 ModConfigEvent.Loading），构造器里读到的是
        // 默认空列表。mixin 判定走 IudebugConfig.isBanned 实时 get()（2026-08-24 根因修复）。
    }
}
