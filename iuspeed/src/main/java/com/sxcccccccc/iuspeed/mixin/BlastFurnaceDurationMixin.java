package com.sxcccccccc.iuspeed.mixin;

import com.denfop.blockentity.mechanism.blastfurnace.block.BlockEntityBlastFurnaceMain;
import com.sxcccccccc.iuspeed.IuSpeedConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * <b>v1.3.0：高炉（{@code BlockEntityBlastFurnaceMain}）纳入 iuspeed 5 倍速。</b>
 *
 * <p>高炉<b>不是配方机</b>：进度机制在 updateEntityServer 里（桌面源码逐行核对，
 * {@code BlockEntityBlastFurnaceMain.java}）：
 * <ul>
 *   <li>字段：{@code public double progress = 0;}（line 90）、
 *       {@code public int bar = 1;}（line 91，玩家可调 1..5，line 571）；</li>
 *   <li>每服务端 tick（line 476）：{@code progress += 1 + (0.25 * (bar1 - 1))}
 *       ——bar1 由剩余蒸汽量裁剪（line 464-467），即兜底速度 1/2 区间
 *       （bar 1..5 → 每 tick 1.0..2.0，看蒸汽）；</li>
 *   <li>完成判定（line 478）：{@code progress >= 3600} ——<b>硬编码字面量</b>
 *       3600（int 字面量被 javac 抬为 double：运行时字节码
 *       {@code ldc2_w 3600.0d; dcmpl; iflt}，updateEntityServer 中唯一一处
 *       3600.0 常量）；产出成功时 progress 归零 + 槽位 shrink + setActive(false)
 *       （line 479-483）。</li>
 * </ul>
 * 输入/输出伴类（BlastInputItem / BlastOutput / FluidInput / HeatBlock / OtherPart）
 * 经包内 grep 核对无任何进度/时间字段——全库只有主控持有进度，伴类是纯库存载体。
 *
 * <p><b>加速方式（延续 1.2.6 的"直接改时长"裁决，无任何倍率/速率机制）
 * </b>：{@code @ModifyConstant} 把<b>机器自身运行时读到的 3600.0 时长常量</b>
 * 直接改成 {@code 3600.0 / speed_multiplier}（默认 /5 → 720）：
 * <ul>
 *   <li>0.8.5 运行时 jar 逐指令核实：{@code ModifyConstantInjector.invokeConstantHandler}
 *       （mixin-0.8.5.jar 反汇编，{@code .libraries/org/spongepowered/mixin/0.8.5}）调
 *       {@code validateParams(InjectorData, handlerType, new Type[]{constantType})}——
 *       <b>期望参数表 = 恰好一个：常量类型本身</b>，因此 handler 必须是
 *       <b>纯 {@code (值类型)值类型} 形态（此处 {@code (double)double}，无接收者）</b>。
 *       （bytecode 中 {@code isStatic==false 时 ALOAD 0 + Extension} 只是给注入点
 *       周围栈帧补上下文，与 handler 签名无关——先前 "(Owner, double)double 与
 *       @Redirect 同规则" 的说法是错的，1.3.0 起一路带病，直到 1.3.2 实测崩溃：
 *       {@code InvalidInjectionException: ... Found unexpected argument type
 *       BlockEntityBlastFurnaceMain at index 0, expected double. Handler signature:
 *       (Lcom/denfop/...Main;D)D Expected signature: (D)D}，
 *       crash-2026-08-31_22.29.03-fml.txt；<b>v1.3.3 已改为 {@code (double)double} 纯值形态</b>，
 *       倍率从静态配置 {@code IuSpeedConfig.SPEED_MULTIPLIER} 读，不依赖接收者。）
 *       不是 @Redirect（无构造器调用限制），不生成任何合成类（区别于
 *       @ModifyArgs 的 Args$1 / mixin issue #584），@ModifyConstant 属
 *       <b>Mixin 核心</b>（mixin-0.8.5.jar 内自带，非 MixinExtras 依赖）；</li>
 *   <li>匹配语义（{@code BeforeConstant} 反汇编）：常量按
 *       {@code Double.equals} 精确匹配——{@code ldc2_w 3600.0d} 与
 *       {@code @Constant(doubleValue = 3600.0)} 全等；该常量在方法内唯一
 *       （另有一处 0.25d，干扰值）；{@code require = 1} 显式声明，
 *       mixins.json 的 {@code defaultRequire: 1} 兜底——找不到即启动报错，
 *       杜绝静默无效果；</li>
 *   <li>阈值 3600 → 720（/5）后：每 tick 蒸汽抽取量不变（line 477
 *       {@code tank.drain(bar1*2)}）、每 tick 能耗不变、bar 1..5 的
 *       速度档位语义原样（720/速率 tick 完成；bar=1 时 720 tick = 36 秒，
 *       原 3600 tick = 180 秒；bar=5 时 144 tick = 7.2 秒）；
 *       每炉次总蒸汽/耗时 ÷5，与项目其它机器的统一语义（时长 ÷N、每 tick
 *       能耗不变）一致。</li>
 * </ul>
 *
 * <p><b>第二处注入的必要性（GUI 一致性）</b>：进度条窗口
 * {@code ScreenBlastFurnace}（客户端专用类）用
 * {@code getProgress() / 3600D} 画条（源码 line 78、281，两处硬编码 3600）——
 * 若只改服务端阈值，因为完了 progress 即归零，进度条永远到不了满格
 * （只到 720/3600 = 20% 就完成），观感=坏条。故在<b>公共类</b>
 * {@code BlockEntityBlastFurnaceMain.getProgress()}（line 562-564，自声明
 * {@code @Override}，全库唯一调用方就是该窗口，IBlastMain 只是接口声明）的
 * RETURN 处 @Inject（{@code CallbackInfoReturnable}），把返回值 × 倍率：
 * 客户端进度条在缩短的 720 tick 里 0→20%（progress 展示值随 pack 到达）
 * 乘 5 → 0→100%，完成瞬间满格。无合成类、无客户端目标类（不碰
 * {@code ScreenBlastFurnace}——它只在客户端存在，若注入它，网络型专用服务器
 * 上该 mixin 目标永不加载，虽因 require 按 apply 时校验不致崩服，但
 * 徒增一个无意义 target；getProgress 一律走公共类注入）。注入规则核对：
 * getProgress 是<b>目标类自声明</b>方法 → handler 不得带接收者参数
 * （1.2.4 教训/0.8.5 规则），形如 {@code (CallbackInfoReturnable)V}；
 * 倍率 1 = 原值返回（setReturnValue 不触发），关闭语义保留。
 * <b>v1.3.4（第二条 @Inject 硬规则实测）</b>：handler 调
 * {@code setReturnValue()} 的 @Inject 必须声明 <b>{@code cancellable = true}</b>——
 * 0.8.5 的 {@code CallbackInfoReturnable.setReturnValue} 在注入点未声明可取消时
 * 直接抛 {@code CancellationException: The call getProgress is not cancellable}
 * （apply 期不报，客户端渲染到进度条字段才爆，
 * crash-2026-08-31_22.58.08-client.txt，堆栈 handler$zzk000$... ← getProgress:
 * 563）。本注入现已加 {@code cancellable = true}。全工程唯一一处
 * CallbackInfoReturnable/setReturnValue 使用点（其余 9 个 mixin 均为构造器
 * RETURN 纯 {@code (CallbackInfo)}，无 cancel/setReturn，不受此规则影响）。
 */
@Mixin(value = BlockEntityBlastFurnaceMain.class, remap = false)
public abstract class BlastFurnaceDurationMixin {

    @ModifyConstant(method = "updateEntityServer", constant = @Constant(doubleValue = 3600.0), remap = false, require = 1)
    private double iuspeed$shortenBlastFurnaceDuration(double value) {
        final int mult = IuSpeedConfig.SPEED_MULTIPLIER.get();
        return mult > 1 ? value / mult : value;
    }

    @Inject(method = "getProgress", at = @At("RETURN"), cancellable = true, remap = false, require = 1)
    private void iuspeed$scaleBlastFurnaceProgress(CallbackInfoReturnable<Double> cir) {
        final int mult = IuSpeedConfig.SPEED_MULTIPLIER.get();
        if (mult > 1) {
            cir.setReturnValue(cir.getReturnValue() * mult);
        }
    }
}
