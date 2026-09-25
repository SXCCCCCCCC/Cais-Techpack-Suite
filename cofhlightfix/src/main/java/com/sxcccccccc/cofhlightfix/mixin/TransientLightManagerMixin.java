package com.sxcccccccc.cofhlightfix.mixin;

import cofh.core.common.TransientLightManager;
import net.minecraft.world.level.lighting.BlockLightSectionStorage;
import net.minecraft.world.level.lighting.LayerLightSectionStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Mixin that fixes the NullPointerException crash in CoFH Core's
 * {@code TransientLightManager.tick(ClientTickEvent)}.
 *
 * <p>Root cause: {@code tick} has two loops. The first (CURRENT entries) guards each
 * position with {@code m_75791_ == getDataLayer(section, true) != null} before calling
 * {@code BlockLightSectionStorage.getStoredLevel(pos)}. The second loop (PREVIOUS
 * entries, CoFH source line 96) has NO such guard. Vanilla
 * {@code LayerLightSectionStorage.getStoredLevel} does not null-check either — it
 * dereferences the DataLayer returned by {@code getDataLayer(section, true)} directly
 * (LayerLightSectionStorage.java:102 in the shipped client). When a retired transient
 * light's section no longer has light data on the client (chunk unloaded / light data
 * pruned / stale PREVIOUS entry surviving a world change — {@code CURRENT} is cleared
 * on {@code level == null} but {@code PREVIOUS} is not), the second loop NPEs every
 * tick.</p>
 *
 * <p>Fix: wrap BOTH {@code getStoredLevel} call sites in {@code tick} with a
 * null-safe fallback. When the section is gone the true stored light level is
 * effectively 0, which is exactly what the original code expects a "missing" section
 * to behave like (the first loop's guard aborts, the second loop's {@code stored ==
 * previous} compare then simply fails and skips {@code checkBlock}).</p>
 *
 * <p>Why reflection: the vanilla method is {@code getStoredLevel} at compile time
 * (official mappings) but {@code m_75795_} at runtime (SRG). A remap=false mixin
 * targeting a mod class cannot reference it by either name directly (compile-time
 * name does not exist at runtime; runtime name does not exist at compile time, and
 * the method is protected), so it is looked up once by runtime name and invoked
 * reflectively.</p>
 */
@Mixin(value = TransientLightManager.class, remap = false)
public abstract class TransientLightManagerMixin {

    /** Runtime (SRG) name of {@code LayerLightSectionStorage.getStoredLevel(long)}. */
    private static final String GET_STORED_LEVEL_RUNTIME_NAME = "m_75795_";

    /** Looked up at class-load time; only ever runs inside the real game (SRG runtime). */
    private static final Method GET_STORED_LEVEL = findGetStoredLevel();

    private static Method findGetStoredLevel() {
        try {
            Method method = LayerLightSectionStorage.class
                    .getDeclaredMethod(GET_STORED_LEVEL_RUNTIME_NAME, long.class);
            method.setAccessible(true);
            return method;
        } catch (ReflectiveOperationException | SecurityException e) {
            throw new IllegalStateException("cofhlightfix: failed to locate getStoredLevel ("
                    + GET_STORED_LEVEL_RUNTIME_NAME + ")", e);
        }
    }

    /**
     * Null-safe {@code getStoredLevel}: returns 0 when the light section is missing
     * (the original call throws NPE inside vanilla code), matching the semantic of
     * "no stored light level" the surrounding CoFH logic expects.
     */
    private static int cofhlightfix$safeGetStoredLevel(BlockLightSectionStorage storage, long pos) {
        try {
            return (int) GET_STORED_LEVEL.invoke(storage, pos);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof NullPointerException) {
                return 0;
            }
            throw new IllegalStateException("cofhlightfix: unexpected getStoredLevel failure", e.getCause());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("cofhlightfix: getStoredLevel is not accessible", e);
        }
    }

    /** First call site: CURRENT-entries loop (CoFH source line 68). */
    @Redirect(
            method = "tick(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;m_75795_(J)I",
                    ordinal = 0
            ),
            remap = false
    )
    private static int cofhlightfix$nullSafeStoredLevel0(BlockLightSectionStorage storage, long pos) {
        return cofhlightfix$safeGetStoredLevel(storage, pos);
    }

    /** Second call site: PREVIOUS-entries cleanup loop (CoFH source line 96, the crash site). */
    @Redirect(
            method = "tick(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/lighting/BlockLightSectionStorage;m_75795_(J)I",
                    ordinal = 1
            ),
            remap = false
    )
    private static int cofhlightfix$nullSafeStoredLevel1(BlockLightSectionStorage storage, long pos) {
        return cofhlightfix$safeGetStoredLevel(storage, pos);
    }
}
