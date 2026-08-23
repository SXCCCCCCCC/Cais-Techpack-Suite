package com.sxcccccccc.iufix.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.denfop.blocks.BlockFluidIU", remap = false)
public class BlockFluidIUDrainMixin {

    @Inject(method = "drain", at = @At("HEAD"), cancellable = true, require = 0)
    private void iucore$drain(Level level, BlockPos pos, IFluidHandler.FluidAction action, CallbackInfoReturnable<FluidStack> cir) {
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isEmpty()) {
            FluidStack stack = new FluidStack(fluidState.getType(), 1000);
            if (action.execute()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            cir.setReturnValue(stack);
        }
    }
}
