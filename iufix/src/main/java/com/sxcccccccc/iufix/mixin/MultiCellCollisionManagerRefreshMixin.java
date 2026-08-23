package com.sxcccccccc.iufix.mixin;

import com.denfop.IUItem;
import com.denfop.api.collision.MultiCellCollisionManager;
import com.denfop.api.collision.MultiCellCollisionShapeHelper;
import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blockentity.collision.BlockEntityCollisionProxy;
import com.denfop.blocks.BlockCollisionProxy;
import com.sxcccccccc.iufix.util.MultiblockCollisionUtil;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.0：MultiCellCollisionManager.refresh 重写——清理范围扩到全维度。
 *
 * <p>原实现的问题：collectExisting 只扫 ±4 半径且要求
 * {@code masterPos.equals(proxy.getMasterPos())}——死指针代理（NBT 里的绝对
 * masterX/Y/Z 指向已消失的 master）永远不被清理，换 master 重建时旧代理
 * 还会被重新指到新 master 上（182↔183 互相指环证据）。16.09 崩溃后该结构
 * 靠 1.3.10 的空形状守卫兜底，但污染方块本身仍留在世界里。
 *
 * <p>重写（服务端分支）：先做全维度清扫——所有已加载代理里，
 * 推导 master 位不是 BlockEntityBase 的一律回收；指向本次 master 但不在
 * desired cells 里的过期代理也回收。之后保留原摆放循环（单元格摆放/
 * 重指行为等价）。客户端分支保持原逻辑一字不改——客户端不拆方块
 * （双端一致风险：客户端清方块会与服务端不同步）。
 *
 * <p>清扫的枚举靠 BlockEntityCollisionProxyIEModeMixin 构造时注册的静态表
 * （1.20.1 Level/LevelChunk 无已加载 BE 全量枚举 API），见
 * MultiblockCollisionUtil。客户端分支的 ±4 扫描复刻原 collectExisting
 * 逻辑（目标类的 private 方法编译期不可见，@Unique 方法随 handler 一并并入目标类）。
 */
@Mixin(value = MultiCellCollisionManager.class, remap = false)
public class MultiCellCollisionManagerRefreshMixin {

    @Inject(method = "refresh", at = @At("HEAD"), cancellable = true)
    private static void iufix$refresh(Level level, BlockPos masterPos, CallbackInfo ci) {
        if (level == null) {
            ci.cancel();
            return;
        }
        if (level.isClientSide) {
            // —— 客户端：原逻辑一字不改（不拆方块，等服务端同步）——
            if (level.getBlockEntity(masterPos) instanceof BlockEntityBase master) {
                if (master.useMultiCellCollision()) {
                    Set<BlockPos> desired = MultiCellCollisionShapeHelper.collectCoveredCells(master, masterPos, true);
                    for (BlockPos pos : iufix$collectExisting(level, masterPos)) {
                        if (!desired.contains(pos)) {
                            BlockState state = level.getBlockState(pos);
                            if (state.is(IUItem.COLLISION_PROXY.get())) {
                                level.removeBlock(pos, false);
                            }
                        }
                    }
                    iufix$placeCells(level, masterPos, desired);
                }
            }
            ci.cancel();
            return;
        }
        // —— 服务端：全维度清扫 + 原摆放循环 ——
        if (level.getBlockEntity(masterPos) instanceof BlockEntityBase master) {
            if (!master.useMultiCellCollision()) {
                MultiblockCollisionUtil.sweep(level, masterPos, Collections.emptySet());
            } else {
                Set<BlockPos> desired = MultiCellCollisionShapeHelper.collectCoveredCells(master, masterPos, true);
                MultiblockCollisionUtil.sweep(level, masterPos, desired);
                iufix$placeCells(level, masterPos, desired);
            }
        } else {
            MultiblockCollisionUtil.sweep(level, masterPos, Collections.emptySet());
        }
        ci.cancel();
    }

    /** 原摆放循环（与 1.3.10 refresh 语义等价）。 */
    @Unique
    private static void iufix$placeCells(Level level, BlockPos masterPos, Set<BlockPos> desired) {
        for (BlockPos pos : desired) {
            BlockState state = level.getBlockState(pos);
            if (!pos.equals(masterPos)) {
                if (state.isAir()) {
                    level.setBlock(pos, ((BlockCollisionProxy) IUItem.COLLISION_PROXY.get()).defaultBlockState(), 3);
                    if (level.getBlockEntity(pos) instanceof BlockEntityCollisionProxy proxy) {
                        proxy.setMasterPos(masterPos);
                    }
                } else if (state.is(IUItem.COLLISION_PROXY.get())) {
                    if (level.getBlockEntity(pos) instanceof BlockEntityCollisionProxy proxy) {
                        proxy.setMasterPos(masterPos);
                    }
                } else if (state.canBeReplaced()) {
                    level.setBlock(pos, ((BlockCollisionProxy) IUItem.COLLISION_PROXY.get()).defaultBlockState(), 3);
                    if (level.getBlockEntity(pos) instanceof BlockEntityCollisionProxy proxy) {
                        proxy.setMasterPos(masterPos);
                    }
                }
            }
        }
    }

    /** 原 collectExisting（±4 半径，masterPos.equals 过滤）复刻，仅客户端分支用。 */
    @Unique
    private static Set<BlockPos> iufix$collectExisting(Level level, BlockPos masterPos) {
        Set<BlockPos> result = new HashSet<>();
        for (int x = -4; x <= 4; x++) {
            for (int y = -4; y <= 4; y++) {
                for (int z = -4; z <= 4; z++) {
                    BlockPos pos = masterPos.offset(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.is(IUItem.COLLISION_PROXY.get())
                            && level.getBlockEntity(pos) instanceof BlockEntityCollisionProxy proxy
                            && masterPos.equals(proxy.getMasterPos())) {
                        result.add(pos);
                    }
                }
            }
        }
        return result;
    }
}
