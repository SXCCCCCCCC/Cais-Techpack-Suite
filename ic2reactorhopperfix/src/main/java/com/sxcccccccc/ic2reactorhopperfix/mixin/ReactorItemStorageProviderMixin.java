package com.sxcccccccc.ic2reactorhopperfix.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把核反应仓 / 反应堆访问接口的 {@code ItemStorage.SIDED} 物品存储 lookup
 * 重定向到中心核堆的 {@code RoutedItemStorage}。
 *
 * <p>背景：{@code ReactorItemStorageProvider} 对仓/接口返回手写的 {@code ReactorItemStorage}，
 * 其 {@code createSnapshot()}/{@code readSnapshot()} 为空实现——事务回滚完全不工作。
 * FFAPI 把它桥接成 Forge 的 {@code ItemStorageItemHandler} 后，凡是"先 simulate 后真插"
 * 的物流（精妙存储漏斗升级 {@code HopperUpgradeWrapper.moveItems}、Create 溜槽
 * {@code ChuteBlockEntity.handleDownwardOutput}）都会把 simulate 的插入真实写进核堆且无法回滚，
 * 后续真插落到别的槽（复制）或满仓时被丢弃（消失）。原版漏斗不 simulate，所以没事。</p>
 *
 * <p>修复：让这两个 lookup 返回中心核堆的 {@code itemStorage}
 * （即 {@code RoutedItemStorage}，SlottedStorage，快照/回滚正确，桥接走
 * {@code SlottedItemStorageItemHandler}，85 个槽位稳定）。</p>
 *
 * <p>实现说明：目标类是 intermediary 命名的 Fabric jar（经 Sinytra Connector 运行），
 * 无法放入编译 classpath，故用 {@code targets} 字符串 + {@code @Coerce} + 反射。
 * 运行时 Connector 只重映射对 MC 类的引用，ic2 自身的类名/方法名保持不变。</p>
 */
@Mixin(targets = "ic2_120.content.block.nuclear.ReactorItemStorageProvider", remap = false)
public abstract class ReactorItemStorageProviderMixin {

    /**
     * 反应仓的存储 lookup：原实现返回 {@code ReactorItemStorage(reactor)}。
     * 改为返回 {@code reactor.itemStorage}（RoutedItemStorage）。
     * 找不到中心核堆时不动返回值，回退原逻辑（原逻辑此时也返回 null）。
     */
    @Inject(method = "getStorageForChamber", at = @At("HEAD"), cancellable = true, remap = false)
    private void ic2reactorhopperfix$chamberStorage(@Coerce Object be, CallbackInfoReturnable<Object> cir) {
        Object routed = findRoutedItemStorage(be, "findAdjacentReactorPublic");
        if (routed != null) {
            cir.setReturnValue(routed);
        }
    }

    /**
     * 访问接口的存储 lookup：同上，用 {@code getCentralReactorPublic()} 找中心核堆。
     */
    @Inject(method = "getStorageForAccessHatch", at = @At("HEAD"), cancellable = true, remap = false)
    private void ic2reactorhopperfix$hatchStorage(@Coerce Object be, CallbackInfoReturnable<Object> cir) {
        Object routed = findRoutedItemStorage(be, "getCentralReactorPublic");
        if (routed != null) {
            cir.setReturnValue(routed);
        }
    }

    /**
     * 反射调用 {@code be.<finderMethod>()} 取得中心核堆，再取它的 {@code itemStorage}。
     * 任一步失败（找不到堆 / ic2 版本变化）返回 null，注入方回退原逻辑。
     */
    @Unique
    private static Object findRoutedItemStorage(Object be, String finderMethod) {
        try {
            Object reactor = be.getClass().getMethod(finderMethod).invoke(be);
            if (reactor == null) {
                return null;
            }
            return reactor.getClass().getMethod("getItemStorage").invoke(reactor);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
