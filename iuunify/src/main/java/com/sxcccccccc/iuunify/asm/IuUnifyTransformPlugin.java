package com.sxcccccccc.iuunify.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * IU 流体强转链修复的 ASM 转换插件（阶段二 B 线核心）。
 *
 * <p>背景（发布 jar 字节码实证，IndustrialUpgrade-1.20.1-3.4.0.10）：
 * IU 代码里把 {@code FluidName.fluidX.getInstance().get()} 的结果强转成
 * {@code com.denfop.blocks.IUFluid} 再调用 {@code m_5613_()}
 * （= {@code FlowingFluid.getSource()}，返回源流体）或 {@code getFluidType()}，
 * 链式取源流体方块状态 / FluidType：
 * <pre>
 *   checkcast IUFluid
 *   invokevirtual IUFluid.m_5613_:()Lnet/minecraft/world/level/material/Fluid;
 *   invokevirtual Fluid.m_76145_:()Lnet/minecraft/world/level/material/FluidState;   // defaultFluidState
 *   invokevirtual FluidState.m_76188_:()Lnet/minecraft/world/level/block/state/BlockState; // createLegacyBlock
 * </pre>
 * 阶段一（FluidNameRedirectMixin）把 getInstance() 重定向到 IC2 Refabricated
 * 流体（运行时为 {@code FlowingFluid} 子类，Connector mappings.tsrg 实证
 * {@code class_3609=FlowingFluid}）后，checkcast 抛 ClassCastException。
 *
 * <p>修复原理：这两个被调方法都声明在 {@code FlowingFluid} 的父类链上
 * （getSource 在 FlowingFluid 本身、getFluidType 在 Fluid——Forge patch），
 * invokevirtual 的静态解析只要求方法在声明类父链存在、运行时按接收者实际类
 * 分发。因此把 checkcast 类型与后续 invokevirtual 的方法引用 owner 从
 * {@code IUFluid} 统一改为 {@code FlowingFluid}：
 * <ul>
 *   <li>校验器：checkcast 后栈类型 FlowingFluid 与 invokevirtual 声明类
 *       FlowingFluid 精确匹配（mixin 应用器以 COMPUTE_FRAMES 重算帧）；</li>
 *   <li>运行时：IC2 流体（Ic2Fluid extends FlowingFluid）正常分发——getSource()
 *       返回 IC2 源流体 → defaultFluidState().createLegacyBlock() 得到 IC2
 *       pahoehoe_lava 源方块状态，即统一后的正确语义；</li>
 *   <li>未重定向的 IU 流体（IUFluid extends ForgeFlowingFluid extends
 *       FlowingFluid）同样通过，行为与原来一致。</li>
 * </ul>
 *
 * <p>只改写"checkcast IUFluid 后紧跟的 invokevirtual（owner=IUFluid）"，不动
 * 其它指令；IUFluid 特有方法（如 getFluidProperty）不在任何强转链后出现
 * （全树 grep 实证），不会误伤。
 *
 * <p>覆盖范围（9 处运行时强转 + 1 处注册期外的 ClientProxy）：
 * ClientProxy.onClientSetup、GeneratorVolcano 构造器×3、SpaceMaterialSet
 * fallbackLiquid/resolveLiquidFallback、SpaceVolcanoPlacementLogic
 * buildVolcanoColumns、SpaceRegionalHydrologyFeature.resolveHydrologyFluid、
 * SpaceBodyDefinitionRegistry.flowingLava。SpaceConfiguredFeaturesBootstrap:365
 * 只在数据生成（BootstapContext）执行，不在覆盖范围。
 */
public class IuUnifyTransformPlugin implements IMixinConfigPlugin {

    private static final String IU_FLUID = "com/denfop/blocks/IUFluid";
    private static final String FLOWING_FLUID = "net/minecraft/world/level/material/FlowingFluid";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        for (MethodNode method : targetClass.methods) {
            AbstractInsnNode insn = method.instructions.getFirst();
            while (insn != null) {
                if (insn.getOpcode() == Opcodes.CHECKCAST) {
                    TypeInsnNode checkcast = (TypeInsnNode) insn;
                    if (IU_FLUID.equals(checkcast.desc)) {
                        // 跳过 Label/LineNumber/Frame 等伪指令（opcode=-1）——javap 行号
                        // 间隔实证：SpaceRegionalHydrologyFeature.resolveHydrologyFluid 的
                        // checkcast 与 invokevirtual 之间夹着 LineNumberNode，直接 getNext()
                        // 会漏改 invokevirtual owner，留下栈类型(FlowingFluid)与声明类
                        // (IUFluid)不匹配的调用 → VerifyError。
                        AbstractInsnNode next = insn.getNext();
                        while (next != null && next.getOpcode() == -1) {
                            next = next.getNext();
                        }
                        if (next != null && next.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            MethodInsnNode invoke = (MethodInsnNode) next;
                            if (IU_FLUID.equals(invoke.owner)) {
                                invoke.owner = FLOWING_FLUID;
                            }
                        }
                        checkcast.desc = FLOWING_FLUID;
                    }
                }
                insn = insn.getNext();
            }
        }
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
