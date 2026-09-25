package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.FilledCastingMoldBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块 + 对应 BlockItem 注册。
 *
 * <p>第 ⑩ 步的三个浇筑模具是**三个独立方块**（早先用一个方块 + stage 属性区分，现按用户要求拆开）：
 * 各自有自己的 id、名字、模型与掉落，创造栏里直接放三件、不用 BlockStateTag。</p>
 */
public final class ModBlocks {
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, TecEnd.MOD_ID);
    public static final DeferredRegister<Item> BLOCK_ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TecEnd.MOD_ID);

    /** 模具方块的共用属性 */
    private static BlockBehaviour.Properties moldProps() {
        return BlockBehaviour.Properties.of()
                .strength(3.0F, 6.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    // ------------------------------------------------------------- 浇筑模具三态

    /** 空模 */
    public static final RegistryObject<Block> CASTING_MOLD =
            BLOCKS.register("casting_mold", () -> new Block(moldProps()));

    /**
     * 含奖杯的浇筑模具（已浇筑、未冷却）。
     *
     * <p>三个模具里只有这一个用自定义类：第 ⑪ 步的「自然冷却」要它放下时排一个方块刻，
     * 五分钟之后自己变成下面那个冷却态。</p>
     */
    public static final RegistryObject<Block> FILLED_CASTING_MOLD =
            BLOCKS.register("filled_casting_mold", () -> new FilledCastingMoldBlock(moldProps()));

    /** 冷却的含奖杯的浇筑模具 */
    public static final RegistryObject<Block> COOLED_CASTING_MOLD =
            BLOCKS.register("cooled_casting_mold", () -> new Block(moldProps()));

    public static final RegistryObject<Item> CASTING_MOLD_ITEM =
            BLOCK_ITEMS.register("casting_mold",
                    () -> new BlockItem(CASTING_MOLD.get(), new Item.Properties()));
    public static final RegistryObject<Item> FILLED_CASTING_MOLD_ITEM =
            BLOCK_ITEMS.register("filled_casting_mold",
                    () -> new BlockItem(FILLED_CASTING_MOLD.get(), new Item.Properties()));
    public static final RegistryObject<Item> COOLED_CASTING_MOLD_ITEM =
            BLOCK_ITEMS.register("cooled_casting_mold",
                    () -> new BlockItem(COOLED_CASTING_MOLD.get(), new Item.Properties()));

    // ---------------------------------------------------------------- 流体方块

    /** 「金猪」流体的流体方块（不放创造栏、无掉落表，属性照原版水的写法） */
    public static final RegistryObject<LiquidBlock> GOLD_PIG_BLOCK =
            BLOCKS.register("gold_pig", () -> solutionBlock(ModFluids.GOLD_PIG.get()));

    /** 溶液类流体的流体方块（残片液 / 活化残片液 / 酸性残片液） */
    public static final RegistryObject<LiquidBlock> FRAGMENT_SOLUTION_BLOCK =
            BLOCKS.register("fragment_solution", () -> solutionBlock(ModFluids.FRAGMENT_SOLUTION.get()));
    public static final RegistryObject<LiquidBlock> ACTIVATED_SOLUTION_BLOCK =
            BLOCKS.register("activated_fragment_solution", () -> solutionBlock(ModFluids.ACTIVATED_SOLUTION.get()));
    public static final RegistryObject<LiquidBlock> ACIDIC_SOLUTION_BLOCK =
            BLOCKS.register("acidic_fragment_solution", () -> solutionBlock(ModFluids.ACIDIC_SOLUTION.get()));

    private static LiquidBlock solutionBlock(FlowingFluid fluid) {
        return new LiquidBlock(fluid, BlockBehaviour.Properties.of()
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid()
                .sound(SoundType.EMPTY));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        BLOCK_ITEMS.register(modBus);
    }
}
