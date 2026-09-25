package com.sxcccccccc.iuunify.mixin;

import com.sxcccccccc.iuunify.compat.InputTagRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 0.3.7 修复一：ForgeFluidTank.fill 防混液检查的 tag 宽限（罐非空场景的跨模同族灌入）。
 *
 * <p>背景（0.3.6 实测：gas_combiner 喂别家杂酚油配方不出）：输入侧 tag 化的三层
 * （罐接受 InternalFluidTankAcceptMixin / 配方匹配 RecipesFluidCoreMatchMixin /
 * 消耗 FluidHandlerRecipeConsumeMixin）全部实证正确且生效，但 0.3.6 漏掉了
 * {@code FluidTank.fill} 的防混液检查——它在 {@code isFluidValid} 之外独立判拒：
 *
 * <pre>
 *   public int fill(FluidStack resource, FluidAction action) {
 *       if (resource.isEmpty() || !isFluidValid(resource)) return 0;   // ← 0.3.6 已宽限
 *       ...
 *       if (!fluid.isFluidEqual(resource)) return 0;                   // ← 0.3.6 漏掉（防混液）
 *       ...
 *   }
 * </pre>
 *
 * 目标类 {@code net.minecraftforge.fluids.capability.templates.FluidTank}（IU 全部
 * InternalFluidTank 的继承链）的 {@code fill(FluidStack, FluidAction)} 内恰有 2 处
 * 防混液 {@code isFluidEqual}（运行时字节码实证：simulate 分支 offset 51、
 * 实填分支 offset 129；receiver=罐内 fluid，arg=传入 resource）。罐内已有
 * ic2_120:creosote 时再灌 immersiveengineering:creosote：isFluidValid 的 tag 宽限
 * 已放行，但此处精确 id 检查直接 return 0——UI 槽/管道两种喂入路径都被拦在罐外，
 * 机器永无配方可跑。
 *
 * <p>修复：@Redirect 换成语义 = "原 isFluidEqual 或同统一 tag"——同族跨模流体
 * 允许混入同一罐（FluidTank.fill 的 grow 语义下罐的 fluid 字段保持先入者 id，
 * 数量累计；配方的 match 侧按罐内实际流体 id 走统一 tag 宽限，消耗侧按罐内流体
 * drain，全链自洽）。非统一流体族路径逐位不变。
 *
 * <p>影响面：isFluidValid 的接受判定本身未放宽（罐拒绝的流体仍在此前返回 0），
 * 仅"已接受的同族流体之间不按精确 id 拆台"——与整体统一语义一致。
 */
@Mixin(value = FluidTank.class)
public abstract class FluidTankFillUnifiedMixin {

    /**
     * 目标（运行时字节码实证）：{@code public int
     * fill(net.minecraftforge.fluids.FluidStack,
     * net.minecraftforge.fluids.capability.IFluidHandler$FluidAction)}；描述符
     * {@code (Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I}。
     * 方法内 2 处 isFluidEqual 全被重定向。remap=false：Forge 类运行时名即 MCP 名。
     *
     * <p>Redirect handler 签名（RedirectedInvokeData.handlerArgs 实证）：实例方法
     * 调用重定向 = [接收者] + [参数]；此处接收者 = 罐内 fluid（成员字段），
     * 参数 = 传入 resource。
     */
    @Redirect(
            method = "fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/fluids/FluidStack;isFluidEqual(Lnet/minecraftforge/fluids/FluidStack;)Z"),
            require = 1,
            remap = false
    )
    private boolean iuunify$fillMixWiden(FluidStack tankFluid, FluidStack resource) {
        if (tankFluid.isFluidEqual(resource)) {
            return true;
        }
        return InputTagRegistry.sameUnifiedTag(tankFluid.getFluid(), resource.getFluid());
    }
}
