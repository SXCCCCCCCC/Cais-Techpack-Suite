package com.sxcccccccc.constructionstickfix.mixin;

import mrbysco.constructionstick.api.IStickUpgrade;
import mrbysco.constructionstick.basics.StickUtil;
import mrbysco.constructionstick.basics.option.StickUpgradesSelectable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.Objects;

/**
 * Mixin that fixes NullPointerException in {@code StickUpgradesSelectable.populateList()}
 * when a null element appears in the upgrade registry list.
 * <p>
 * Root cause: Under Sinytra Connector, {@link StickUtil#getAllUpgrades()} can return
 * a list containing null elements. The for-each loop in populateList calls
 * {@code upgrade.getUpgradeKey()} on null, causing a crash during resource reload (F3+T).
 * <p>
 * Fix: Redirect {@code getAllUpgrades()} call in {@code populateList()} to filter
 * out null elements before the list is iterated.
 *
 * @see StickUtil#getAllUpgrades()
 * @see StickUpgradesSelectable#populateList(IStickUpgrade)
 */
@Mixin(value = StickUpgradesSelectable.class, remap = false)
public abstract class StickUpgradesSelectableMixin {

    /**
     * Intercepts the {@code StickUtil.getAllUpgrades()} call inside
     * {@code populateList()} and removes any null elements from the returned list.
     * <p>
     * The original bytecode at this call site:
     * <pre>
     *   invokestatic StickUtil.getAllUpgrades()Ljava/util/List;
     *   invokeinterface List.iterator()Ljava/util/Iterator;
     * </pre>
     * Our handler returns a null-filtered list, so the subsequent iterator
     * never sees null elements.
     */
    @Redirect(
        method = "populateList",
        at = @At(
            value = "INVOKE",
            target = "Lmrbysco/constructionstick/basics/StickUtil;getAllUpgrades()Ljava/util/List;"
        ),
        remap = false
    )
    private List<IStickUpgrade> constructionstickfix$filterNullUpgrades() {
        List<IStickUpgrade> upgrades = StickUtil.getAllUpgrades();
        // Remove any null elements that may have leaked into the registry list.
        // Under Sinytra Connector, Fabric mod items can in rare cases produce
        // null entries when the item registry is iterated during resource reload.
        upgrades.removeIf(Objects::isNull);
        return upgrades;
    }
}
