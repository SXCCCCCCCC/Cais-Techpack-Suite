package com.sxcccccccc.tecend.compat;

import com.simibubi.create.api.stress.BlockStressValues;
import com.sxcccccccc.tecend.common.TrophyKineticBlockEntity;
import net.mcreator.proofofhonor.init.ProofOfHonorModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create 集成的门禁层：引用 Create 类型的代码全放这里，调用方一律先过 {@link Compat#CREATE}。
 *
 * <p>奖杯方块是 proof_of_honor 的类、改不了父类，所以走的是「方块补 IRotate（mixin）
 * + 方块实体用 Create 变体」这条路 —— 旋转网络传播只认 {@code KineticBlockEntity}。</p>
 */
public final class CreateTrophy {
    private CreateTrophy() {}

    /** 应力影响（SU/RPM），照 create:copper_backtank 的 4.0 */
    private static final double IMPACT = 4.0D;

    /** BE 工厂：Create 在场时用动力变体。返回 BlockEntity —— 两个变体没有共同父类 */
    public static BlockEntity create(BlockPos pos, BlockState state) {
        return new TrophyKineticBlockEntity(pos, state);
    }

    /** 方块实体 tick 分派（普通 BE 不 tick） */
    public static void tick(BlockEntity blockEntity) {
        if (blockEntity instanceof TrophyKineticBlockEntity kinetic) {
            kinetic.tick();
        }
    }

    /** 注册应力影响，让这块方块在 Create 的应力账本里有名字 */
    public static void registerStress() {
        BlockStressValues.IMPACTS.register(ProofOfHonorModBlocks.CHAMPIONPLATFORM.get(), () -> IMPACT);
    }
}
