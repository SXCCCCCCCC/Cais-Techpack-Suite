package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.TrophyBlockEntity;
import com.sxcccccccc.tecend.compat.Compat;
import com.sxcccccccc.tecend.compat.CreateTrophy;
import net.mcreator.proofofhonor.init.ProofOfHonorModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块实体注册。
 *
 * <p>奖杯 BE 挂在 {@code proof_of_honor:championplatform}（金奖杯那个方块）上 ——
 * 我们 mixin 给它挂了 EntityBlock。valid blocks 引用它的注册对象，所以没装那个 mod 时
 * 不能注册；过 Compat 门禁，缺它时本 mod 其余部分照常工作。</p>
 */
public final class ModBlockEntities {
    private ModBlockEntities() {}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, TecEnd.MOD_ID);

    /** 缺 proof_of_honor 时为 null（调用方自行判空）。两个 BE 变体没有共同父类，泛型取 BlockEntity */
    public static RegistryObject<BlockEntityType<BlockEntity>> TROPHY;

    public static void register(IEventBus modBus) {
        if (Compat.isLoaded(Compat.PROOF_OF_HONOR)) {
            TROPHY = BLOCK_ENTITIES.register("trophy", () -> BlockEntityType.Builder
                    .of(ModBlockEntities::createTrophy, ProofOfHonorModBlocks.CHAMPIONPLATFORM.get())
                    .build(null));
        }
        BLOCK_ENTITIES.register(modBus);
    }

    /**
     * 方块实体工厂：装了 Create 就是动力变体（背面接转速、消耗应力充气），否则普通 BE。
     *
     * <p>{@link CreateTrophy} 引用 Create 的类型，只有真的走那一支（Create 在场）才会被解析。</p>
     */
    public static BlockEntity createTrophy(BlockPos pos, BlockState state) {
        return Compat.isLoaded(Compat.CREATE)
                ? CreateTrophy.create(pos, state)
                : new TrophyBlockEntity(pos, state);
    }
}
