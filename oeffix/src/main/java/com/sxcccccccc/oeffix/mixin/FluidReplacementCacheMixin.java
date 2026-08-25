package com.sxcccccccc.oeffix.mixin;

import com.flechazo.oneenoughfluid.Oneenoughfluid;
import com.flechazo.oneenoughfluid.init.FluidReplacementCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * 修 OEF {@link FluidReplacementCache#beginReloadOverride(Map)} 的 NPE。
 *
 * <p>反编译证据（OEF 1.1.1-hotfix，CFR + javap 字节码）——原实现顺序：
 * <pre>
 *   if (map == null || map.isEmpty()) { ReloadOverrideFluidMap = null; return; }
 *   LOGGER.info("Enabled reload-override mapping for this resource reload: {} fluid",
 *               ReloadOverrideFluidMap.size());   // ← 此时字段仍为 null → NPE
 *   ReloadOverrideFluidMap = new HashMap&lt;&gt;(currentFluidMap);   // 赋值反而在后
 * </pre>
 * OEI 同函数（ItemReplacementCache）语义是"先赋值后打日志"，本 mixin 照抄该语义，
 * 相当于把两行对调。
 */
@Mixin(FluidReplacementCache.class)
public abstract class FluidReplacementCacheMixin {

    @Shadow(remap = false)
    private static volatile Map<String, String> ReloadOverrideFluidMap;

    @Inject(
            method = "beginReloadOverride(Ljava/util/Map;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void oeffix$beginReloadOverride(Map<String, String> currentFluidMap, CallbackInfo ci) {
        if (currentFluidMap == null || currentFluidMap.isEmpty()) {
            ReloadOverrideFluidMap = null;
        } else {
            ReloadOverrideFluidMap = new HashMap<>(currentFluidMap);
            Oneenoughfluid.LOGGER.info(
                    "Enabled reload-override mapping for this resource reload: {} fluid",
                    ReloadOverrideFluidMap.size());
        }
        ci.cancel();
    }
}
