package com.sxcccccccc.iuunify.mixin;

import com.denfop.api.recipe.FluidHandlerRecipe;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 0.3.4 任务二输入侧 tag 化的消耗侧（与 RecipesFluidCoreMatchMixin 配套）。
 *
 * <p>目标方法 {@code FluidHandlerRecipe.consume()}（反编译实证）逐输入罐执行：
 * {@code inputTank.get(i).drain(output.input.getInputs().get(i), EXECUTE)}。
 * Forge FluidTank.drain(FluidStack) 要求罐内流体与资源 isFluidEqual——匹配侧
 * 已放行跨 tag 杂酚油（罐里 IE 杂酚油、配方要求 ic2 杂酚油）时，原消耗会
 * drain 出 0 → 罐里流体没扣、输出照常 → 凭空造料。本 @Redirect 在消耗点
 * 换成"罐内流体同 tag 则按罐内流体 drain"：
 *
 * <ul>
 *   <li>非统一流体（石油/甜油等 12 族外）：走原路径，行为逐位不变；
 *   <li>同 id：走原路径；
 *   <li>统一族内跨 id（罐 IE 杂酚油 × 配方 ic2 杂酚油）：按罐内流体 + 配方
 *       数量 + 罐内 NBT drain，消耗量与原语义一致（min(配方量, 罐存量)）。
 * </ul>
 *
 * <p>Redirect handler 签名（mixin 字节码实证）：实例方法调用重定向，
 * 接收者（FluidTank）是第一个参数，随后是目标方法的参数。
 */
@Mixin(value = FluidHandlerRecipe.class)
public abstract class FluidHandlerRecipeConsumeMixin {

    /**
     * 目标（jar javap 实证）：{@code public void consume()}；体内唯一 drain 调用
     * 描述符
     * {@code FluidTank.drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;}。
     * remap=false：目标类/方法是 mod 成员。
     */
    @Redirect(
            method = "consume()V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;"),
            require = 1,
            remap = false
    )
    private FluidStack iuunify$drainTagAware(FluidTank tank, FluidStack recipeInput, FluidAction action) {
        if (!InputTagRegistry.isUnified(recipeInput.getFluid())) {
            return tank.drain(recipeInput, action); // 非统一流体：原路径
        }
        FluidStack tankFluid = tank.getFluid();
        if (tankFluid.isEmpty() || tankFluid.getFluid() == recipeInput.getFluid()) {
            return tank.drain(recipeInput, action); // 同 id / 罐空：原路径
        }
        if (InputTagRegistry.sameUnifiedTag(recipeInput.getFluid(), tankFluid.getFluid())) {
            return tank.drain(new FluidStack(tankFluid.getFluid(), recipeInput.getAmount(), tankFluid.getTag()), action);
        }
        return tank.drain(recipeInput, action); // 统一族内但不同族：原路径
    }
}
