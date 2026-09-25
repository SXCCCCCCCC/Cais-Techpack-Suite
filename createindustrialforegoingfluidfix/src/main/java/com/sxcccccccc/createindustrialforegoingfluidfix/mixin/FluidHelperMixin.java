package com.sxcccccccc.createindustrialforegoingfluidfix.mixin;

import com.buuz135.industrial.fluid.OreFluid;
import com.hrznstudio.titanium.fluid.TitaniumFluid;
import com.simibubi.create.foundation.fluid.FluidHelper;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin that fixes Create's fluid conversion for Industrial Foregoing fluids.
 * <p>
 * Root cause: IF fluids extend vanilla {@code FlowingFluid} directly — latex, sludge,
 * pink slime, essence, ether, sewage, biofuel and meat use {@link TitaniumFluid},
 * while raw/fermented ore meat use IF's own {@link OreFluid}. Neither is a
 * {@link ForgeFlowingFluid}, so {@code FluidHelper.convertToStill} /
 * {@code convertToFlowing} returned them unchanged. Because the hose pulley drains
 * the world fluid state type, blocks of the flowing layer (level 1-8) yielded
 * {@code industrialforegoing:latex_flowing} stacks, while buckets and machine
 * recipes use the source fluid {@code industrialforegoing:latex}. The two never
 * stack in AE2 and machine inputs reject the flowing variant.
 * <p>
 * Fix: normalize every IF fluid (TitaniumFluid and OreFluid) to its source or
 * flowing counterpart via their own getSource()/getFlowing() implementations.
 * Vanilla water/lava special cases and ForgeFlowingFluid handling are preserved.
 */
@Mixin(value = FluidHelper.class, remap = false)
public abstract class FluidHelperMixin {

    @Inject(method = "convertToStill", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createindustrialforegoingfluidfix$convertToStill(Fluid fluid, CallbackInfoReturnable<Fluid> cir) {
        if (fluid == Fluids.FLOWING_WATER) {
            cir.setReturnValue(Fluids.WATER);
            return;
        }
        if (fluid == Fluids.FLOWING_LAVA) {
            cir.setReturnValue(Fluids.LAVA);
            return;
        }
        // Industrial Foregoing special case: all IF fluids (TitaniumFluid-based
        // machines fluids + OreFluid-based ore meat fluids) extend vanilla
        // FlowingFluid, so the original ForgeFlowingFluid check never matched them.
        if (fluid instanceof TitaniumFluid titaniumFluid) {
            Fluid source = titaniumFluid.getSource();
            cir.setReturnValue(source != null ? source : fluid);
            return;
        }
        if (fluid instanceof OreFluid oreFluid) {
            Fluid source = oreFluid.getSource();
            cir.setReturnValue(source != null ? source : fluid);
            return;
        }
        // Preserve the original ForgeFlowingFluid behaviour.
        if (fluid instanceof ForgeFlowingFluid forgeFlowingFluid) {
            cir.setReturnValue(forgeFlowingFluid.getSource());
            return;
        }
        // Generic fallback: any other mod fluid extending vanilla FlowingFluid
        // (e.g. IC2's ModFluids$Ic2Fluid steam/biomass/etc., running through
        // Sinytra Connector) is also normalized to its source variant, so pumped
        // fluid stacks match buckets and machine recipes.
        if (fluid instanceof FlowingFluid flowingFluid) {
            Fluid source = flowingFluid.getSource();
            cir.setReturnValue(source != null ? source : fluid);
            return;
        }
        cir.setReturnValue(fluid);
    }

    @Inject(method = "convertToFlowing", at = @At("HEAD"), cancellable = true, remap = false)
    private static void createindustrialforegoingfluidfix$convertToFlowing(Fluid fluid, CallbackInfoReturnable<Fluid> cir) {
        if (fluid == Fluids.WATER) {
            cir.setReturnValue(Fluids.FLOWING_WATER);
            return;
        }
        if (fluid == Fluids.LAVA) {
            cir.setReturnValue(Fluids.FLOWING_LAVA);
            return;
        }
        if (fluid instanceof TitaniumFluid titaniumFluid) {
            Fluid flowing = titaniumFluid.getFlowing();
            cir.setReturnValue(flowing != null ? flowing : fluid);
            return;
        }
        if (fluid instanceof OreFluid oreFluid) {
            Fluid flowing = oreFluid.getFlowing();
            cir.setReturnValue(flowing != null ? flowing : fluid);
            return;
        }
        if (fluid instanceof ForgeFlowingFluid forgeFlowingFluid) {
            cir.setReturnValue(forgeFlowingFluid.getFlowing());
            return;
        }
        if (fluid instanceof FlowingFluid flowingFluid) {
            Fluid flowing = flowingFluid.getFlowing();
            cir.setReturnValue(flowing != null ? flowing : fluid);
            return;
        }
        cir.setReturnValue(fluid);
    }
}
