package com.sxcccccccc.iuunify.mixin;

import com.denfop.componets.Fluids;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阶段二 C 线：输入侧 tag 化的判定侧（咽喉点）。
 *
 * <p>目标类 {@code Fluids$InternalFluidTank} 的
 * {@code acceptsFluid(Fluid)} 是 IU 全部流体罐接受判定的公共汇聚点
 * （jar 字节码实证）：外部管道/罐互灌走 capability
 * {@code Fluids$FluidHandler.isFluidValid → acceptsFluid}，罐内自灌走
 * {@code FluidTank.fill → isFluidValid(重写) → acceptsFluid}。在此 HEAD 拦截：
 * 若该罐登记了宽限 tag 且传入流体命中 tag → 直接放行；否则回落到机器原始
 * 精确 id 谓词（阶段一重定向后的 ic2_120 流体恒命中，行为与 0.1.0 一致）。
 *
 * <p>语义 = "tag 或原判定"并集：19 台蒸汽机/焦炉/高炉只认 ic2 蒸汽的现状不变，
 * 额外接受同 tag 的 mek 蒸汽等；BaseHeatMachine/SmelteryFuelTank 多流体热源列表、
 * 汽轮机冷却液多流体列表、SingleFluidAdapter 配方驱动列表同理只加不减。
 *
 * <p>输出侧/判定侧零影响：未登记 tag 的罐（焦炉产物罐、创造蒸汽罐、锅炉/汽轮
 * 校验、主控堆校验等）完全不经过宽限分支，行为与 0.1.0 完全一致。
 */
@Mixin(value = Fluids.InternalFluidTank.class)
public abstract class InternalFluidTankAcceptMixin {

    /**
     * 目标签名（jar javap 实证）：{@code public boolean
     * acceptsFluid(net.minecraft.world.level.material.Fluid)}；描述符
     * {@code (Lnet/minecraft/world/level/material/Fluid;)Z}。
     * remap=false：目标类/方法是 mod 成员；handler 体内原版调用由 reobfJar 自动
     * 重映射为运行时 SRG 名。
     *
     * <p>0.3.2 修复（同 FluidNameRedirectMixin 排雷序列）：@Inject handler 参数
     * 只允许 [目标方法参数] + [CallbackInfo]，this 不占参数位——原写法
     * {@code (InternalFluidTank self, fluid, CIR)} 会 InvalidInjectionException。
     * this（= 目标罐实例）直接传给 {@link InputTagRegistry#tagFor(Object)}，
     * 参数声明 Object 以便 mixin 类的 this 向上转型（WeakHashMap.get(Object)
     * 兼容，无需 @Shadow）。
     */
    @Inject(
            method = "acceptsFluid(Lnet/minecraft/world/level/material/Fluid;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void iuunify$tagWiden(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        TagKey<Fluid> tag = InputTagRegistry.tagFor(this);
        if (tag != null && fluid.builtInRegistryHolder().is(tag)) {
            cir.setReturnValue(true);
        }
    }
}
