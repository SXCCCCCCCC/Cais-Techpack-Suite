package com.sxcccccccc.iuspeed.mixin;

import com.denfop.blockentity.mechanism.BlockEntityProgrammingTable;
import com.denfop.componets.ComponentTimer;
import com.denfop.utils.Timer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * <b>v1.3.2：编程台（电），{@link BlockEntityProgrammingTable} 直接时限改造。</b>
 *
 * <p><b>机制定案（桌面源码 + 运行时 3.4.0.10 字节码双重核实，前一轮"点击小游戏"
 * 结论是张冠李戴——那是 Primal 变体）：</b>
 * <ul>
 *   <li>电编程台是<b>纯计时器机器</b>：构造器
 *       {@code this.timer = this.addComponent(new ComponentTimer(this, new Timer(0, 2, 0)) { ... })}（源码 line 57，
 *       运行时字节码 iconst_0/iconst_2/iconst_0 + new Timer(III)V + 匿名类
 *       {@code BlockEntityProgrammingTable$1} 逐指令一致）= 2 分钟/次（120 计时秒；
 *       levelBlock=0 时每秒 20 tick → 2400 game tick）。</li>
 *   <li>完成判定在 {@code updateEntityServer()} line 185：
 *       {@code this.timer.getTimers().get(0).getTime() <= 0} → consume + output（line 186-188），
 *       {@code getOutput()}（line 203）会 {@code timer.resetTime()} 用 defaultTimers 读回——<b>两个列表必须同步改写</b>。</li>
 *   <li>匿名子类的 {@code getTickFromSecond()} = {@code max(1, 20 - levelBlock*1.75)}（源码 line 59-61）：
 *       levelBlock（onActivated 用 upgrade_speed_creation 右击升级，line 104-119）越大每计时秒的
 *       game tick 越少 = 越快。本 mixin 只改 Timer 基础时长、不碰该倍率——等级加速语义上
 *       叠加保留，与 SiliconCrystalTimerMixin 对 GUI 晶体机的处理完全同型。</li>
 *   <li><b>"点击小游戏"属于另一台机器</b>：{@code BlockEntityPrimalProgrammingTable}（Primal 编程台，
 *       id {@code industrialupgrade:primal_programming_table/primal_programming_table}），其
 *       {@code ComponentProgress(1, 300)}（line 65）经 {@code updateTileServer(Player,double)} 玩家点击
 *       增减 + 色条小游戏驱动——不是本 mixin 目标，零触碰。</li>
 *   <li>物品 id 用运行时 kubejs 导出核实：
 *       {@code industrialupgrade:basemachine3/programming_table}（item/block/block_entity_type
 *       三个注册表均在此 id；用户记忆中的 "basemachine2" 实为 basemachine3——枚举
 *       {@code BlockBaseMachine3Entity.programming_table(BlockEntityProgrammingTable.class, 183)}
 *       行 262，{@code getMainPath()} = "basemachine3"）。</li>
 * </ul>
 *
 * <p><b>改法</b>：本类<b>唯一构造器</b>{@code <init>(BlockPos, BlockState)} 的 RETURN（super() 之后、
 * 对象未发布）注入，handler 纯 {@code (CallbackInfo)}（1.2.1 教训：构造器注入禁 self；1.2.0 教训：
 * 禁 HEAD）；把已构造完成的 Timer 的 hour/minute/seconds/max 四字段直接写为
 * {@code 总秒数 / 25}（120s → 4.8s；2400 tick → 96 tick）。<b>v1.4.1：该机不可插
 * 升级，独立固定 25 倍，不随全局 speed_multiplier。</b>
 * addComponent 虽然在构造期间就把 timer 挂进父实体，但父实体的 tick 循环只在整体构造
 * 完成后才启动，handler 改写发生在对象发布之前——无竞态，与晶机完全同型（该形态
 * 1.3.1 已实证可 APPLY 并执行）。
 *
 * <p>{@code defaultTimers} 的克隆只被 {@code resetTime()} 的 {@code Timer.readTimer()} 读走
 * hour/minute/seconds（max 不复制，见 Timer.java line 167-172），但 {@code getTimes()}（GUI 进度条
 * 数据源，ComponentTimer line 99-107）用 {@code defaultTimers.get(i).getBar()}（由字段现算），
 * 故<b>两个列表的四字段全部改写</b>、max 一并写（getMax/getProgressBar/NBT 同步依赖），
 * 与 SiliconCrystalTimerMixin 完全同型（v1.2.6 实训结论）。
 *
 * <p>每 tick 能耗 {@code canUseEnergy(2)/useEnergy(2)}（line 175/184）原样：只短时间，
 * 每 op 总能耗按比例下降，与项目约定一致。百分比=默认 1.0 不动；注入
 * {@code require=1}（config 级 defaultRequire=1），注入目标缺失直接启动报错。
 */
@Mixin(value = BlockEntityProgrammingTable.class, remap = false)
public abstract class ProgrammingTableTimerMixin {

    @Shadow(remap = false)
    private ComponentTimer timer;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void iuspeed$shortenTimer(CallbackInfo ci) {
        int mult = 25;
        if (mult > 1) {
            List<Timer> active = this.timer.getTimers();
            List<Timer> defaults = this.timer.getDefaultTimers();
            if (!active.isEmpty() && active.size() == defaults.size()) {
                for (int i = 0; i < active.size(); i++) {
                    int total = active.get(i).getTime() / mult;
                    if (total < 1) {
                        total = 1;
                    }
                    TimerDurationAccessor a = (TimerDurationAccessor) (Object) active.get(i);
                    TimerDurationAccessor d = (TimerDurationAccessor) (Object) defaults.get(i);
                    int h = total / 3600;
                    int m = (total % 3600) / 60;
                    int s = total % 60;
                    a.setHour(h);
                    a.setMinute(m);
                    a.setSeconds(s);
                    a.setMax(total);
                    d.setHour(h);
                    d.setMinute(m);
                    d.setSeconds(s);
                    d.setMax(total);
                }
            }
        }
    }
}
