package com.sxcccccccc.tecend.mixin;

import com.simibubi.create.content.kinetics.base.IRotate;
import com.sxcccccccc.tecend.common.TrophyBlockEntity;
import net.mcreator.proofofhonor.block.ChampionplatformBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 给金奖杯方块挂上 Create 的 {@code IRotate}：**竖轴**，动力从顶上或底下接进来
 * （调速控制器、传动轴、齿轮都行），跟 {@code create:copper_backtank} 一样是垂直轴。
 *
 * <p>方块本身是 proof_of_honor 的类，改不了父类；Create 的 kinetic 只要求方块实现 {@code IRotate}
 * （BE 那边另需继承 {@code KineticBlockEntity}，见 {@code TrophyKineticBlockEntity}），所以补接口即可。
 * {@code IRotate} 的父接口 {@code IWrenchable} 方法全是 default，不用额外实现。</p>
 */
@Mixin(value = ChampionplatformBlock.class, remap = false)
public abstract class TrophyRotateMixin implements IRotate {

    /**
     * 上下两向都收（竖轴穿过去，调速控制器装在下面或用轴从上面插进来都对得上），
     * 但**只有「金奖杯（未充能）」那一档才连轴** —— 别的档位根本接不上传动轴，
     * 也就不转、不吃应力。应力影响是按方块注册的、管不到档位，所以断在这一层最干净。
     */
    @Override
    public boolean hasShaftTowards(LevelReader level, BlockPos pos, BlockState state, Direction side) {
        return side.getAxis() == Direction.Axis.Y
                && state.hasProperty(TrophyBlockEntity.AGE)
                && state.getValue(TrophyBlockEntity.AGE) == TrophyBlockEntity.STAGE_UNCHARGED;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }
}
