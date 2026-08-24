package com.sxcccccccc.iufix.mixin;

import com.denfop.integration.jade.BlockComponentProvider;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.3-hotfix5：Jade provider 对 anvil.data null 的最小守卫。
 *
 * <p>现象：放置/加载铁砧后立即用 Jade 查看 → 服务端
 * {@code BlockComponentProvider.writeData(Level, BlockEntity, Player, CompoundTag)}
 * 第一处 {@code anvil.data.getOrDefault(player.getUUID(), 0.0)}（源码行 167，
 * 字节码偏移 105-118）NPE（"Cannot invoke Map.getOrDefault because anvil.data
 * is null"），Jade 捕获后打 "Caught unhandled exception" + 写 JadeErrorOutput.txt。
 *
 * <p>根因（字节码实证）：{@code BlockEntityAnvil.data}（public Map 字段）不在
 * 构造器初始化，而在 {@code onLoaded()}（源码行 181）里赋值；{@code onLoaded}
 * 被 {@code BlockEntityBase.onLoad()} 经 {@code TickHandlerIU.requestSingleWorldTick}
 * 调度到下一世界 tick 异步执行。因此任何刚放置/刚 NBT 加载的铁砧，在其 onLoaded
 * 执行前 data 恒为 null；Jade 请求（服务端 tick 队列）先到即 NPE。与放置位置
 * 是否合法无关。
 *
 * <p><b>hotfix4 为何失效（本次真正的修复点）</b>：上一版 @Redirect 的
 * {@code method = "writeData"} 只写了方法名、没有描述符。BlockComponentProvider
 * 里有两个 writeData 重载，且 javap 实证类文件中<b>私有 5 参客户端渲染版
 * {@code writeData(CompoundTag, ITooltip, BlockAccessor, IPluginConfig, IElementHelper)}
 * 排在公有 4 参服务端数据版之前</b>。Mixin 0.8.5 对无描述符的 method 目标按类文件
 * 顺序取<b>第一个</b>同名方法 → 注入落到私有客户端版上，而它内部没有任何
 * {@code Map.getOrDefault} 调用点（9 处全在公有服务端版里）→ 0 处注入。叠加
 * iufix.mixins.json 全局 {@code "injectors": {"defaultRequire": 0}}，0 命中完全不
 * 报错（无启动崩溃、无日志），于是 NPE 原样保留、肉眼看起来就是"hotfix4 没用"。
 *
 * <p>修复：method 写<b>完整方法描述符</b>（remap=false 原样进 jar、运行时按字节码
 * 精确匹配；denfop 类运行时就是官方名，见 javap 与堆栈）锁定公有服务端版
 * {@code writeData(Level, BlockEntity, Player, CompoundTag)}，其内 9 处
 * getOrDefault 调用点（BlockEntityAnvil / BlockEntityStrongAnvil /
 * BlockEntityCompressor / BlockEntityMacerator / BlockEntityPrimalWireInsulator /
 * BlockEntityRollingMachine / BlockEntityPrimalLaserPolisher / BlockEntitySqueezer /
 * BlockEntityDryer 分支）一次全守。receiver（anvil.data）为 null 时返回默认值
 * 0.0——不 NPE、不写日志、不刷屏；下一 tick onLoaded 执行后 data 就绪，Jade 自然
 * 正常显示。其余展示逻辑（wrench/durability/progress 等）原样保留。
 *
 * <p>require=1（显式覆盖全局 defaultRequire=0）：目标方法或调用点一旦匹配不上
 * 立即启动崩溃暴露，杜绝"静默失效"重演（hotfix4 的教训）。本注入 9 个调用点只
 * 要求 ≥1 命中，IU 3.4.0.10 实测命中 9/9。
 *
 * <p>不做「data 移进构造器」类重构（改 IU 初始化语义，未获用户批准）。
 * 注入串用 denfop 官方方法名（writeData）+ 接口 target
 * （java/util/Map.getOrDefault），remap=false；handler 为纯 Java 引用，无重映射需求。
 *
 * <p>target 写法坑（已实证 mixin 0.8.5 MemberInfo.parse 字节码）：@At target
 * 字符串 {@code Lowner;name(desc)} 的 name 与 desc 之间【不能有冒号】——解析器先
 * {@code indexOf('(')} 按左括号切分 desc，冒号会残留进 name 字段，随后 name 正则
 * {@code ^<?[\w\p{Sc}]+>?$} 校验失败报 "Invalid name: getOrDefault:"。javap 输出格式
 * （getOrDefault:(...)) 不可直接抄，须去掉方法名后的冒号。method 描述符同理按
 * javap 的完整声明抄、无冒号。
 */
@Mixin(value = BlockComponentProvider.class, remap = false)
public abstract class BlockComponentProviderJadeDataGuard {

    @Redirect(method = "writeData(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/nbt/CompoundTag;)V",
            require = 1,
            at = @At(value = "INVOKE",
                    target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object iufix$guardAnvilData(Map<Object, Object> map, Object key, Object defaultValue) {
        if (map == null) {
            return defaultValue;
        }
        return map.getOrDefault(key, defaultValue);
    }
}
