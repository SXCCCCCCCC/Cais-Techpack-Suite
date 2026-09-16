package com.sxcccccccc.tecend.mixin;

import net.mcreator.proofofhonor.block.RunnerupTrophyBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * 银奖杯（{@code proof_of_honor:runnerup_trophy}）原本是 {@code strength(-1.0F, 3600000.0F)} ——
 * 硬度 -1 = 不可破坏（基岩写法），而**挖掘 tag 治不好它**：tag 只解决"没有工具算正确工具"。
 * 这里把这两个参数改成与金块一致（3.0F / 6.0F）。
 *
 * <p><b>为什么只能从构造参数下手</b>（javap 逐条查过）：硬度由 {@code Properties} 在构造时定死，
 * 方块状态创建时缓存进 {@code BlockBehaviour$BlockStateBase} 的一个 float 字段；
 * {@code getDestroySpeed} 的实现就是 {@code getfield 那个字段; freturn} ——
 * 之后没有任何可覆写的 getter（{@code Block} 的四个无参 float getter 是
 * explosionResistance / friction / speedFactor / jumpFactor，都不是硬度）。
 * 所以唯一的干净入口就是构造器里那次 {@code Properties.strength(FF)} 调用。</p>
 *
 * <p><b>关于"构造器能不能注入"</b>：Mixin 文档里"构造器只支持 RETURN 注入点"的限制是针对
 * {@code @Inject} 这类基于注入点的注入器；{@code @ModifyArg}/{@code @ModifyArgs} 属于调用点注入器
 * （{@code InvokeInjector}），官方 issue #267 也把"redirect 构造器里的调用"列为可行做法。
 * 这里用 {@code @ModifyArg} 按参数下标改（下标是该调用自身的参数序号，不含 receiver，无歧义）。</p>
 *
 * <p>跨 mod mixin ⇒ {@code remap = false} + 目标串写运行时真名（SRG {@code m_60913_}，描述符照字节码抄）。</p>
 *
 * <p><b>handler 必须是 static</b>（实测踩坑）：注入点在建 Properties 那几行，位于本构造器的
 * {@code super()} 之前（字节码顺序：先 {@code Properties.of()...strength()}，再 {@code invokespecial Block."<init>"}），
 * 而 {@code super()} 之前 {@code this} 还不存在 —— Mixin 会直接报
 * {@code InvalidInjectionException: @ModifyArg handler before super() invocation must be static}。
 * 所以这里不能套用"handler 的 static 修饰符与被注入方法一致"那条通则。</p>
 */
@Mixin(value = RunnerupTrophyBlock.class, remap = false)
public abstract class RunnerupTrophyHardnessMixin {

    private static final String STRENGTH_CALL =
            "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;m_60913_(FF)"
                    + "Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;";

    /** 硬度：换成金块的 3.0F（原本 -1.0F = 不可破坏） */
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = STRENGTH_CALL), index = 0, require = 1)
    private static float tecend$normalHardness(float hardness) {
        return 3.0F;
    }

    /** 抗爆：换成金块的 6.0F（原本 3600000.0F） */
    @ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = STRENGTH_CALL), index = 1, require = 1)
    private static float tecend$normalResistance(float resistance) {
        return 6.0F;
    }
}
