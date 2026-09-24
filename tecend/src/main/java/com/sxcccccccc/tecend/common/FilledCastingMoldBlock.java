package com.sxcccccccc.tecend.common;

import com.sxcccccccc.tecend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 含奖杯的浇筑模具（未冷却）—— 第 ⑪ 步的「自然冷却」路线：放到地上，五分钟。
 *
 * <p>放下时排一个方块刻（{@code scheduleTick}），到点把方块换成冷却态 ——
 * 一整条命里就这两拍。方块刻本身<b>随区块存盘</b>（{@code LevelChunk} 的 {@code block_ticks}），
 * 区块卸载再加载会接着算。机制跟 IC2 强化建筑泡沫那一类「放下等一会儿自己变」的方块同源。</p>
 *
 * <p>冷却态是另一个方块（{@code cooled_casting_mold}）—— 那是设计定的三个独立模具方块，
 * 各有自己的 id / 名字 / 模型与掉落（见 {@code ModBlocks} 顶部）。</p>
 *
 * <p>挖掉重放会重新计时，这与「放地上五分钟」的语义一致（AE2 熵变机械臂那条路线仍然是即时的）。</p>
 */
public class FilledCastingMoldBlock extends Block {

    /** 自然冷却耗时：5 分钟 */
    public static final int COOL_TICKS = 5 * 60 * 20;

    public FilledCastingMoldBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, COOL_TICKS);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlockAndUpdate(pos, ModBlocks.COOLED_CASTING_MOLD.get().defaultBlockState());
    }
}
