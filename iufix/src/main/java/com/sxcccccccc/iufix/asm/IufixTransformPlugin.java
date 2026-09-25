package com.sxcccccccc.iufix.asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 升级模块构造器桶强转崩溃的 ASM 修复插件（2026-09-09 用户报告 + 用户裁决
 * "用真正的 orElseGet + lambda 修复"）。
 *
 * <p><b>背景</b>：{@code ItemStackUpgradeModules} 构造器
 * （items/ItemStackUpgradeModules.java:57）
 * {@code getCapability(FLUID_HANDLER_ITEM).orElse((CapabilityFluidHandlerItem)
 * initCapabilities(...))}——orElse 参数立即求值：原版桶的 initCapabilities 返回
 * FluidBucketWrapper，checkcast 执行即抛 ClassCastException（latest.log 12:00:01
 * 实崩复盘）。mixin 三重手段均够不到构造器内部（@Inject 仅 RETURN、@Redirect
 * 禁构造器目标、@Overwrite 构造器被 AP 拒绝——1.9.7 实崩复盘），故按用户裁决
 * 走 ASM。1.9.7 前两版（checkcast 单改 / safeHandler 静态桥）经用户否决
 * （静态桥"不是等效方法"且实测崩服），本版为<b>真实 orElseGet + lambda</b>。
 *
 * <p><b>字节码实证</b>（发布 jar javap -c -l -p，构造器 197-246）：
 * <pre>
 *   getCapability:(Capability;Direction)LazyOptional;   // → LazyOptional 入栈
 *   ... list[i].getItem() / list[i].getTag() / initCapabilities → ICapabilityProvider
 *   checkcast CapabilityFluidHandlerItem    ← 崩点
 *   LazyOptional.orElse:(Object)Object
 *   checkcast IFluidHandlerItem → astore 4
 * </pre>
 * 循环变量 i = 局部变量槽 3（istore_3 @178 复用）。javac 实证 lambda 形态
 * （本地 T.java 编译）：捕获 this+int 的 lambda，invokedynamic 描述符
 * {@code (LReceiver;I)LSupplier;}（receiver 也是调用点实参，压 [this, i]），
 * 合成方法 {@code private synthetic (I)Object}，impl handle REF_invokeVirtual。
 * LazyOptional.orElseGet 的参数是 Forge 的
 * {@link net.minecraftforge.common.util.NonNullSupplier}（SAM {@code T get()}，
 * 擦除 {@code ()Object}，javap 实证），非 java.util.function.Supplier。
 *
 * <p><b>修复</b>：删 getCapability 之后到 orElse（含）的整段，插入：
 * <pre>
 *   aload_0 / iload_3
 *   invokedynamic get:(LItemStackUpgradeModules;I)LNonNullSupplier;
 *       bsm=LambdaMetafactory.metafactory,
 *       args=[()Object, REF_invokeVirtual 合成方法:(I)Object, ()Object]
 *   invokevirtual LazyOptional.orElseGet:(LNonNullSupplier;)Object;
 * </pre>
 * 合成方法 {@code iuunify$lambda$orElseGet$0(I)Object}：照抄原段字节码
 * （list[i].getItem().initCapabilities(list[i], list[i].getTag())），仅
 * checkcast 目标改为接口 IFluidHandlerItem（桶的 FluidBucketWrapper implements
 * 之 → 转换成功；null 合法；getCapability 命中时 lambda 恒不被调用——惰性语义
 * 与 orElseGet 完全一致）。后续 checkcast IFluidHandlerItem 为 no-op、ASTORE
 * 槽类型一致，帧由 mixin 应用器 COMPUTE_FRAMES 重算（iuunify
 * IuUnifyTransformPlugin 先例注释实证）。
 *
 * <p><b>触发锚点</b>：preApply 只在目标类有 mixin 应用时被调用——iufix 提供
 * 空锚 mixin {@code ItemStackUpgradeModulesCtorFixMixin}（mixins.json 登记）
 * 保证本类被 mixin 处理、preApply 触发。
 */
public class IufixTransformPlugin implements IMixinConfigPlugin {

    // preApply 的 targetClassName 参数是点分隔类名（javadoc/运行时实证），
    // 不是 ASM 内部名——0.9.0 初版写成斜杠形式，比对永远不匹配，preApply
    // 静默跳过（2026-09-09 崩溃复盘：日志无 preApply 行、CCE 依旧）。
    private static final String TARGET = "com.denfop.items.ItemStackUpgradeModules";
    private static final String TARGET_INTERNAL = "com/denfop/items/ItemStackUpgradeModules";
    private static final String LAMBDA_NAME = "iuunify$lambda$orElseGet$0";
    private static final String LAMBDA_DESC = "(I)Ljava/lang/Object;";
    private static final String INDY_DESC =
            "(Lcom/denfop/items/ItemStackUpgradeModules;I)Lnet/minecraftforge/common/util/NonNullSupplier;";
    private static final String SAM_DESC = "()Ljava/lang/Object;";
    private static final String GET_CAP_DESC =
            "(Lnet/minecraftforge/common/capabilities/Capability;Lnet/minecraft/core/Direction;)"
                    + "Lnet/minecraftforge/common/util/LazyOptional;";
    private static final String OR_ELSE_DESC = "(Ljava/lang/Object;)Ljava/lang/Object;";
    private static final String OR_ELSE_GET_DESC =
            "(Lnet/minecraftforge/common/util/NonNullSupplier;)Ljava/lang/Object;";

