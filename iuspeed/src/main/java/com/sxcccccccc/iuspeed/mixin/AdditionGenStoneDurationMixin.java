package com.sxcccccccc.iuspeed.mixin;

import com.denfop.blockentity.mechanism.BlockEntityBaseAdditionGenStone;
import com.sxcccccccc.iuspeed.util.IuSpeedHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * <b>v1.4.0：扩增石头发生器（Expanded/GenerationAdditionStone 一族）</b>
 * ——{@code BlockEntityBaseAdditionGenStone} 双构造器之下的定向缩放。
 *
 * <p><b>为什么独成一个 mixin（而不进 StandaloneMachineOperationLengthMixin）</b>：
 * 该抽象基类有两个构造器——6 参数
 * {@code (int energyPerTick, int length, int outputSlots, MultiBlockEntity,
 * BlockPos, BlockState)} 直接 {@code this(..., 1, ...)} 委派给 7 参数
 * {@code (..., int aDefaultTier, ...)}；字段赋值只发生在 7 参构造器
 * （line 62/65: {@code defaultOperationLength = operationLength = length}）。
 * 若按一般的 {@code "<init>"} 全构造器注入，this() 链会让同一次实例化
 * 触发<b>两次</b>缩放（40 tick → 8 → 2 或直线错误）。按 7 参构造器的完整
 * descriptor {@code (IIILcom/denfop/api/blockentity/MultiBlockEntity;
 * Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V}
 * 定向注入（remap=false，descriptor 不参与映射），任何实例化路径——
 * 无论子类直接 super 7 参、还是经 6 参 this() 链——都<b>恰好命中一次</b>。
 * 运行时 jar 3.4.0.10 javap 已核对：两个构造器 descriptor 与此处完全一致。
 *
 * <p>机制：与标准 standalone 族同型——updateEntityServer（line 195+）
 * 自动 {@code progress++}，完成判定 {@code progress >= operationLength}
 * （line 基础 100 tick，能量 1/tick {@code energyConsume}），升级重算
 * {@code setOverclockRates()}（line 249）从 <b>defaultOperationLength</b> 派生
 * （{@code upgradeSlot.getOperationLength(defaultOperationLength)}），故
 * 两个字段同步 {@link IuSpeedHelper#scale} 后升级后仍然是 5 倍速；
 * 每 tick 能耗不动（每 op 总能耗 ÷5，与项目约定一致）。
 */
@Mixin(value = BlockEntityBaseAdditionGenStone.class, remap = false)
public abstract class AdditionGenStoneDurationMixin {

    @Inject(
            method = "<init>(IIILcom/denfop/api/blockentity/MultiBlockEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("RETURN"),
            remap = false
    )
    private void iuspeed$scaleAddedGenStone(CallbackInfo ci) {
        IuSpeedHelper.scale(this);
    }
}
