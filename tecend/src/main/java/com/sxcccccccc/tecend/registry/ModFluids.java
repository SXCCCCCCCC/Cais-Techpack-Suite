package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import com.sxcccccccc.tecend.common.GoldPigFluidType;
import com.sxcccccccc.tecend.common.TecendFluidType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 流体注册。
 *
 * <p>目前四种：金猪（熔融金属，自绘动画贴图）与三种溶液（残片液 / 活化残片液 / 酸性残片液，
 * 贴图按用户给的颜色程序生成）。溶液贴图都是静态的 —— 流动贴图靠渲染器自身的滚动，
 * 不配 mcmeta 也能动。</p>
 *
 * <p>字段声明顺序讲究：先建两个流体（supplier 懒执行），最后建 Properties 并引用它们与
 * ModBlocks/ModItems 的注册对象 —— 这是 Forge 流体那套"互相引用"的标准写法，
 * 真正构造发生在 register(bus) 之后。supplier 里用类名限定，避开 Java 的简单名前向引用限制。</p>
 */
public final class ModFluids {
    private ModFluids() {}

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, TecEnd.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, TecEnd.MOD_ID);

    private static ResourceLocation blockTex(String name, String suffix) {
        return new ResourceLocation(TecEnd.MOD_ID, "block/" + name + suffix);
    }

    // ---------------------------------------------------------------- 金猪

    public static final RegistryObject<FluidType> GOLD_PIG_TYPE =
            FLUID_TYPES.register("gold_pig", GoldPigFluidType::new);
    public static final RegistryObject<FlowingFluid> GOLD_PIG =
            FLUIDS.register("gold_pig", () -> new ForgeFlowingFluid.Source(ModFluids.GOLD_PIG_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_GOLD_PIG =
            FLUIDS.register("flowing_gold_pig", () -> new ForgeFlowingFluid.Flowing(ModFluids.GOLD_PIG_PROPERTIES));
    public static final ForgeFlowingFluid.Properties GOLD_PIG_PROPERTIES =
            new ForgeFlowingFluid.Properties(GOLD_PIG_TYPE, GOLD_PIG, FLOWING_GOLD_PIG)
                    .block(ModBlocks.GOLD_PIG_BLOCK).bucket(ModItems.GOLD_PIG_BUCKET)
                    .slopeFindDistance(2).levelDecreasePerBlock(2).tickRate(20).explosionResistance(100.0F);

    // ------------------------------------------------------- 残片液（基色）

    public static final RegistryObject<FluidType> FRAGMENT_SOLUTION_TYPE = FLUID_TYPES.register("fragment_solution",
            () -> TecendFluidType.solution(blockTex("fragment_solution", "_still"), blockTex("fragment_solution", "_flowing")));
    public static final RegistryObject<FlowingFluid> FRAGMENT_SOLUTION =
            FLUIDS.register("fragment_solution", () -> new ForgeFlowingFluid.Source(ModFluids.FRAGMENT_SOLUTION_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_FRAGMENT_SOLUTION =
            FLUIDS.register("flowing_fragment_solution", () -> new ForgeFlowingFluid.Flowing(ModFluids.FRAGMENT_SOLUTION_PROPERTIES));
    public static final ForgeFlowingFluid.Properties FRAGMENT_SOLUTION_PROPERTIES =
            new ForgeFlowingFluid.Properties(FRAGMENT_SOLUTION_TYPE, FRAGMENT_SOLUTION, FLOWING_FRAGMENT_SOLUTION)
                    .block(ModBlocks.FRAGMENT_SOLUTION_BLOCK).bucket(ModItems.FRAGMENT_SOLUTION_BUCKET)
                    .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100.0F);

    // ------------------------------------------- 活化残片液（基色偏绿）

    public static final RegistryObject<FluidType> ACTIVATED_SOLUTION_TYPE = FLUID_TYPES.register("activated_fragment_solution",
            () -> TecendFluidType.solution(blockTex("activated_fragment_solution", "_still"), blockTex("activated_fragment_solution", "_flowing")));
    public static final RegistryObject<FlowingFluid> ACTIVATED_SOLUTION =
            FLUIDS.register("activated_fragment_solution", () -> new ForgeFlowingFluid.Source(ModFluids.ACTIVATED_SOLUTION_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_ACTIVATED_SOLUTION =
            FLUIDS.register("flowing_activated_fragment_solution", () -> new ForgeFlowingFluid.Flowing(ModFluids.ACTIVATED_SOLUTION_PROPERTIES));
    public static final ForgeFlowingFluid.Properties ACTIVATED_SOLUTION_PROPERTIES =
            new ForgeFlowingFluid.Properties(ACTIVATED_SOLUTION_TYPE, ACTIVATED_SOLUTION, FLOWING_ACTIVATED_SOLUTION)
                    .block(ModBlocks.ACTIVATED_SOLUTION_BLOCK).bucket(ModItems.ACTIVATED_SOLUTION_BUCKET)
                    .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100.0F);

    // ------------------------------------------- 酸性残片液（基色偏橙）

    public static final RegistryObject<FluidType> ACIDIC_SOLUTION_TYPE = FLUID_TYPES.register("acidic_fragment_solution",
            () -> TecendFluidType.solution(blockTex("acidic_fragment_solution", "_still"), blockTex("acidic_fragment_solution", "_flowing")));
    public static final RegistryObject<FlowingFluid> ACIDIC_SOLUTION =
            FLUIDS.register("acidic_fragment_solution", () -> new ForgeFlowingFluid.Source(ModFluids.ACIDIC_SOLUTION_PROPERTIES));
    public static final RegistryObject<FlowingFluid> FLOWING_ACIDIC_SOLUTION =
            FLUIDS.register("flowing_acidic_fragment_solution", () -> new ForgeFlowingFluid.Flowing(ModFluids.ACIDIC_SOLUTION_PROPERTIES));
    public static final ForgeFlowingFluid.Properties ACIDIC_SOLUTION_PROPERTIES =
            new ForgeFlowingFluid.Properties(ACIDIC_SOLUTION_TYPE, ACIDIC_SOLUTION, FLOWING_ACIDIC_SOLUTION)
                    .block(ModBlocks.ACIDIC_SOLUTION_BLOCK).bucket(ModItems.ACIDIC_SOLUTION_BUCKET)
                    .slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100.0F);

    public static void register(IEventBus modBus) {
        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
    }
}
