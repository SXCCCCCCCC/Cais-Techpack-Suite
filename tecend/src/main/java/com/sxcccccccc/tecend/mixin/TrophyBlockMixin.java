package com.sxcccccccc.tecend.mixin;

import com.sxcccccccc.tecend.common.TrophyBlockEntity;
import net.mcreator.proofofhonor.block.ChampionplatformBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 给「金奖杯」方块（{@code proof_of_honor:championplatform}，类 {@link ChampionplatformBlock}，
 * 用的网格是 {@code custom/championship_trophy}）加：阶段属性（age）+ 方块实体 + 放置时按物品 NBT 定阶段。
 *
 * <p><b>注意别被名字骗</b>：{@code championplatform} 才是金/冠军奖杯，
 * {@code proofofhonor} 那个方块用的是 {@code custom/champion_platform} 网格（站台）。</p>
 *
 * <p>为什么能注入：这个类<b>自己声明</b>了 {@code createBlockStateDefinition}（SRG {@code m_7926_}）
 * 和 {@code getStateForPlacement}（SRG {@code m_5573_}）—— javap 实测，见 {@code libs/README.md}。
 * Mixin 只扫目标类自身声明的方法（继承来的注入不了），这两个恰好是自己声明的，所以 {@code @Inject} 可用。</p>
 *
 * <p>跨 mod mixin ⇒ {@code remap = false} + 方法名写运行时真名（SRG）。handler 体内对原版成员的
 * 调用由 reobfJar 打包时重映射回 SRG，源码里照常写官方名。</p>
 *
 * <p>掉落方向不在这里做：BE 的 {@code type}/{@code charge} 由战利品表的
 * {@code minecraft:copy_nbt} 抄进掉落物的 {@code BlockEntityTag}（原版机制，潜影盒同款做法）。</p>
 */
@Mixin(value = ChampionplatformBlock.class, remap = false)
public abstract class TrophyBlockMixin implements EntityBlock {

    /** ① 把阶段属性加进方块状态定义（在它自己加完 FACING 之后） */
    @Inject(method = "m_7926_", at = @At("TAIL"), require = 1)
    private void tecend$addAgeProperty(StateDefinition.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(TrophyBlockEntity.AGE);
    }

    /** ② 放置时按物品 NBT 的阶段决定方块状态（type → age）；没有 NBT 就是 age=0 */
    @Inject(method = "m_5573_", at = @At("RETURN"), cancellable = true, require = 1)
    private void tecend$typeToAge(BlockPlaceContext context, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = cir.getReturnValue();
        if (state == null || !state.hasProperty(TrophyBlockEntity.AGE)) {
            return;
        }
        ItemStack stack = context.getItemInHand();
        CompoundTag beTag = BlockItem.getBlockEntityData(stack);
        int type = beTag != null ? beTag.getInt(TrophyBlockEntity.TAG_TYPE) : 0;
        cir.setReturnValue(state.setValue(TrophyBlockEntity.AGE, clamp(type)));
    }

    /**
     * ③ 挂上方块实体。
     *
     * <p><b>不能加 {@code @Override}</b>：编译期 classpath 是官方名（{@code newBlockEntity}），而运行时是 SRG
     * （{@code m_142194_}），javac 不认为它是覆写 —— 加了就报"方法不会覆盖或实现超类型的方法"。
     * 类声明成 {@code abstract} 且省掉 {@code @Override} 即可编译；接口实现由 Mixin 在类加载时补上
     * （它会把 EntityBlock 接口并进目标类）。</p>
     */
    public BlockEntity m_142194_(BlockPos pos, BlockState state) {
        return new TrophyBlockEntity(pos, state);
    }

    private static int clamp(int type) {
        return Math.max(TrophyBlockEntity.STAGE_FINISHED, Math.min(type, TrophyBlockEntity.STAGE_MAX));
    }
}
