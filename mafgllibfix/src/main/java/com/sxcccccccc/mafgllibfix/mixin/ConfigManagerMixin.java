package com.sxcccccccc.mafgllibfix.mixin;

import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.config.IConfigHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * MaFgLib 0.1.14 defers all config loading to {@code loadAllConfigs()}, which runs
 * <em>after</em> every mod's {@code registerModHandlers()} has executed. Feature callbacks
 * constructed during {@code registerModHandlers()} (e.g. Tweakerge's
 * {@code FeatureCallbackGamma}) therefore see default config values on every launch and
 * never apply their "if enabled at launch" logic.
 *
 * <p>This injects an immediate {@code handler.load()} after registration, restoring the
 * upstream malilib semantics: configs are loaded when registered, which happens at the
 * start of {@code registerModHandlers()}, before the callbacks are created. The later
 * {@code loadAllConfigs()} re-loads the same files and is harmless.</p>
 */
@Mixin(value = ConfigManager.class, remap = false)
public abstract class ConfigManagerMixin {

    @Inject(method = "registerConfigHandler", at = @At("TAIL"), remap = false)
    private void mafgllibfix$loadOnRegister(String modId, IConfigHandler handler, CallbackInfo ci) {
        handler.load();
    }
}
