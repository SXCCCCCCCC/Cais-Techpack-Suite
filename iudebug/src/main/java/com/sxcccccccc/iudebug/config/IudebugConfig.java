package com.sxcccccccc.iudebug.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * iudebug 配置（config/iudebug-common.toml）。
 *
 * <p>{@code banned_registrations}：平铺字符串列表，元素为完整注册 id，格式
 * {@code namespace:path}（如 {@code industrialupgrade:tools/treetap}）。默认空列表
 * = 完全无操作。修改后需重启游戏生效（Forge COMMON 配置不做运行时热重载）。
 *
 * <p><b>取值时机铁律（2026-08-24 实测根因修复）</b>：Forge 在 mod 构造器
 * （{@code ModLoadingContext.registerConfig}）时<b>不读文件</b>，真正读文件发生在
 * CONFIG_LOAD 阶段（ModConfigEvent.Loading）；而注册事件 RegisterEvent 在 REGISTRY
 * 阶段（CONFIG_LOAD 之后）触发。因此 {@link #isBanned} 必须<b>每次实时
 * {@code BANNED_REGISTRATIONS.get()}</b>，绝不能做成"构造器一次性快照"——旧实现的
 * 静态快照集合在文件加载前就被填成默认空列表，之后永不更新，导致 mixin 0 命中
 * （用户实测：配置 60 条但流体全部注册成功，latest.log 只有
 * {@code banned_registrations = []}）。配置加载完成的诊断打印挂在
 * {@link #onConfigLoad(ModConfigEvent.Loading)}（CONFIG_LOAD 阶段，读到的是真实文件值）。
 */
@Mod.EventBusSubscriber(modid = "iudebug", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IudebugConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(IudebugConfig.class);

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_REGISTRATIONS;

    // ---- [recipecheck] 段（功能2：配方不完整检查崩溃） ----
    public static final ForgeConfigSpec.ConfigValue<Boolean> RECIPE_CHECK_ENABLED;
    public static final ForgeConfigSpec.ConfigValue<Boolean> RECIPE_CHECK_INCLUDE_DENFOP;
    public static final ForgeConfigSpec.ConfigValue<Boolean> RECIPE_CHECK_INCLUDE_VANILLA;
    public static final ForgeConfigSpec.ConfigValue<Boolean> RECIPE_CHECK_INCLUDE_VANILLA_SPECIAL;
    public static final ForgeConfigSpec.ConfigValue<Boolean> RECIPE_CHECK_INCLUDE_WARN;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RECIPE_CHECK_IGNORED_RECIPE_IDS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        BANNED_REGISTRATIONS = builder
                .comment(
                        "要取消注册的 IU 注册项 id 列表，格式 namespace:path（如 industrialupgrade:tools/treetap）。",
                        "被取消的 id 在游戏注册阶段（RegisterEvent）被跳过，不会进入游戏注册表。",
                        "本 mod 只拦截 industrialupgrade 命名空间的注册（注入点在 IU 注册流程内），",
                        "填写其他命名空间的 id 不会生效。",
                        "注意：取消注册是破坏性操作，被取消对象之后被 .get() 引用会抛",
                        " 'Registry Object not present' NPE（详见 _oei_backup/iu_20260821/iudebug_usage.md）。",
                        "默认空列表 = 完全无操作。修改后需重启游戏生效。"
                )
                .defineList("banned_registrations", List.of(), e -> e instanceof String);
        builder.push("recipecheck");
        RECIPE_CHECK_ENABLED = builder
                .comment(
                        "功能2 总开关。默认 false —— 平时游玩绝不误炸存档。",
                        "开启后：每次服务端数据加载时机（TagsUpdatedEvent，启动 1 次 + 每次",
                        " /reload 1 次；照 kjsexportcmd 用反射查 RecipeManager 内部 recipes map",
                        " 类型区分，可变 HashMap = 服务端执行）做一次检查，发现不完整配方立即",
                        " 抛异常崩溃游戏（crash report 的 Exception Message 含完整清单）。",
                        "KubeJS 异步 tag 竞态已由 ModernFix 修好（2026-08-19 实测生效），",
                        "无需复检/延迟。修改后需重启游戏生效。"
                )
                .define("enabled", false);
        RECIPE_CHECK_INCLUDE_DENFOP = builder
                .comment("线 A：检查 IU（Denfop）物品机 + 流体机配方（主目标）。")
                .define("includeDenfop", true);
        RECIPE_CHECK_INCLUDE_VANILLA = builder
                .comment("线 B：检查原生 RecipeManager 配方（含其他 mod 的数据包配方，",
                        "抓功能1 取消注册在原生配方上的联动破坏）。")
                .define("includeVanilla", true);
        RECIPE_CHECK_INCLUDE_VANILLA_SPECIAL = builder
                .comment("是否检查 isSpecial 配方（默认跳过，减少噪音；特殊配方可能故意无匹配）。")
                .define("includeVanillaSpecial", false);
        RECIPE_CHECK_INCLUDE_WARN = builder
                .comment("空输出列表（D6）是否计入崩溃。默认 false：",
                        "不确定的判定一律不报，宁可漏报不可误炸。")
                .define("includeWarn", false);
        RECIPE_CHECK_IGNORED_RECIPE_IDS = builder
                .comment(
                        "忽略清单：原生线按配方 id 前缀匹配；denfop 线按机器名前缀匹配（如 \"heat\"）。",
                        "白名单式排除误报。"
                )
                .defineList("ignoredRecipeIds", List.of(), e -> e instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private IudebugConfig() {
    }

    /**
     * CONFIG_LOAD 阶段（Forge 真正读完配置文件后）触发：打印实际生效的禁注册列表。
     * 此时 {@code BANNED_REGISTRATIONS.get()} 返回的是文件里的真实值（非默认值），
     * 日志里出现这行且条数/内容与文件一致 = 配置加载正确，可作为验证手段。
     */
    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (!"iudebug".equals(event.getConfig().getModId())) {
            return;
        }
        LOGGER.info("[iudebug] banned_registrations 已加载（{} 条）: {}",
                BANNED_REGISTRATIONS.get().size(), BANNED_REGISTRATIONS.get());
    }

    /**
     * 判定某注册 id 是否在禁注册列表中。
     * 只对 {@code industrialupgrade} 命名空间生效（移除点在 RegisterEvent 阶段的
     * IU DeferredRegister.entries 反射删除，按任务要求不扩展 scope）。
     *
     * <p>每次调用<b>实时</b>读 ForgeConfigSpec（REGISTRY 阶段 get() 返回的已是
     * CONFIG_LOAD 阶段加载后的文件值），禁止一次性快照（见类注释的"取值时机铁律"）。
     */
    public static boolean isBanned(ResourceLocation id) {
        if (!"industrialupgrade".equals(id.getNamespace())) {
            return false;
        }
        return BANNED_REGISTRATIONS.get().contains(id.toString());
    }
}
