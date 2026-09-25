package com.sxcccccccc.mafgllibfix.mixin;

import fi.dy.masa.malilib.event.InitializationHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Replaces the {@code ArrayList} used for the initialization handler list with a
 * {@link CopyOnWriteArrayList}. Forge loads mods in parallel (ForkJoinPool), so two mod
 * constructors can call {@code registerInitializationHandler} on the malilib singleton
 * simultaneously. Concurrent {@code ArrayList.add()} corrupts the internal array
 * ({@code ArrayIndexOutOfBoundsException}), and {@code onGameInitDone} later crashes with a
 * {@code NullPointerException} while iterating the damaged list.
 *
 * <p>COW's snapshot semantics make the concurrent {@code add()} calls safe; the worst case
 * is a duplicate registration (harmless). This is the runtime-mixin equivalent of the
 * manual jar patch, so the original MaFgLib jar can be used unmodified.</p>
 *
 * <p>Implementation note: the substitution is done by assigning a {@code @Shadow}ed field
 * at the constructor tail. A {@code NEW} redirect would require the handler to return the
 * exact constructed type ({@code ArrayList}), and a {@code PUTFIELD} redirect cannot change
 * the stored value (its handler must return {@code void}), so neither can swap the list
 * type. {@code @Mutable} is required on the shadow field so Mixin strips the target
 * field's {@code final} modifier — otherwise the injected method's putfield trips the JVM
 * final-field write check ({@code IllegalAccessError}).</p>
 */
@Mixin(value = InitializationHandler.class, remap = false)
public abstract class InitializationHandlerMixin {

    @Shadow
    @Mutable
    private List handlers;

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void mafgllibfix$copyOnWriteList(CallbackInfo ci) {
        this.handlers = new CopyOnWriteArrayList<>();
    }
}
