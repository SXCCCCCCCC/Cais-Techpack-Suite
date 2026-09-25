package com.sxcccccccc.ae2ipnswipefix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

import appeng.client.gui.me.common.ClientReadOnlySlot;
import appeng.menu.slot.AppEngSlot;

import org.anti_ad.mc.ipnext.inventory.ContainerClicker;

/**
 * Every click IPN generates - swipe moving, scroll-to-move, sorting, move-all, hotkey actions -
 * funnels through {@code ContainerClicker.genericClick(AbstractContainerMenu, int, int, ClickType,
 * boolean)}, which sends the actual packet. AE2's terminal item list consists of client-only
 * {@code RepoSlot}s whose {@code index} field is never assigned (AE2 adds them via
 * {@code slots.add()}, bypassing {@code addSlot()}) and thus stays at its default 0; IPN features
 * resolving the hovered list entry through {@code slot.index} therefore issue clicks on menu slot 0 -
 * the first upgrade card of the wireless crafting terminal (AE2 #7834) - or, more generally, on
 * whichever AE2-owned slot the click lands on ({@code AppEngSlot.container} is a shared dummy
 * {@code SimpleContainer(0)}, so IPN's container filters treat AE2 slots as ordinary storage).
 *
 * <p>Fix: cancel IPN-generated clicks whose target slot is an AE2-owned slot ({@link AppEngSlot}) or
 * an AE2 client-only display slot ({@link ClientReadOnlySlot}). AE2 slots must only ever move via
 * real user clicks, which do not pass through this class; player-inventory slots in AE2 GUIs and all
 * vanilla container slots are unaffected, so every IPN feature keeps working everywhere it is
 * meaningful.</p>
 */
@Mixin(value = ContainerClicker.class, remap = false)
public abstract class ContainerClickerMixin {

    @Inject(
            method = "genericClick(Lnet/minecraft/world/inventory/AbstractContainerMenu;IILnet/minecraft/world/inventory/ClickType;Z)V",
            at = @At("HEAD"),
            cancellable = true)
    private void ae2ipnswipefix$blockAe2SlotClicks(AbstractContainerMenu menu, int slotId, int button,
            ClickType type, boolean sendContentUpdates, CallbackInfo ci) {
        var slot = slotId >= 0 && slotId < menu.slots.size() ? menu.slots.get(slotId) : null;
        if (slot instanceof AppEngSlot || slot instanceof ClientReadOnlySlot) {
            ci.cancel();
        }
    }
}
