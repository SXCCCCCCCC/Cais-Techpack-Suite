package com.sxcccccccc.iufix.mixin;

import com.denfop.api.recipe.InventoryRecipes;
import com.denfop.componets.ComponentProcess;
import com.denfop.componets.Fluids;
import com.denfop.inventory.Inventory;
import com.google.common.base.Predicate;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;

@Mixin(value = ComponentProcess.class)
public class ComponentProcessNullFix {

    private static final Fluids.InternalFluidTank DUMMY;

    static {
        Fluids.InternalFluidTank dummy = null;
        try {
            Constructor<Fluids.InternalFluidTank> constructor = Fluids.InternalFluidTank.class.getDeclaredConstructor(
                    String.class,
                    Collection.class,
                    Collection.class,
                    Predicate.class,
                    int.class,
                    Inventory.TypeItemSlot.class
            );
            constructor.setAccessible(true);
            dummy = constructor.newInstance(
                    "iucore_null_fix",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    (Predicate<Fluid>) (f -> false),
                    0,
                    null
            );
        } catch (Exception e) {
            dummy = null;
        }
        DUMMY = dummy;
    }

    @Redirect(method = "operateWithMax", at = @At(value = "INVOKE", target = "Lcom/denfop/api/recipe/InventoryRecipes;getTank()Lcom/denfop/componets/Fluids$InternalFluidTank;"), remap = false)
    private Fluids.InternalFluidTank iucore$safeGetTank(InventoryRecipes inv) {
        Fluids.InternalFluidTank tank = inv.getTank();
        if (tank != null) {
            return tank;
        }
        return DUMMY;
    }
}
