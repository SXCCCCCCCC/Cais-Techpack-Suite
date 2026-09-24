package com.sxcccccccc.tecend.mixin;

import com.sxcccccccc.tecend.common.TrophyBlockEntity;
import com.sxcccccccc.tecend.common.TrophyHolder;
import com.sxcccccccc.tecend.compat.Compat;
import com.sxcccccccc.tecend.compat.CreateTrophy;
import com.sxcccccccc.tecend.registry.ModBlockEntities;
import net.mcreator.proofofhonor.block.ChampionplatformBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
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
        return ModBlockEntities.createTrophy(pos, state);
    }

    /**
     * ④ tick 分派：装了 Create 时方块实体是动力变体，要 tick（旋转 + 应力充气）。
     *
     * <p>引用 Create 类型的调用都在 {@link CreateTrophy} 里，只有执行到那一支才会解析那个类。</p>
     */
    public <T extends BlockEntity> BlockEntityTicker<T> m_142354_(Level level, BlockState state, BlockEntityType<T> type) {
        if (!Compat.isLoaded(Compat.CREATE)) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> CreateTrophy.tick(blockEntity);
    }

    /**
     * ⑤ 放下时排一个计划刻 —— 第 ⑭ 步「自然风化」的计时起点。
     *
     * <p><b>为什么用计划刻而不是让方块实体每刻 tick</b>：风化这件事整条命里只有两个时刻
     * ——"放下"和"到点"。计划刻正好就是"排一次、到点跑一次"，一个方块一辈子两拍；
     * 换成常驻 ticker 就是区块加载期间每刻都调，摆一百个奖杯就是每刻一百次，
     * 而那里面九成九的调用只是看一眼阶段然后什么都不做。MC 给"未来的某一刻要干件事"
     * 提供的原生工具就是计划刻，这里没有理由不用它。</p>
     *
     * <p>计划刻是<b>随区块存盘</b>的（{@code LevelChunk} 的 {@code block_ticks}，
     * 见 {@code onPlace} 同款用法的一众原版方块），区块卸载再回来照样会到点。
     * 挖掉重放 = 重新排刻 = 重新计时，与「摆在地上满一小时」的语义一致。</p>
     *
     * <p>只有未喷砂（stage 2）那一档才排刻 —— 别的阶段放下不该被风化。
     * {@code onPlace} 是 {@code BlockBehaviour} 的方法，SRG {@code m_6807_}（查
     * {@code tools/tooling/srg_to_official.csrg} 实证，签名
     * {@code (BlockState, Level, BlockPos, BlockState, boolean)V}）；
     * {@code ChampionplatformBlock} 自己没声明它，所以跟 {@code m_142194_} 一个套路：
     * 加方法覆写，不能加 {@code @Override}。父类那个实现本身是空的，完全覆写没有损失。</p>
     */
    public void m_6807_(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide()
                && state.hasProperty(TrophyBlockEntity.AGE)
                && state.getValue(TrophyBlockEntity.AGE) == TrophyBlockEntity.STAGE_ROUGH_TRIMMED) {
            level.scheduleTick(pos, state.getBlock(), TrophyBlockEntity.AGING_TICKS);
        }
    }

    /**
     * ⑥ 计划刻到点：阶段推进一步（未喷砂 2 → 未充能 1）。这就是第 ⑭ 步的自然风化。
     *
     * <p>到点会再确认一次阶段 —— 这一小时里玩家可能已经用别的手段（比如 AE2 熵变机械臂）
     * 把它推走了，那样就不该再推。判据跟 {@code onPlace} 对称。</p>
     *
     * <p>{@code tick} 是 {@code BlockBehaviour} 的方法，SRG {@code m_213897_}（csrg 实证，
     * 签名 {@code (BlockState, ServerLevel, BlockPos, RandomSource)V}）—— 注意别跟
     * {@code randomTick}（{@code m_213898_}）搞混，那个是随机刻。</p>
     *
     * <p>认的是 {@link TrophyHolder} 而不是具体 BE 类：装了 Create 时方块实体是动力变体，
     * 两者没有共同父类。</p>
     */
    public void m_213897_(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof TrophyHolder holder
                && holder.trophyState().type() == TrophyBlockEntity.STAGE_ROUGH_TRIMMED) {
            holder.trophyState().advance();   // 2 → 1
        }
    }

    private static int clamp(int type) {
        return Math.max(TrophyBlockEntity.STAGE_FINISHED, Math.min(type, TrophyBlockEntity.STAGE_MAX));
    }
}
