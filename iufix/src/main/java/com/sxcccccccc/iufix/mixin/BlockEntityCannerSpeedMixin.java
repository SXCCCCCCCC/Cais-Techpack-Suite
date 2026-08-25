package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityInventory;
import com.denfop.blockentity.mechanism.BlockEntityCanner;
import com.denfop.componets.ComponentProcess;
import com.denfop.componets.ComponentProgress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 装罐机加工时长 300 刻 → 30 刻（Jade/进度条显示的 10 秒 → 1 秒）。
 *
 * <p>证据链（源码 + 发布 jar 字节码核对）：
 * BlockEntityCanner.<init> 里
 * (a) componentProgress = addComponent(new ComponentProgress(this, 1, (short)300)) ——
 *     ComponentProgress 初值 max=300，Jade 的 BlockComponentProvider（Jade data 那段）
 *     首读 maxValue，开机 4 秒内显示 300；
 * (b) componentProcess = addComponent(new ComponentProcess(this, 300, 1)) —— defaultOperationLength=300，
 *     setOverclockRates 由它派生，updateEntityServer 里
 *     componentProgress.setMaxValue((short)operationLength) 会逐刻把进度条同步成 300。
 * 两处都置 30：机器转 1 秒完成，Jade 即刻显示 30；超频/降速 upgrade 仍按默认 30 的倍数走
 * （getOperationLength1(defaultOperationLength)）。
 *
 * <p>@Redirect(value=NEW)：target 写“类名+构造器描述符”（外部描述符形式，无点号）——
 * mixin 0.8.5 的 BeforeNew 构造时会把 target 里的所有 '.' 替换成 '/'（原文
 * type.replace('.', '/')），此前写的 "com.denfop.componets.ComponentProcess.<init>"
 * 解析前就被揉成 "com/denfop/componets/ComponentProcess/<init>"，过不了
 * MemberInfo.validate（Invalid owner，1.5.1 启动崩溃点）。
 * 描述符取自发布 jar 字节码实证：BlockEntityCanner.<init> 中
 * ComponentProcess."<init>":(Lcom/denfop/blockentity/base/BlockEntityInventory;ID)V、
 * ComponentProgress."<init>":(Lcom/denfop/blockentity/base/BlockEntityInventory;IS)V，
 * 且每个类的 NEW 恰出现一次，require=1 落空即直接崩溃（宁崩不静默）。
 * handler 签名 = 构造器参数 + 返回被构造类型，返回同型对象，原 INVOKESPECIAL 被吞掉。
 */
@Mixin(value = BlockEntityCanner.class, remap = false)
public abstract class BlockEntityCannerSpeedMixin {

    @Redirect(method = "<init>",
            at = @At(value = "NEW", target = "(Lcom/denfop/blockentity/base/BlockEntityInventory;ID)Lcom/denfop/componets/ComponentProcess;"),
            require = 1)
    private ComponentProcess iufix$cannerProcess(BlockEntityInventory base, int operationLength, double energyConsume) {
        return new ComponentProcess(base, 30, energyConsume);
    }

    @Redirect(method = "<init>",
            at = @At(value = "NEW", target = "(Lcom/denfop/blockentity/base/BlockEntityInventory;IS)Lcom/denfop/componets/ComponentProgress;"),
            require = 1)
    private ComponentProgress iufix$cannerProgress(BlockEntityInventory base, int col, short max) {
        return new ComponentProgress(base, col, (short) 30);
    }

}
