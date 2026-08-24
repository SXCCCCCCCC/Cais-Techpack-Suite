package com.sxcccccccc.iuunify.mixin;

import com.denfop.api.recipe.RecipesFluidCore;
import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 0.3.4 任务二输入侧 tag 化的匹配侧（gas_combiner 杂酚油跨 mod 吃料）。
 *
 * <p>目标方法 {@code RecipesFluidCore.getRecipeOutput} 的两个流体变体
 * （varargs + List）内部各自恰有 1 处
 * {@code isFluidEqual} 调用（javap 逐点实证）："配方输入流体 × 机器罐当前流体"
 * 全输入匹配判定。@Redirect 换成语义 = "原 isFluidEqual 或同统一 tag"：
 * 非统一流体（石油、甜油等 12 族外流体）命中原判定分支，行为与 0.3.3 逐位一致；
 * 统一流体族内跨 mod 同义流体（IE 杂酚油 × ic2 杂酚油同属 forge:creosote）判定通过。
 *
 * <p>配套：消耗侧由 {@link FluidHandlerRecipeConsumeMixin} 换用罐内流体 drain；
 * tag 数据由 iuunify datapack 的 forge:/c: creosote JSON 发布。
 *
 * <p>Redirect handler 签名（mixin 字节码实证 RedirectedInvokeData.handlerArgs =
 * 非 static 调用时 [owner] + [参数]）：实例方法调用重定向，接收者实例是第一个
 * 参数；目标方法返回 boolean → handler 返回 boolean。
 */
@Mixin(value = RecipesFluidCore.class)
public abstract class RecipesFluidCoreMatchMixin {

    /**
     * List 变体（gas_combiner 的 FluidHandlerRecipe.getOutput 实际调用路径，
     * 描述符 javap 实证）：
     * {@code getRecipeOutput(Lcom/denfop/api/recipe/IBaseRecipe;Ljava/util/List;ZLjava/util/List;)Lcom/denfop/api/recipe/BaseFluidMachineRecipe;}
     */
    @Redirect(
            method = "getRecipeOutput(Lcom/denfop/api/recipe/IBaseRecipe;Ljava/util/List;ZLjava/util/List;)Lcom/denfop/api/recipe/BaseFluidMachineRecipe;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidStack;isFluidEqual(Lnet/minecraftforge/fluids/FluidStack;)Z"),
            require = 1,
            remap = false
    )
    private boolean iuunify$matchList(FluidStack receiver, FluidStack arg) {
        return match(receiver, arg);
    }

    /**
     * varargs 变体（其他机器的 List<FluidStack> 路径，描述符 javap 实证）：
     * {@code getRecipeOutput(Lcom/denfop/api/recipe/IBaseRecipe;Ljava/util/List;Z[Lnet/minecraftforge/fluids/FluidStack;)Lcom/denfop/api/recipe/BaseFluidMachineRecipe;}
     */
    @Redirect(
            method = "getRecipeOutput(Lcom/denfop/api/recipe/IBaseRecipe;Ljava/util/List;Z[Lnet/minecraftforge/fluids/FluidStack;)Lcom/denfop/api/recipe/BaseFluidMachineRecipe;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidStack;isFluidEqual(Lnet/minecraftforge/fluids/FluidStack;)Z"),
            require = 1,
            remap = false
    )
    private boolean iuunify$matchVarargs(FluidStack receiver, FluidStack arg) {
        return match(receiver, arg);
    }

    /**
     * "原判定或同统一 tag"。先走原 isFluidEqual（含 NBT 与空栈语义，非统一流体
     * 路径行为与目标方法原逻辑完全一致），失败再查 12 统一 tag。
     * sameUnifiedTag 内部 null 安全、对称。
     */
    private static boolean match(FluidStack receiver, FluidStack arg) {
        if (receiver.isFluidEqual(arg)) {
            return true;
        }
        return InputTagRegistry.sameUnifiedTag(receiver.getFluid(), arg.getFluid());
    }
}
