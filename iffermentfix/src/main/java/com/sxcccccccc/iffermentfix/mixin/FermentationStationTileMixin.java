package com.sxcccccccc.iffermentfix.mixin;

import com.buuz135.industrial.block.resourceproduction.tile.FermentationStationTile;
import com.hrznstudio.titanium.component.fluid.SidedFluidTankComponent;
import java.util.Objects;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * IF 发酵站：输出罐已有不同矿种的发酵肉汤（NBT Tag 不同）时，原版 canIncrease
 * 只查容量不查流体身份，机器照样跑完，onFinish 的 fillForced 因 isFluidEqual
 * 为 false 返回 0，产出被吞，输入肉汤和催化剂照扣。
 * 这里在 canIncrease 放行时补一道 NBT 检查：输出罐非空且 NBT 与输入不一致
 * → 不放行，机器停摆等输出罐排空，输入/催化剂/产出零损失。
 * 输出罐是 DRAIN 动作罐，原版流程只会装入发酵肉汤（同一种流体），
 * 所以 NBT 相同即 fillForced 可合并。
 */
@Mixin(value = FermentationStationTile.class, remap = false)
public abstract class FermentationStationTileMixin {

    @Shadow
    private SidedFluidTankComponent<FermentationStationTile> input;

    @Shadow
    private SidedFluidTankComponent<FermentationStationTile> output;

    @Inject(method = "canIncrease", at = @At("RETURN"), cancellable = true, remap = false)
    private void iffermentfix$checkOutputNbt(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        FluidStack tankFluid = this.output.getFluid();
        if (!tankFluid.isEmpty() && !Objects.equals(tankFluid.getTag(), this.input.getFluid().getTag())) {
            cir.setReturnValue(false);
        }
    }
}
