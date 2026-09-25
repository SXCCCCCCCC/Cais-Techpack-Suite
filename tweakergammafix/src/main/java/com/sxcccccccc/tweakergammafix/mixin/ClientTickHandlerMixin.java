package com.sxcccccccc.tweakergammafix.mixin;

import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.config.FeatureToggle;
import fi.dy.masa.tweakeroo.event.ClientTickHandler;
import fi.dy.masa.tweakeroo.mixin.IMixinSimpleOption;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tweakerge 0.1.5 (mafglib 0.1.14) loads config files AFTER {@code registerModHandlers()}
 * has already run, so {@code FeatureCallbackGamma}'s constructor sees the toggle as its
 * default value (false) on every restart and never applies the override. Manual re-toggle
 * works because that goes through {@code onValueChanged}.
 *
 * <p>This mixin re-applies the override on the first client tick where the toggle reads
 * true and gamma is still in vanilla range (i.e. the override has not been applied yet),
 * mirroring what the callback constructor does. It is idempotent: once the override value
 * (&gt; 1.0) is in the option, later ticks skip it.</p>
 */
@Mixin(value = ClientTickHandler.class, remap = false)
public abstract class ClientTickHandlerMixin {

    @Unique
    private static boolean tweakergammafix$applied;

    @Inject(method = "onClientTick", at = @At("HEAD"), remap = false)
    private void tweakergammafix$applyOverrideOnStartup(Minecraft mc, CallbackInfo ci) {
        if (tweakergammafix$applied) {
            return;
        }
        // Config may not be loaded yet on the very first tick; don't give up in that case.
        if (!FeatureToggle.TWEAK_GAMMA_OVERRIDE.getBooleanValue()) {
            return;
        }
        OptionInstance<Double> gamma = mc.options.gamma();
        double current = gamma.get();
        if (current > 1.0) {
            // Override already applied (e.g. re-toggled manually before the first tick).
            tweakergammafix$applied = true;
            return;
        }
        tweakergammafix$applied = true;
        Configs.Internal.GAMMA_VALUE_ORIGINAL.setDoubleValue(current);
        ((IMixinSimpleOption<Double>) (Object) gamma)
                .tweakeroo_setValueWithoutCheck(Configs.Generic.GAMMA_OVERRIDE_VALUE.getDoubleValue());
    }
}
