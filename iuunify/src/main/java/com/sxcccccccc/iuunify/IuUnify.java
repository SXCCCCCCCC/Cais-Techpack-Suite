package com.sxcccccccc.iuunify;

import net.minecraftforge.fml.common.Mod;

/**
 * IU（Industrial Upgrade 3.4.0.10）× IC2 Refabricated 流体统一层（阶段一）。
 *
 * <p>背景（用户裁决）：IU 的 12 种流体并入 IC2 Refabricated（biofuel 无需统一——
 * IU 无此流体）。删除由 iudebug 的 {@code banned_registrations} 在注册期完成
 * （每流体 5 条目：FLUIDS source/flowing、ITEMS 桶、BLOCKS 流体方块、FLUID_TYPES），
 * 本 mod 把 IU 侧对它们的全部引用（FluidName.getInstance() 路径）重定向到
 * {@code ic2_120} 流体。删除必须注册期生效、引用没改完立即崩——崩溃即反馈。
 *
 * <p>实现：
 * <ul>
 *   <li>{@code FluidNameRedirectMixin}：12 个枚举值的 getInstance() HEAD 注入，
 *       注册阶段外返回 ic2_120 包装 RegistryObject；</li>
 *   <li>{@code DeferredRegisterPhaseMixin}：addEntries HEAD/RETURN 置位注册阶段
 *       标志（与 iudebug 同咽喉点、不同注入位，无冲突）。</li>
 * </ul>
 *
 * <p>阶段二待办：世界生成 9 处 {@code (IUFluid)} 强转、机器/配方层的
 * {@code == / equals / fluidPredicate} 逐个核对与改写。
 */
@Mod("iuunify")
public class IuUnify {

    public IuUnify() {
    }
}
