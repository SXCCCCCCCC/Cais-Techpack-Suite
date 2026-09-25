package com.sxcccccccc.fluidunifyapi.iu;

import com.denfop.componets.Fluids;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * IU 流体罐 → 所属方块实体类的弱引用登记表（iuunify InputTagRegistry 同款手法：
 * {@code Fluids.addTank(InternalFluidTank)} TAIL 登记，弱引用随区块卸载/BE 回收
 * 自动清除，无泄漏）。
 *
 * <p>用途：{@code InternalFluidTank.acceptsFluid} 探针路径需要知道"这台机器是谁"
 * 才能查补丁表——InternalFluidTank 自身不持有父组件引用，只能登记。</p>
 *
 * <p>mixin 铁律：本 helper 放 iu 包（mixin 包内只许放 mixin 类）。</p>
 */
public final class IuTankOwnerRegistry {

    private static final Map<Fluids.InternalFluidTank, Class<?>> OWNERS = new WeakHashMap<>();

    private IuTankOwnerRegistry() {
    }

    public static void register(Class<?> ownerClass, Fluids.InternalFluidTank tank) {
        if (ownerClass == null || tank == null) {
            return;
        }
        synchronized (OWNERS) {
            OWNERS.put(tank, ownerClass);
        }
    }

    public static Class<?> ownerOf(Object tank) {
        synchronized (OWNERS) {
            return OWNERS.get(tank);
        }
    }
}
