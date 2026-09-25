package com.sxcccccccc.iufix.mixin;

import com.denfop.blocks.state.State;
import com.denfop.blocks.state.TypeProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 1.3.9 重构：IU "type" 属性在 WE 边界以**名字字符串**往返（无损耗），仿原版
 * EnumProperty 的 m_7912_/m_6215_ 字符串往返模式。替换 1.3.6 的 skip-on-IAE 近似与
 * 1.3.8 的 first-combo 回退（两者都把错误静默变成错误状态，1.3.8 曾致碰撞形状代理
 * ClassCastException 崩溃，见 crash-2026-08-23_15.11.31）。
 *
 * <p>根因（日志实证，设计文档 we_state_fix_design.md）：
 * <ul>
 *   <li>IU 的 {@code BlockTileEntity.create()} 用静态 currentTypeProperty 在构造期间
 *       暂存 TypeProperty（BlockTileEntity.java 133-160 行），m_7926_ 经 getTypeProperty()
 *       读静态装进 stateDefinition（356-363 行）；注册期并发/重入交错会让某些方块的
 *       stateDefinition 装入**别的方块**的 TypeProperty（2026-08-23-3 日志：
 *       electric_squeezer 的 stateDefinition 里是 graphite_reactor/graphite_controller
 *       的 TypeProperty，setValue 拒绝 "not an allowed value"）。</li>
 *   <li>WE 原本把这种属性包成 IPropertyAdapter，快照**原始 State 对象**（内容 =
 *       teBlock 实例引用 + state 字符串），而 WE BlockType 与 MC stateDefinition 解析到的
 *       TypeProperty 可能不是同一实例 → 值域不一致 → 启动期 PlatformReadyEvent
 *       setValue 拒绝（每把必现）、运行时 //regen BlockType.getState 查空
 *       "no state for"（双向崩溃）。</li>
 *   <li>TypeProperty 自带与属性实例无关的字符串编解码：getName(State) =
 *       teBlock.getName() [+ "_" + state]，allowedValues 成员间名字唯一。世界状态的
 *       type 值必是 stateDefinition 该属性 allowedValues 的成员，其名字串必能被同一
 *       属性的名字匹配解析回**同一个成员实例**——污染与否都成立。字符串因此是
 *       MC→WE→MC 无损耗往返的载体（EnumProperty 同款模式，WE 内部字符串值路径已被
 *       原版枚举属性证明可用）。</li>
 * </ul>
 *
 * <p>实现：三个钩子全部落在 ForgeTransmogrifier（WE 侧单收口）：
 * <ol>
 *   <li>{@code transmogToWorldEditProperty} HEAD：MC 属性是 TypeProperty 时返回
 *       WE 自带 EnumProperty("type", 名字串列表)（值 = allowedValues 各成员
 *       getName，内容派生，与属性实例无关）；否则原逻辑（PROPERTY_CACHE → 类型化
 *       adapter / IPropertyAdapter）不变。</li>
 *   <li>{@code transmogToWorldEditProperties} @Redirect 循环内唯一 Map.put：值是
 *       State 时改为其名字字符串（与 TypeProperty.getName 同式）。Direction/Enum
 *       分支在 put 前已被原逻辑处理，不受影响。</li>
 *   <li>{@code transmogToMinecraftProperties} HEAD 重写循环：TypeProperty 条目 +
 *       String 值 → 在 stateDefinition.m_61081_("type") 返回的属性（不管是不是被污染
 *       的实例）的 allowedValues 里按 getName 匹配解析回成员 → setValue；解析不到
 *       则跳过该条目（保默认，防御性，正常流必命中）。Direction/Enum 分支复刻原逻辑；
 *       兜底 catch(IllegalArgumentException) 跳过（防御其他异常属性）。</li>
 * </ol>
 * 同时删除 BlockTypeIUCompatMixin（first-combo 回退）：字符串往返后 WE 内部查空按构造
 * 不可能发生，回退只会在意外时把错误静默变成错误状态——恢复严格 checkArgument 语义。
 *
 * <p>remap=false：target 串按运行时官方名原样进 jar（WE Forge 类官方名字节码）。
 * 引用 IU 成员只用官方名（TypeProperty.allowedValues 公开字段、TypeProperty.getName、
 * State.teBlock/state、MultiBlockEntity.getName）；MC 引用由 reobf/refmap 统一重映射。
 */
