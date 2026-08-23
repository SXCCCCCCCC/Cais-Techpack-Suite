package com.sxcccccccc.iufix;

import com.denfop.api.collision.MultiCellCollisionManager;
import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.events.TickHandlerIU;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 1.4.1：村庄/结构生成的铁砧（多胞机器）幽灵方块修复——世界生成路径补 refresh。
 *
 * <p><b>根因（字节码证据）：</b>IU 村庄房屋由 ProfessionHamletPiece 在代码里
 * 直接摆方块（buildMetallurgForge → placeJobBlock → StructurePiece.placeBlock →
 * WorldGenRegion.setBlock → ProtoChunk.setBlockState），铁砧 BE 在 ProtoChunk
 * 里生成后，随 chunk 转正由 {@code LevelChunk(ServerLevel, ProtoChunk,
 * PostLoadProcessor)} 构造器转移进块实体表。该转移循环逐个调用
 * {@code LevelChunk.m_142169_（setLevel + clearRemoved + 入表，无 onLoad）}，
 * 与玩家放置路径 {@code setBlockState → m_142170_(setBlockEntity) →
 * m_156406_ + BlockEntity.onLoad}、磁盘加载路径 {@code m_62870_
 * (createAndLoadBlockEntity) → onLoad} 都不同——世界生成路径从不触发
 * {@code BlockEntity.onLoad}。而 IU 的 {@code BlockEntityBase.onLoad} 会用
 * {@code TickHandlerIU.requestSingleWorldTick} 调度
 * {@code MultiCellCollisionManager.refresh} 去摆放碰撞代理方块；onLoad 不触发
 * → 村庄铁砧首次激活永远没有代理 → 碰撞形状为空 → 幽灵方块。
 * 玩家放置与磁盘重载（保存后重进）都走 onLoad，所以那两个路径正常。
 *
 * <p><b>修复：</b>chunk 转正加载时（ChunkEvent.Load，server 端、仅新生成
 * chunk——旧 chunk 磁盘加载路径自带 onLoad，不需要补）扫描块内所有
 * needCollision 的 BlockEntityBase，按与 onLoad 完全相同的模式把 refresh
 * 调度到下一 tick（幂等；与玩家路径同时序，安全性已由玩家放置验证）。
 * 村庄铁砧与 apiary 等一切世界生成的多胞机器统一覆盖，不动任何原版类、
 * 不动 IU 结构生成代码。
 */
public class ChunkLoadCollisionRefreshFix {

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!event.isNewChunk()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!(event.getChunk() instanceof LevelChunk chunk)) {
            return;
        }
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof BlockEntityBase base && base.needCollision()) {
                BlockPos pos = be.getBlockPos();
                TickHandlerIU.requestSingleWorldTick(serverLevel, world -> MultiCellCollisionManager.refresh(world, pos));
            }
        }
    }
}
