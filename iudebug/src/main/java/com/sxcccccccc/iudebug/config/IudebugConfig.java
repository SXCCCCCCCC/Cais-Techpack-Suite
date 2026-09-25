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

    /**
     * 分子重构机默认配方（= 原生 init() 全部 30 条，核心链按用户裁决 4→3 换 1，
     * 钚输入直接用 OEI 统一品 ic2_120:plutonium）。id 均经 3.4.0.1 源码核对
     * （DataItem mainPath/枚举名；iuingot.getStack(5)=caravky_ingot、crafting
     * 275/645/649 = crafting_N_element 连续枚举）。
     */
    private static final List<String> MOLECULAR_DEFAULT_RECIPES = List.of(
            "minecraft:wither_skeleton_skull*1|minecraft:nether_star*1|4000000.0",
            "ic2_120:plutonium*1|industrialupgrade:reactors/proton*1|15500000.0",
            "#forge:ingots/Spinel*1|industrialupgrade:itemingots/caravky_ingot*1|2500000.0",
            "industrialupgrade:photoniy*1|industrialupgrade:photoniy_ingot*1|12000000.0",
            "minecraft:netherrack*1|minecraft:gunpowder*2|70000.0",
            "minecraft:sand*1|minecraft:gravel*1|45000.0",
            "#forge:ingots/Iridium*1|industrialupgrade:itemcore/advcore*1|1500.0",
            "industrialupgrade:itemcore/advcore*3|industrialupgrade:itemcore/hybcore*1|11720.0",
            "industrialupgrade:itemcore/hybcore*3|industrialupgrade:itemcore/ultcore*1|60000.0",
            "industrialupgrade:itemcore/ultcore*3|industrialupgrade:itemcore/quacore*1|300000.0",
            "industrialupgrade:itemcore/quacore*3|industrialupgrade:itemcore/specore*1|1500000.0",
            "industrialupgrade:itemcore/specore*3|industrialupgrade:itemcore/procore*1|7500000.0",
            "industrialupgrade:itemcore/procore*3|industrialupgrade:itemcore/sincore*1|45000000.0",
            "industrialupgrade:itemcore/sincore*3|industrialupgrade:itemcore/admcore*1|180000000.0",
            "industrialupgrade:itemcore/admcore*3|industrialupgrade:itemcore/phocore*1|900000000.0",
            "industrialupgrade:itemcore/phocore*3|industrialupgrade:itemcore/neucore*1|2700000000.0",
            "industrialupgrade:itemcore/neucore*3|industrialupgrade:itemcore/barcore*1|4500000000.0",
            "industrialupgrade:itemcore/barcore*3|industrialupgrade:itemcore/adrcore*1|9000000000.0",
            "industrialupgrade:itemcore/adrcore*3|industrialupgrade:itemcore/gracore*1|12000000000.0",
            "industrialupgrade:itemcore/gracore*3|industrialupgrade:itemcore/kvrcore*1|21000000000.0",
            "industrialupgrade:crafting_elements/crafting_649_element*8|industrialupgrade:crafting_elements/crafting_645_element*1|20000000.0",
            "industrialupgrade:solidmatter/sun_matter*1|industrialupgrade:lens/sunlinse*1|25000000.0",
            "industrialupgrade:solidmatter/aqua_matter*1|industrialupgrade:lens/rainlinse*1|25000000.0",
            "industrialupgrade:solidmatter/nether_matter*1|industrialupgrade:lens/netherlinse*1|25000000.0",
            "industrialupgrade:solidmatter/night_matter*1|industrialupgrade:lens/nightlinse*1|25000000.0",
            "industrialupgrade:solidmatter/earth_matter*1|industrialupgrade:lens/earthlinse*1|25000000.0",
            "industrialupgrade:solidmatter/end_matter*1|industrialupgrade:lens/endlinse*1|25000000.0",
            "industrialupgrade:solidmatter/aer_matter*1|industrialupgrade:lens/aerlinse*1|25000000.0",
            "minecraft:iron_ingot*1|industrialupgrade:crafting_elements/crafting_275_element*1|7500000.0",
            "industrialupgrade:crafting_elements/crafting_275_element*1|industrialupgrade:photoniy*1|1450000.0"
    );

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BANNED_REGISTRATIONS;

    // ---- [molecular] 段（v1.2.0：分子重构机配方全量配置驱动） ----
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MOLECULAR_RECIPES;

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

        // ---- [molecular] 段（v1.2.0：分子重构机 init() 配方全量配置驱动） ----
        builder.push("molecular");
        MOLECULAR_RECIPES = builder
                .comment(
                        "分子重构机（BlockEntityMolecularTransformer）的完整配方表（@Overwrite init()）。",
                        "每行一条，格式：输入*数量|输出*数量|能量",
                        "输入带 # 前缀 = tag 输入（如 #forge:ingots/Iridium*1）；不带 = 物品 id。",
                        "数量省略时默认 1。能量为 double。",
                        "解析失败直接抛异常崩溃（不留静默错误）；改完需重启游戏生效。",
                        "默认值 = 原版全部 30 条配方，其中：核心升级 4 换 1 已改为 3 换 1",
                        "（用户 2026-09-04 裁决，3^13 总需求），钚输入直接用 ic2_120:plutonium",
                        "（OEI 统一品），其余与原生逐条一致。"
                )
                .defineList("recipes", MOLECULAR_DEFAULT_RECIPES, e -> e instanceof String);
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
