package com.sxcccccccc.iufix.mixin;

import com.sk89q.worldedit.registry.state.Property;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

/**
 * 1.3.6 主修：治 //regen 崩（BlockType.getState 的 "no state for %s" checkArgument）。
 *
 * <p>字节码实证的崩溃链（regen 栈 16554-16598）：
 * {@code ForgeAdapter.adapt(MC BlockState)} 先查 {@code Block.getId} → WE ID 缓存
 * （BlockStateIdAccess），miss 时回退 {@code ForgeTransmogrifier.transmogToWorldEdit} →
 * {@code transmogToWorldEditProperties}（正向；propertyMap 含 "type"——ForgeBlockRegistry.
 * getProperties 按 defaultBlockState 属性名 put——所以 getProperty("type") 命中 adapter 不崩）
 * → {@code BlockType.getState(props)} → {@code BlockState.generateStateMap} 按各属性
 * {@code getValues()} 笛卡尔积预生成组合（含 "type" 维度）→ {@code ImmutableMap.get(keyMap)}
 * 用 Map.equals/hashCode 匹配（value 参与 equals：IU State 的 equals/hashCode 含运行时
 * teBlock 实例引用）→ 运行时 value 实例与生成时的 allowedValues 快照实例不一致 → 查空 →
 * {@code Preconditions.checkArgument} 抛 "no state for %s"。
 *
 * <p>修复：@Redirect {@code getState} 里唯一的 {@code Map.get} 调用点，查空时**回退
 * getDefaultState()**（= generateStateMap 组合的第一个状态，computeDefaultState 实证非
 * null、无递归），不再走 checkArgument。WE 注册表构建（启动期 PlatformReadyEvent）对
 * IU 方块状态枚举即完整，ID 缓存齐全后 //regen 的 adapt 直接命中缓存，连正向转译都不走。
 *
 * <p>1.3.7：handler 返回类型必须是 {@code Object}——@Redirect 的 handler 返回类型必须等于
 * 被替换指令的返回类型（{@code Map.get:(Object)Object} 返回 Object）；1.3.6 误写成
 * BlockState 触发 {@code InvalidInjectionException}（mixin APPLY 期校验拒绝），required:true
 * 连坐整个 iufix 加载失败。方法体内 getDefaultState()（BlockState）作为 Object 返回，
 * 调用点后续有 checkcast BlockState，安全。
 *
 * <p>1.3.8：回退值弃用 getDefaultState()——1.3.7 实测启动 StackOverflowError。递归根因：
 * computeDefaultState（getDefaultState 的 LazyReference supplier）里 {@code values.apply()}
 * 调的是 ForgeWorldEdit.setupRegistries 传入 BlockType 构造的 factory lambda
 * （ForgeWorldEdit.java:182），该 lambda 内部走完整 {@code ForgeAdapter.adapt(MC→WE) →
 * transmogToWorldEdit → BlockType.getState} 转译链 → 对 IU 状态再次查空 → 回退
 * getDefaultState() → LazyReference 重入（computeDefaultState 尚未返回）→ 无限递归。
 * 任何经 getDefaultState() 的回退都是递归陷阱（computeDefaultState 本身就是转译链一环）。
 *
 * <p>回退改为直接取组合 map（handler 的 map 参数 = 已求值的 getBlockStatesMap() 返回值，
 * 非 LazyReference 调用点）第一个条目：generateStateMap 尾部有 fallback 分支（组合为空时
 * 仍 {@code ImmutableMap.of(空 keyMap, new BlockState(type))}），map 保证至少一个合法
 * BlockState；取首个即 computeDefaultState 原来拿的那个状态，无递归。空 map 仅理论
 * 不可达（防御性返回 null 保持原 checkArgument 崩溃语义）。
 *
 * <p>remap=false：WE 是 Forge 类（官方名字节码），method/target 串按运行时名原样进 jar。
 */
@Mixin(value = BlockType.class, remap = false)
public abstract class BlockTypeIUCompatMixin {

    @Redirect(method = "getState",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object iufix$stateLookup(Map<Map<Property<?>, Object>, BlockState> map, Object key) {
        BlockState found = map.get(key);
        if (found != null) {
            return found;
        }
        // 查空回退：取组合 map 第一个状态（generateStateMap 保证非空），不调 getDefaultState()。
        for (BlockState state : map.values()) {
            return state;
        }
        return null;
    }
}