@Mixin(targets = "com.sk89q.worldedit.forge.internal.ForgeTransmogrifier", remap = false)
public abstract class ForgeTransmogrifierIUCompatMixin {

    /**
     * 钩子 1：MC 属性 → WE 属性。TypeProperty 换成字符串值属性（WE 自带 EnumProperty，
     * 与属性实例无关），不再落 IPropertyAdapter 原始值路径。
     */
    @Inject(method = "transmogToWorldEditProperty",
            at = @At("HEAD"), cancellable = true)
    private static void iufix$transmogToWorldEditProperty(
            Property<?> property,
            CallbackInfoReturnable<com.sk89q.worldedit.registry.state.Property<?>> cir) {
        if (property instanceof TypeProperty) {
            TypeProperty tp = (TypeProperty) property;
            List<String> names = new ArrayList<>(tp.allowedValues.size());
            for (State s : tp.allowedValues) {
                names.add(tp.getName(s));
            }
            cir.setReturnValue(new com.sk89q.worldedit.registry.state.EnumProperty("type", names));
        }
    }

    /**
     * 钩子 2：MC→WE 值转换。循环里唯一的 Map.put 被重定向：值是 State（只可能来自
     * IU "type" 属性）时改为名字字符串（与 TypeProperty.getName 同式）。
     */
    @Redirect(method = "transmogToWorldEditProperties",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object iufix$putStateName(
            Map map, Object key, Object value) {
        if (value instanceof State) {
            State s = (State) value;
            value = s.state.isEmpty() ? s.teBlock.getName() : s.teBlock.getName() + "_" + s.state;
        }
        return map.put(key, value);
    }

    /**
     * 钩子 3：WE→MC 值解析。重写循环：TypeProperty 条目 + String 值 → 按名解析回
     * stateDefinition 该属性 allowedValues 的成员实例 → setValue（无损耗的关键）。
     * Direction/Enum 分支复刻原逻辑；异常属性走兜底跳过。
     */
    @Inject(method = "transmogToMinecraftProperties", at = @At("HEAD"), cancellable = true)
    private static void iufix$transmogToMinecraftProperties(
            StateDefinition<?, ?> stateDefinition,
            BlockState state,
            Map<com.sk89q.worldedit.registry.state.Property<?>, Object> properties,
            CallbackInfoReturnable<BlockState> cir) {

        BlockState result = state;
        for (Map.Entry<com.sk89q.worldedit.registry.state.Property<?>, Object> entry : properties.entrySet()) {
            com.sk89q.worldedit.registry.state.Property<?> weProp = entry.getKey();
            Property<?> mcProp = stateDefinition.getProperty(weProp.getName());
            Object value = entry.getValue();

            if (mcProp instanceof TypeProperty) {
                // IU "type"：字符串往返。值串在 stateDefinition 该属性自己的
                // allowedValues 里按 getName 匹配解析（内容派生，与属性实例无关）。
                if (value instanceof String) {
                    TypeProperty tp = (TypeProperty) mcProp;
                    State resolved = null;
                    for (State s : tp.allowedValues) {
                        if (tp.getName(s).equals(value)) {
                            resolved = s;
                            break;
                        }
                    }
                    if (resolved != null) {
                        try {
                            result = result.setValue(tp, resolved);
                        } catch (IllegalArgumentException e) {
                            // 防御网（正常流不触发）
                        }
                    }
                    // 解析不到 → 跳过该条目（保默认值）
                }
                continue;
            }

            if (mcProp == null) {
                continue;
            }
            Comparable<?> comparable;
            if (mcProp instanceof DirectionProperty) {
                comparable = com.sk89q.worldedit.forge.ForgeAdapter
                        .adapt((com.sk89q.worldedit.util.Direction) value);
            } else if (mcProp instanceof EnumProperty) {
                final String rawValue = (String) value;
                final String propName = weProp.getName();
                java.util.Optional<?> resolved = ((EnumProperty) mcProp).getValue(rawValue);
                if (!resolved.isPresent()) {
                    throw new IllegalStateException(
                            "Unknown value '" + rawValue + "' for property '" + propName + "'");
                }
                comparable = (Comparable<?>) resolved.get();
            } else {
                comparable = (Comparable<?>) value;
            }
            try {
                result = result.setValue((Property) mcProp, (Comparable) comparable);
            } catch (IllegalArgumentException e) {
                // 防御网：跳过该条目（正常流不触发）
            }
        }
        cir.setReturnValue(result);
    }
}
