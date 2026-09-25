package com.sxcccccccc.iuspeed;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * IU 机器配方 5 倍速（仿 GT kjsexportcmd 加速，但机制不同——见下）。
 *
 * <p><b>为什么不是改配方</b>：IU 的配方对象 {@code BaseMachineRecipe} /
 * {@code BaseFluidMachineRecipe} 只有输入/输出，<b>没有任何时间字段</b>
 * （桌面源码工业升级 3.4.0.1 + patched 3.4.0.10 运行时 jar 已核对）。
 * 操作时间在每台机器<b>方块实体</b>持有的操作组件里：
 * 各机器类构造器把硬编码的 {@code length}（200/300/400/800 等 tick）传给
 * {@code com.denfop.componets.ComponentProcess}（及蒸汽/多胞变体），
 * 组件每 tick 读取 {@code operationLength}，进度到达即完成
 * （ComponentProcess.updateEntityServer: line 326
 * {@code progress >= this.operationLength}）。
 *
 * <p><b>本 mod 做法</b>：五个 mixin 家族。
 * <ul>
 *   <li>组件家族（5 个 mixin）：{@code ComponentProcess}（电）、
 *       {@code ComponentSteamProcess}（蒸汽）、{@code ProcessMultiComponent} /
 *       {@code BioProcessMultiComponent} / {@code SteamProcessMultiComponent}
 *       （多胞）的 <init> RETURN 注入；</li>
 *   <li>自带时长机器家族（1 个多目标 mixin，45 个类）：不用组件、自行持有
 *       {@code int operationLength} 的配方机器（流体混合/分离/加热/整合、
 *       固体/电解、蒸汽机、基因、油提纯、世界收集器等）。</li>
 *   <li><b>v1.1</b> ComponentProgress 时长机器家族（1 个 mixin，3 类）：
 *       以 {@code ComponentProgress.maxValue} 为操作时长的机器——多方块
 *       冶炼炉 Furnace/Casting（108 tick，完成判定走 controller 的
 *       {@code getBar() >= 1}）与 AutoCrafter（100 tick）。</li>
 *   <li><b>v1.2</b> Timer 计时机器家族（2 类）：
 *       以 {@code com.denfop.utils.Timer} 计时的机器——原始/高级晶体生长室
 *       （3 分钟 / 45 秒）。<b>v1.2.6（当前）</b>：按用户裁决放弃倍率/速率机制，
 *       改为<b>直接时限替换</b>——{@code SiliconCrystalTimerMixin}（目标=两台机器类）
 *       + {@code TimerDurationAccessor}（interface accessor，setter 生成在
 *       Timer 类内部）。机制：两机器各自<b>唯一构造器</b>{@code <init>(BlockPos,
 *       BlockState)} 的 RETURN 注入，handler 标准形 {@code (CallbackInfo)V}，
 *       把<b>已构造完成</b>的 Timer 的 hour/minute/seconds/max 全部四字段
 *       直接写为 {@code 原始总秒数 / 倍率}（Primal 180s → 36s，GUI 45s → 9s）。
 *       关键点：{@code ComponentTimer} 构造器把原 Timer 放进 {@code timers}、
 *       克隆进 {@code defaultTimers}，而 {@code resetTime()}（onLoaded/getOutput/
 *       setCanWork 转换等时机）会拿 defaultTimers <b>读回</b>计时——
 *       <b>两个列表里的 Timer 实例必须同步改写</b>，只写活动 Timer 会被 resetTime
 *       还原。max 同步写（getMax/进度条/NBT 依赖）。GUI 机的 timer 是匿名
 *       ComponentTimer 子类，改的是其持有的共享 Timer 实例，levelBlock 等级加速
 *       语义不受影响。倍率写一次（one-shot），零每 tick 注入，percent 恒为
 *       默认 1.0 不触碰；其余 ComponentTimer 用户（太阳能板族昼夜周期、
 *       雷击棒控制器天气、观月台/石墨炉/物质厂/碱性采石场）
 *       零触碰——Timer 类只多出 4 个惰性 setter。
 *       v1.2.5（percent 速率方案）已删除：其 @Shadow 漏写 {@code remap=false}，
 *       0.8.5 MixinInfo$State.validateRemappable 在 post-initialise 抛
 *       {@code InvalidMixinException: Found a remappable @Shadow annotation}，
 *       mixin 被<b>整只拒载</b>（游戏继续、加速零生效、日志一条 ERROR）——
 *       现以 {@code @Shadow(remap = false)} 显式覆盖。
 *       前五代败因回顾（全部规避，详见 SiliconCrystalTimerMixin 的 javadoc）：
 *       1.2.0 构造器 HEAD（super() 前注入被拒；现为 RETURN，super() 后）、
 *       1.2.1 构造器注入 handler 带 self（构造器注入禁 self；现为纯
 *       {@code (CallbackInfo)}）、1.2.2 @Redirect 构造器调用（Mixin 0.8.5 禁止；
 *       现不 redirect 任何东西）、1.2.3 @Shadow 超类方法 {@code getParent()}
 *       （0.8.5 类 mixin @Shadow 只认<b>目标类自声明成员</b>；现 @Shadow 的
 *       {@code timer} 是两目标类自声明字段）、1.2.4 给 ComponentTimer
 *       <b>自声明</b>方法 updateEntityServer()（零参数）注入却带接收者参数
 *       （0.8.5 的 handler 接收者参数只对<b>继承</b>的注入方法有效，目标类
 *       自声明方法要求纯 {@code (CallbackInfo)}；现不注入该方法）。
 *       1 = 关闭语义保留。</li>
 *   <li><b>v1.3.0</b> 高炉家族（1 个 mixin：{@code BlastFurnaceDurationMixin}，
 *       目标 {@code BlockEntityBlastFurnaceMain}）：高炉不是配方机——硬编码
 *       状态机，updateEntityServer 里 {@code progress += 1 + 0.25*(bar1-1)}
 *       （每秒 1..2，看蒸汽档）累计到<b>硬编码字面量</b> {@code >= 3600}
 *       才产出（源码 line 476/478）。{@code @ModifyConstant}（Mixin 核心注解，
 *       0.8.5 自带；{@code ModifyConstantInjector extends RedirectInjector}，
 *       <b>handler 无接收者：纯 {@code (double)double}</b>——0.8.5
 *       {@code invokeConstantHandler} 的 validateParams 期望参数表 = 恰好
 *       [常量类型] 单个（运行时 jar 反汇编 + 崩溃日志交叉核实；
 *       1.3.0-1.3.2 的 "(Owner, double)double" 带接收者签名是错的，
 *       1.3.2 在 APPLY 期爆 {@code InvalidInjectionException}，v1.3.3 已修））
 *       在服务端把 3600.0 直接改写为 3600/倍率（默认 720 tick 完成）；
 *       同一 mixin 的第二处注入在公共类 {@code getProgress()}
 *       （自声明方法，handler 不含接收者——1.2.4 规则）RETURN 处把返回值 × 倍率
 *       （handler 调 {@code setReturnValue} → @Inject 必须 {@code cancellable=true}，
 *       0.8.5 缺它会在渲染期抛 {@code CancellationException}；v1.3.3 实测崩于
 *       crash-2026-08-31_22.58.08，v1.3.4 已修），
 *       让客户端进度条（{@code ScreenBlastFurnace} 用
 *       {@code getProgress()/3600D} 画，源码 line 78/281）在缩短的 720 tick 里
 *       满格，避免"条只到 20% 就完成"。不碰客户端 Screen 类（专用服务器上
 *       目标永不加载，徒增无效 target）。bar 1..5 档位、每 tick 蒸汽抽取
 *       （line 477）与每 tick 能耗原样；每炉次耗时/总蒸汽 ÷5。输入/输出
 *       伴类（BlastInputItem/BlastOutput/FluidInput/HeatBlock/OtherPart）
 *       无任何进度字段（包内 grep 核对）。</li>
 *   <li><b>v1.3.2</b> 编程台（电）加入 Timer 家族（{@code ProgrammingTableTimerMixin}）。
 *       <b>机制定案</b>：电编程台是<b>纯计时器机器</b>——构造器
 *       {@code new ComponentTimer(this, new Timer(0, 2, 0))}（源码 line 57，运行时
 *       3.4.0.10 字节码 iconst_0/iconst_2/iconst_0 + 匿名类
 *       {@code BlockEntityProgrammingTable$1} 逐指令一致）= 2 分钟/次（levelBlock=0
 *       时 2400 tick），完成判定 {@code getTimers().get(0).getTime() <= 0}（line 185）。
 *       先前"点击小游戏"定论系<b>张冠李戴</b>：那是 Primal 编程台
 *       （{@code BlockEntityPrimalProgrammingTable}，ComponentProgress 300 色条点击
 *       {@code updateTileServer}，id {@code industrialupgrade:primal_programming_table/
 *       primal_programming_table}），不在本 mod 目标内。物品 id 经运行时 kubejs 导出
 *       核实 = {@code industrialupgrade:basemachine3/programming_table}（item/block/
 *       block_entity_type 三表一致；枚举 {@code BlockBaseMachine3Entity.
 *       programming_table(BlockEntityProgrammingTable.class, 183)}，用户记忆的
 *       "basemachine2" 实为 basemachine3）。改法与晶机同型：唯一构造器 RETURN +
 *       {@code TimerDurationAccessor} 双列表直写四字段（120s → 24s @5x）；
 *       levelBlock 升级（{@code getTickFromSecond = 20 - 1.75*level}）语义原样叠加；
 *       每 tick 能耗（2 EU/t）不变、每 op 总能耗 ÷5。</li>
 *   <li><b>v1.4.0</b>（用户裁决"所有机器都想加速"）全量审计补漏：
 *       <ul>
 *         <li><b>Timer 家族 +4</b>（{@code TimerMachinesMixin}）：
 *             碱性采石场 15s → 3s、石墨炉 90s → 18s、物质工厂 60s → 12s、
 *             观月台 20s → 4s——天然或匿名 {@code getTickFromSecond}
 *             （20 - level*1.75/1.9）等级加速语义保留，其余与晶机/编程台同型；</li>
 *         <li><b>standalone 家族 +5</b>：石头/黑曜石发生器
 *             （收抽象父类 {@code BaseGenStone}/{@code BaseObsidianGenerator}
 *             覆盖全族）、电泵 {@code BlockEntityPump}、组合泵
 *             {@code BlockEntityCombinedPump}、原始流体加热器
 *             {@code BlockEntityPrimalFluidHeater}——均自带
 *             {@code operationLength} 自动进度；Pump 的 {@code onLoaded →
 *             setUpgradestat} 与各机器升级重算都从缩后的
 *             {@code defaultOperationLength} 派生，一致性成立；</li>
 *         <li><b>双构造器例外</b>：{@code BlockEntityBaseAdditionGenStone}
 *             （this() 链）走专属 {@code AdditionGenStoneDurationMixin}，
 *             按 7 参构造器 descriptor 定向注入、恰好命中一次；</li>
 *         <li><b>蒸汽泵例外</b>：{@code BlockEntitySteamPump} 的
 *             {@code ComponentProgress.maxValue} = 25 只在构造器写死、
 *             此后<b>无任何重派点</b>（无 setUpgradestat、无裸 progress），
 *             只缩字段等于没缩——专属 {@code SteamPumpDurationMixin}
 *             同时调 {@code scale} + {@code scaleProgress}；</li>
 *         <li><b>ComponentProgress 家族 +3</b>（ProgressLengthMachineMixin）：
 *             夜转换器 80 → 16、土壤分析器 400 → 80（一次性 analyzed）、
 *             量子矿机 1000 → 200（{@code getBar() == 1} 精确相等：进度
 *             不写 NBT、重载归零、≤32767 在 double 下整除精确，安全）。</li>
 *       </ul>
 *       <b>仍然不碰、等待单独裁决（语义禁止）</b>：相位/天气调度机——太阳能板
 *       （8h/4h/4h）、迷你板（3h/2h30/2h）、雷击棒控制器（5 分钟打击冷却）：
 *       缩短相位会与现实太阳周期脱同步、打击频率与天气语义绑定，需用户裁决。
 *       <b>玩家驱动、非计时</b>：PrimalPump（潜行点击 +4）、Primal 小游戏
 *       三件套。原始手动配电机（粉碎/压缩/烘干/轧制/铁砧等 progress→100 模式）
 *       仍是玩家交互语义不碰。</li>
 * </ul>
 * 缩放 {@code operationLength / defaultOperationLength / operationChange}
 * / ComponentProgress 的 {@code maxValue} ——等效于 GT 的 duration/5，全局
 * 覆盖该类机器（每台机器都有独立组件/字段实例，构造时即缩放，含后续创建的
 * 新机器，与 /reload、KubeJS 配方写出均无关）。每 tick 能耗与任何内部缓冲
 * 容量一律不动，只缩短时间。
 * v1.1 起 mixin 注入 defaultRequire=1：任何注入目标缺失会在启动时直接报错，
 * 杜绝"静默没生效"。
 */
@Mod(IuSpeed.MODID)
public class IuSpeed {

    public static final String MODID = "iuspeed";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IuSpeed() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, IuSpeedConfig.SPEC);
        LOGGER.info("[iuspeed] loaded, speed_multiplier={} (1 = off; config: config/iuspeed-common.toml)",
                IuSpeedConfig.SPEED_MULTIPLIER.get());
    }
}
