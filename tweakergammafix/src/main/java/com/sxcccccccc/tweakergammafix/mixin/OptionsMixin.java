package com.sxcccccccc.tweakergammafix.mixin;

import fi.dy.masa.tweakeroo.config.Configs;
import fi.dy.masa.tweakeroo.mixin.IMixinSimpleOption;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The gamma override stores its value (default 16.0) directly in the option instance.
 * Forge 47.4.x patched {@code Options.save()} to serialize each option through its codec,
 * which validates the [0.0, 1.0] range and logs
 * {@code Error saving option 亮度: DynamicException[...]} when it sees 16.0, skipping the line.
 *
 * <p>This mixin temporarily swaps the override value for the original gamma around
 * {@code Options.save()} so the codec check passes, then restores it. The vanilla slider
 * value in options.txt keeps its real (0.0-1.0) value, which is also what the
 * {@link ClientTickHandlerMixin} expects on the next launch.</p>
 */
@Mixin(Options.class)
public abstract class OptionsMixin {

    @Unique
    private static double tweakergammafix$pendingRestore = Double.NaN;

    @Inject(method = "save", at = @At("HEAD"))
    private void tweakergammafix$sanitizeBeforeSave(CallbackInfo ci) {
        Options self = (Options) (Object) this;
        OptionInstance<Double> gamma = self.gamma();
        double current = gamma.get();
        if (current > 1.0) {
            tweakergammafix$pendingRestore = current;
            ((IMixinSimpleOption<Double>) (Object) gamma)
                    .tweakeroo_setValueWithoutCheck(Configs.Internal.GAMMA_VALUE_ORIGINAL.getDoubleValue());
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void tweakergammafix$restoreAfterSave(CallbackInfo ci) {
        if (!Double.isNaN(tweakergammafix$pendingRestore)) {
            double restore = tweakergammafix$pendingRestore;
            tweakergammafix$pendingRestore = Double.NaN;
            ((IMixinSimpleOption<Double>) (Object) ((Options) (Object) this).gamma())
                    .tweakeroo_setValueWithoutCheck(restore);
        }
    }
}
