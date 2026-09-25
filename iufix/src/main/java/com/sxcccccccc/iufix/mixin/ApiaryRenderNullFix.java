package com.sxcccccccc.iufix.mixin;

import com.denfop.render.apiary.TileEntityRenderApiary;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityRenderApiary.class)
public class ApiaryRenderNullFix {

    @Inject(method = "renderItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void iucore$guardNullItem(ItemStack itemStack, Level level, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, CallbackInfo ci) {
        if (itemStack == null || itemStack.isEmpty()) {
            ci.cancel();
        }
    }
}
