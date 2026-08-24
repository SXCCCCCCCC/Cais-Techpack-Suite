package com.sxcccccccc.iuunify;

/**
 * 注册阶段标志。
 *
 * <p>由 {@code mixin.DeferredRegisterPhaseMixin} 在 Forge
 * {@code DeferredRegister.addEntries(RegisterEvent)} 的 HEAD/RETURN 置位/复位：
 * 该窗口 = IU 全部注册条目（含 iudebug 对 12 流体各 5 条目的跳过）写入注册表的时刻。
 *
 * <p>{@code FluidNameRedirectMixin} 在此窗口内透传原始 RegistryObject，注册链不被
 * 重定向干扰；窗口外才把 12 个流体的 getInstance() 换为 ic2_120 包装 RegistryObject。
 *
 * <p>铁律（见 mc-bugfix-sop §5）：mixin 包内只许放 mixin 类，helper 必须在 mixin
 * 包外——本类放根包。
 */
public final class RegistrationPhaseGuard {

    private static volatile boolean registrationActive = false;

    private RegistrationPhaseGuard() {
    }

    public static boolean isRegistrationActive() {
        return registrationActive;
    }

    public static void setRegistrationActive(boolean active) {
        registrationActive = active;
    }
}
