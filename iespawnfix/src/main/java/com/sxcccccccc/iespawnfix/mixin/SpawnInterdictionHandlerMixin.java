package com.sxcccccccc.iespawnfix.mixin;

import blusunrize.immersiveengineering.common.util.SpawnInterdictionHandler;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Fixes the crash where IE's {@link SpawnInterdictionHandler} cancels a spawn via
 * {@code FinalizeSpawn#setSpawnCancelled(true)} for an entity that is already added
 * to the world.
 *
 * <p>Vanilla adds the entity to the world <em>before</em> calling {@code finalizeSpawn}
 * for conversion-type spawns (e.g. a villager turning into a zombie villager when killed
 * by a zombie). Forge 47.4.x guards {@code Mob#setSpawnCancelled} against exactly this
 * case and throws {@code UnsupportedOperationException}, crashing the server.</p>
 *
 * <p>For entities already in the world we discard them directly, which reproduces the
 * pre-47.4 behavior (the cancellation flag caused the mob to be discarded when it was
 * added to the world). Natural spawns still take the normal flag path, since their
 * {@code finalizeSpawn} runs before the entity is added to the world.</p>
 */
@Mixin(value = SpawnInterdictionHandler.class, remap = false)
public abstract class SpawnInterdictionHandlerMixin {

    @Redirect(
            method = "onEntitySpawnCheck",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/event/entity/living/MobSpawnEvent$FinalizeSpawn;setSpawnCancelled(Z)V"
            ),
            remap = false
    )
    private static void iespawnfix$redirectSetSpawnCancelled(MobSpawnEvent.FinalizeSpawn event, boolean cancel) {
        if (!cancel) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity.isAddedToWorld()) {
            entity.discard();
        } else {
            event.setSpawnCancelled(true);
        }
    }
}
