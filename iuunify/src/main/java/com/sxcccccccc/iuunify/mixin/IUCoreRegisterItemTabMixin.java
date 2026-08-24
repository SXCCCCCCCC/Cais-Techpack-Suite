package com.sxcccccccc.iuunify.mixin;

import com.denfop.IUCore;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 创造菜单兼容：IU 创造 tab 填充（{@code IUCore.registerItemTab}）遍历
 * {@code DataItem.objects / DataBlock.objects / DataBlockEntity.objects} 里全部
 * {@code RegistryObject}，对每个无条件 {@code .get()}——被 iudebug 删除注册的
 * 12 个流体桶 RO 永不填充 value，get() 抛 "Registry Object not present"
 * （javap 实证 {@code RegistryObject.get()} 反编译为 requireNonNull 检查），
 * 打开创造菜单即崩。
 *
 * <p>修复：@Redirect 该方法内全部 {@code RegistryObject.get()} 调用点，
 * handler 用 {@code isPresent()} 守卫，未注册条目返回 null：
 * <ul>
 *   <li>{@code object.get() instanceof IItemTab} → null instanceof 为 false，
 *       被删条目（桶）跳过填充——只影响被删除流体桶的展示，其余不动；</li>
 *   <li>循环尾 {@code object == null || object.get() == null} 打印分支照旧
 *       走 null 路径（原逻辑本就有 null 兜底打印）。</li>
 * </ul>
 *
 * <p>目标签名（发布 jar javap 实证，Forge 方法保留可读名）：
 * {@code public void registerItemTab(net.minecraftforge.event.BuildCreativeModeTabContentsEvent)}
 * 方法内 {@code invokevirtual RegistryObject.get:()Ljava/lang/Object;} 共 4 处
 * 调用点，全部替换，行为一致；require=1 保证方法串/目标串写错时启动即崩
 * （显式反馈，杜绝静默失效）。
 *
 * <p>0.3.1 修复（2026-08-24 实崩复盘，mixin-0.8.5
 * {@code Injector.checkTargetModifiers} 反编译实证）：handler 必须是
 * <b>非 static</b>——Mixin 要求 handler 的 static 修饰符与<b>被注入方法</b>
 * 的 static 修饰符一致（{@code registerItemTab} 是实例方法）。0.3.0 里写成
 * static → InvalidInjectionException → IUCore 类加载失败 → IU 容器 broken →
 * ModList 容器初始化失败 → Timer 构造时 carpet CarpetSettings.<clinit> 查
 * ModList.getModContainerById("carpet") 拿空 → NoSuchElementException 连锁崩。
 * handler 合并进 IUCore 成为实例方法后，{@code RegistryObject} 参数即被
 * 重定向调用的 receiver，this 自动绑定（参照 iufix ScreenVeinSensorMixin：
 * 实例方法 @Redirect 一律非 static handler）。
 */
@Mixin(value = IUCore.class, remap = false)
public abstract class IUCoreRegisterItemTabMixin {

    @Redirect(
            method = "registerItemTab(Lnet/minecraftforge/event/BuildCreativeModeTabContentsEvent;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraftforge/registries/RegistryObject;get()Ljava/lang/Object;"),
            require = 1,
            remap = false
    )
    private Object iuunify$safeRegistryObjectGet(RegistryObject<?> registryObject) {
        return registryObject.isPresent() ? registryObject.get() : null;
    }
}
