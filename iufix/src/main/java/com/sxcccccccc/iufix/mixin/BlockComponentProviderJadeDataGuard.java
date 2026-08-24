package com.sxcccccccc.iufix.mixin;

import com.denfop.integration.jade.BlockComponentProvider;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.3-hotfix4：Jade provider 对 anvil.data null 的最小守卫（用户裁决）。
 *
 * <p>现象：放置/加载铁砧后立即用 Jade 查看 → 服务端
 * {@code BlockComponentProvider.writeData:167} NPE
 * （"Cannot invoke Map.getOrDefault because anvil.data is null"），Jade 捕获后打
 * Caught unhandled exception + 写 JadeErrorOutput.txt。
 *
 * <p>根因（源码证据）：{@code BlockEntityAnvil.data}（public Map 字段）不在构造器
 * 初始化，而在 {@code onLoaded()}（行 181）里赋值；{@code onLoaded} 被
 * {@code BlockEntityBase.onLoad()}（行 437-445）经
 * {@code TickHandlerIU.requestSingleWorldTick} 调度到下一世界 tick 异步执行。
 * 因此任何刚放置/刚 NBT 加载的铁砧，在其 onLoaded 执行前 data 恒为 null；
 * Jade 请求（服务端 tick 队列）先到即 NPE。与放置位置是否合法无关。
 *
 * <p>修复：@Redirect {@code writeData(Level, BlockEntity, Player, CompoundTag)}
 * 内全部 {@code Map.getOrDefault} 调用点（同文件同模式共 9 处：BlockEntityAnvil /
 * BlockEntityStrongAnvil / BlockEntityCompressor / BlockEntityMacerator /
 * BlockEntityPrimalWireInsulator / BlockEntityRollingMachine /
 * BlockEntityPrimalLaserPolisher / BlockEntitySqueezer / BlockEntityDryer 分支），
 * receiver（anvil.data）为 null 时返回默认值 0.0——不 NPE、不写日志、不刷屏；
 * 下一 tick onLoaded 执行后 data 就绪，Jade 自然正常显示。其余展示逻辑
 * （wrench/durability/progress 等）原样保留。
 *
 * <p>不做「data 移进构造器」类重构（改 IU 初始化语义，未获用户批准）。
 * 注入串用 denfop 官方方法名（writeData）+ 接口 target
 * （java/util/Map.getOrDefault），remap=false；handler 为纯 Java 引用，无重映射需求。
 *
 * <p>target 写法坑（已实证 mixin 0.8.5 MemberInfo.parse 字节码）：target 字符串
 * {@code Lowner;name(desc)} 的 name 与 desc 之间【不能有冒号】——解析器先
 * {@code indexOf('(')} 按左括号切分 desc，冒号会残留进 name 字段，随后 name 正则
 * {@code ^<?[\w\p{Sc}]+>?$} 校验失败报 "Invalid name: getOrDefault:"。javap 输出格式
 * （getOrDefault:(...)) 不可直接抄，须去掉方法名后的冒号。
 */
@Mixin(value = BlockComponentProvider.class, remap = false)
public abstract class BlockComponentProviderJadeDataGuard {

    @Redirect(method = "writeData",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object iufix$guardAnvilData(Map<Object, Object> map, Object key, Object defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        return map.getOrDefault(key, defaultValue);
    }
}
