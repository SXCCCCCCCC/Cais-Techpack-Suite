package com.sxcccccccc.iuunify.mixin;

import com.sxcccccccc.iuunify.RegistrationPhaseGuard;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegisterEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 注册阶段守卫：把 {@code DeferredRegister.addEntries(RegisterEvent)} 执行窗口标记到
 * {@link RegistrationPhaseGuard}，供 {@link FluidNameRedirectMixin} 判定"注册阶段内
 * 透传、注册阶段外重定向"。
 *
 * <p>注入点（字节码实证，forge-1.20.1-47.4.10-universal.jar javap）：
 * {@code DeferredRegister.addEntries(RegisterEvent)} 是 Forge 全部 DeferredRegister
 * 条目写入注册表的唯一路径（private 单方法、无重载）；iudebug 的
 * {@code DeferredRegisterBanMixin} 在同一方法内 @Redirect 那唯一一处
 * {@code RegisterEvent.register(...)} 调用——本 mixin 的 @Inject HEAD/RETURN 与它
 * 注入位置不同（方法边界 vs 调用点），互不冲突（无同调用点 redirector 争抢）。
 *
 * <p>为什么必须按"注册阶段"守卫而不是直接永久重定向（字节码证据链）：
 * <ul>
 *   <li>{@code RegistryObject.get()} 反编译 = {@code Objects.requireNonNull(value, ...)}
 *       （"Registry Object not present"），value 由 {@code updateReference} 在注册表
 *       冻结时填充——注册阶段内 12 流体的原始 RO 尚未填充（且被 iudebug 跳过 = 永
 *       不填充）；若注册阶段内有人 getInstance() 并立即 .get()，透传原始 RO 抛 NPE
 *       （与 iudebug 删除语义一致：崩溃=引用未覆盖反馈），而不是让 IC2R 流体对象
 *       泄漏进 IU 注册链；</li>
 *   <li>IC2R 流体的注册时机（Connector 桥接进 Forge 注册表）不可与 IU 的
 *       addEntries 次序对齐——注册阶段内重定向可能拿到未注册的 ic2_120 id。</li>
 * </ul>
 * 注册阶段内 IU 自身代码对 getInstance() 零调用（Register.java 全文件 0 处、
 * FluidHandler 0 处、dataregistry 0 处、CropInit 0 处、registerContent→writeItems 0
 * 处——逐文件 grep 实证），因此守卫在正常路径上从不触发透传分支，纯兜底。
 */
@Mixin(value = DeferredRegister.class, remap = false)
public abstract class DeferredRegisterPhaseMixin {

    @Inject(method = "addEntries", at = @At("HEAD"), require = 1, remap = false)
    private void iuunify$phaseStart(RegisterEvent event, CallbackInfo ci) {
        RegistrationPhaseGuard.setRegistrationActive(true);
    }

    @Inject(method = "addEntries", at = @At("RETURN"), require = 1, remap = false)
    private void iuunify$phaseEnd(RegisterEvent event, CallbackInfo ci) {
        RegistrationPhaseGuard.setRegistrationActive(false);
    }
}
