package com.sxcccccccc.iufix.mixin;

import com.denfop.api.collision.MultiCellCollisionShapeHelper;
import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blockentity.collision.BlockEntityCollisionProxy;
import com.denfop.blocks.BlockCollisionProxy;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.3.10：BlockCollisionProxy.getDelegatedShape 无条件强转守卫。
 *
 * <p>崩溃证据（crash-2026-08-23_16.09.16 与 crash-2026-08-23_15.11.31）：
 * {@code ClassCastException: BlockEntityCollisionProxy cannot be cast to BlockEntityBase}
 * 于 {@code getDelegatedShape(BlockCollisionProxy.java:255)}——
 * 原实现 {@code (BlockEntityBase) level.m_7702_(masterPos)} 无 instanceof 守卫：
 * 当世界数据里某 cell 代理的 masterPos（NBT masterX/Y/Z，或本次 refresh 刚写入的）
 * 指向的位置上是另一个代理时直接崩。15:11 那次由玩家走动触发（碰撞查询），
 * 16:09 那次由客户端 tick 的 anvil onLoaded → MultiCellCollisionManager.refresh →
 * setMasterPos → setChanged → 碰撞查询链触发，两个入口零 WE 帧。
 *
 * <p>结构损坏来源：IU 多胞碰撞的 refresh 会在「空气/可替换」cell 位放代理，
 * 与 //regen 类批量回写（及 1.3.8 时代错误 type 值写入）叠加后，可能出现
 * 「代理指向代理」或「master 位被代理占据」的持久损坏（TE NBT 存盘）。
 * 修复只做守卫：master 处 TE 不是 BlockEntityBase 时返回空形状，
 * 与 masterBe==null 分支同语义，合法结构行为逐字节等价。
 *
 * <p>denfop 自有方法运行时名即 getDelegatedShape（IU jar 官方名映射），
 * 目标方法为私有实例方法，签名
 * (Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Z)
 * → Lnet/minecraft/world/phys/shapes/VoxelShape;。类内对原版方法的调用
 * 由 reobf 映射为 SRG，@Mixin remap=false 时注入串不重映射。
 */
@Mixin(value = BlockCollisionProxy.class, remap = false)
public abstract class BlockCollisionProxyGetDelegatedShapeGuard {

    /** 已告警过的 (代理位, master位) 组合，避免刷屏。 */
    private static final Set<Long> WARNED = Collections.synchronizedSet(new HashSet<>());

    @Inject(method = "getDelegatedShape", at = @At("HEAD"), cancellable = true)
    private void iufix$guardGetDelegatedShape(BlockGetter level, BlockPos pos, boolean collision, CallbackInfoReturnable<VoxelShape> cir) {
        if (level.getBlockEntity(pos) instanceof BlockEntityCollisionProxy proxy) {
            BlockPos masterPos = proxy.getMasterPos();
            if (masterPos == null) {
                cir.setReturnValue(Shapes.empty());
                return;
            }
            if (level.getBlockEntity(masterPos) instanceof BlockEntityBase master) {
                cir.setReturnValue(MultiCellCollisionShapeHelper.buildClippedShapeForCell(master, masterPos, pos, collision));
            } else {
                warnOnce(pos, masterPos);
                cir.setReturnValue(Shapes.empty());
            }
        } else {
            cir.setReturnValue(Shapes.empty());
        }
    }

    private static void warnOnce(BlockPos proxyPos, BlockPos masterPos) {
        long key = masterPos.asLong() ^ Long.rotateLeft(proxyPos.asLong(), 32);
        if (WARNED.add(key)) {
            System.out.println("[iufix] 检测到损坏的 IU 多胞碰撞结构：位置 " + proxyPos.toShortString()
                    + " 的代理 masterPos=" + masterPos.toShortString()
                    + " 处不是 BlockEntityBase（代理指向代理/被代理占据）。已返回空形状防崩溃，"
                    + "请拆除重建该位置的结构（如 //set air 后重放）。");
        }
    }
}