    private static final Handle METAFACTORY = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                    + "Ljava/lang/invoke/CallSite;",
            false
    );
    private static final Handle IMPL_HANDLE = new Handle(
            Opcodes.H_INVOKEVIRTUAL, TARGET_INTERNAL, LAMBDA_NAME, LAMBDA_DESC, false
    );
    private static final Type SAM_TYPE = Type.getType(SAM_DESC);

    @Override
    public void onLoad(String mixinPackage) {
        System.out.println("[iufix-asm] IufixTransformPlugin.onLoad (mixinPackage=" + mixinPackage + ")");
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
        if (!TARGET.equals(targetClassName)) {
            return;
        }
        for (MethodNode method : targetClass.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            // 定位 getCapability 与紧随其后的 orElse（构造器内唯一一对）
            MethodInsnNode getCap = null;
            MethodInsnNode orElse = null;
            for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode invoke = (MethodInsnNode) insn;
                    if (getCap == null
                            && "net/minecraft/world/item/ItemStack".equals(invoke.owner)
                            && "getCapability".equals(invoke.name)
                            && GET_CAP_DESC.equals(invoke.desc)) {
                        getCap = invoke;
                    } else if (getCap != null && orElse == null
                            && "net/minecraftforge/common/util/LazyOptional".equals(invoke.owner)
                            && "orElse".equals(invoke.name)
                            && OR_ELSE_DESC.equals(invoke.desc)) {
                        orElse = invoke;
                        break;
                    }
                }
            }
            if (getCap == null || orElse == null) {
                System.out.println("[iufix-asm] preApply " + targetClassName
                        + ": pattern not found (getCap=" + (getCap != null) + ", orElse=" + (orElse != null) + ")");
                continue;
            }
            // 删除 getCapability 之后（不含）到 orElse（含）之间的整段
            AbstractInsnNode afterOrElse = orElse.getNext();
            AbstractInsnNode cur = getCap.getNext();
            while (cur != null && cur != afterOrElse) {
                AbstractInsnNode next = cur.getNext();
                method.instructions.remove(cur);
                cur = next;
            }
            // 插入：aload_0 / iload_3 / invokedynamic / LazyOptional.orElseGet
            InsnList insert = new InsnList();
            insert.add(new VarInsnNode(Opcodes.ALOAD, 0));
            insert.add(new VarInsnNode(Opcodes.ILOAD, 3));
            insert.add(new InvokeDynamicInsnNode("get", INDY_DESC, METAFACTORY, SAM_TYPE, IMPL_HANDLE, SAM_TYPE));
            insert.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraftforge/common/util/LazyOptional",
                    "orElseGet",
                    OR_ELSE_GET_DESC,
                    false
            ));
            method.instructions.insert(getCap, insert);
            System.out.println("[iufix-asm] preApply " + targetClassName + " <init>: orElse -> orElseGet + lambda OK");
        }
        addLambdaMethod(targetClass);
    }

    /** 合成方法 iuunify$lambda$orElseGet$0(I)Object：照抄原段字节码，checkcast 改接口。 */
    private static void addLambdaMethod(ClassNode targetClass) {
        MethodNode lambda = new MethodNode(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, LAMBDA_NAME, LAMBDA_DESC, null, null
        );
        InsnList body = new InsnList();
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL, "list", "[Lnet/minecraft/world/item/ItemStack;"));
        body.add(new VarInsnNode(Opcodes.ILOAD, 1));
        body.add(new InsnNode(Opcodes.AALOAD));
        body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack", "m_41720_", "()Lnet/minecraft/world/item/Item;", false));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL, "list", "[Lnet/minecraft/world/item/ItemStack;"));
        body.add(new VarInsnNode(Opcodes.ILOAD, 1));
        body.add(new InsnNode(Opcodes.AALOAD));
        body.add(new VarInsnNode(Opcodes.ALOAD, 0));
        body.add(new FieldInsnNode(Opcodes.GETFIELD, TARGET_INTERNAL, "list", "[Lnet/minecraft/world/item/ItemStack;"));
        body.add(new VarInsnNode(Opcodes.ILOAD, 1));
        body.add(new InsnNode(Opcodes.AALOAD));
        body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/ItemStack", "m_41783_", "()Lnet/minecraft/nbt/CompoundTag;", false));
        body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/item/Item", "initCapabilities",
                "(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/nbt/CompoundTag;)"
                        + "Lnet/minecraftforge/common/capabilities/ICapabilityProvider;",
                false));
        body.add(new TypeInsnNode(Opcodes.CHECKCAST, "net/minecraftforge/fluids/capability/IFluidHandlerItem"));
        body.add(new InsnNode(Opcodes.ARETURN));
        lambda.instructions.add(body);
        // maxStack/maxLocals=0：由 mixin 应用器 COMPUTE_FRAMES 重算（iuunify 先例注释实证）
        lambda.maxStack = 0;
        lambda.maxLocals = 0;
        targetClass.methods.add(lambda);
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
