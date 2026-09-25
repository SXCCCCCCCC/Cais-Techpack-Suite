package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, TecEnd.MOD_ID);

    /** 「金猪」桶（第 ⑨ 步流体，压力室/遗传稳定器那条线要桶装） */
    public static final RegistryObject<Item> GOLD_PIG_BUCKET =
            ITEMS.register("gold_pig_bucket",
                    () -> new BucketItem(ModFluids.GOLD_PIG,
                            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    /** 三种溶液的桶 */
    public static final RegistryObject<Item> FRAGMENT_SOLUTION_BUCKET =
            ITEMS.register("fragment_solution_bucket", ModItems::solutionBucket);
    public static final RegistryObject<Item> ACTIVATED_SOLUTION_BUCKET =
            ITEMS.register("activated_fragment_solution_bucket",
                    () -> new BucketItem(ModFluids.ACTIVATED_SOLUTION, bucketProps()));
    public static final RegistryObject<Item> ACIDIC_SOLUTION_BUCKET =
            ITEMS.register("acidic_fragment_solution_bucket",
                    () -> new BucketItem(ModFluids.ACIDIC_SOLUTION, bucketProps()));

    private static Item solutionBucket() {
        return new BucketItem(ModFluids.FRAGMENT_SOLUTION, bucketProps());
    }

    private static Item.Properties bucketProps() {
        return new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1);
    }

    // ---------------------------------------------------------- 第 ⑦ 步的三个模具

    /** 坑坑洼洼的模具（沙）—— 沙子工作台合成，消耗远古奖杯 */
    public static final RegistryObject<Item> ROUGH_MOLD_SAND =
            ITEMS.register("rough_mold_sand", () -> new Item(new Item.Properties()));

    /** 坑坑洼洼的模具（蜡）—— 八个蜜蜡围一圈，不消耗 */
    public static final RegistryObject<Item> ROUGH_MOLD_WAX =
            ITEMS.register("rough_mold_wax", () -> new Item(new Item.Properties()));

    /** 完美模具 —— IU 质子丸/质子核心 或 GTCA 质子 8 个围一圈 */
    public static final RegistryObject<Item> PERFECT_MOLD =
            ITEMS.register("perfect_mold", () -> new Item(new Item.Properties()));

    // ---------------------------------------------------- 第 ⑧ 步：粗制完美模具水晶

    /**
     * 粗制完美模具水晶（我们自己的物品，材质拷自 IC2 的 raw_crystal_memory）。
     *
     * <p>为什么要自建而不是直接用 IC2 的 raw_crystal_memory：烧炼要产出<b>带 NBT</b> 的
     * {@code ic2_120:crystal_memory}（{@code {UuTemplate:{ItemId:"tecend:perfect_mold"}}}），
     * 用 IC2 自己的 raw 会先撞上它自带的烧炼配方、出来的是无 NBT 的空水晶；
     * 换成我们自己的原料，这条烧炼链才干净。</p>
     */
    public static final RegistryObject<Item> RAW_CASTING_MOLD_CRYSTAL_MEMORY =
            ITEMS.register("raw_casting_mold_crystal_memory", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
