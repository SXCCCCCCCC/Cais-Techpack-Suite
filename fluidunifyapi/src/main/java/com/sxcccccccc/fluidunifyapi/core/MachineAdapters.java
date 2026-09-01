package com.sxcccccccc.fluidunifyapi.core;

import java.util.Map;
import java.util.Set;

/**
 * 机器登记表：配置里用到的 machineId（方块注册名）与运行时查询用到的
 * 方块实体类之间的映射，以及机器族分类。所有表都是手工维护（包内机器
 * 有限且已全量盘过，不引入运行时反射扫描）。
 *
 * <p>machineId 约定：</p>
 * <ul>
 *   <li>IF / IC2：方块注册名（如 "industrialforegoing:bioreactor"、"ic2_120:condenser"）；</li>
 *   <li>IU 配方族："industrialupgrade:&lt;配方管理器名&gt;"（如 "industrialupgrade:fluid_mixer"）；</li>
 *   <li>IU 硬编码族："industrialupgrade:&lt;机器俗名&gt;"（如 "industrialupgrade:biofuel_generator"）。</li>
 * </ul>
 */
public final class MachineAdapters {

    // ==================== IU 配方族（运行时配方表驱动，公开 API 可增删）====================
    // 与 RecipesFluidCore.init() 的 46 个配方管理器一一对应（3.4.0.10 dev 树实证）。
    public static final Set<String> IU_RECIPE_MANAGERS = Set.of(
            "obsidian", "mixer", "replicator", "mixer_double", "gas_combiner", "electrolyzer",
            "refrigerator", "item_divider", "item_divider_fluid", "fluid_separator", "polymerizer",
            "fluid_adapter", "fluid_integrator", "solid_electrolyzer", "squeezer", "oil_purifier",
            "dryer", "oil_refiner", "adv_oil_refiner", "imp_oil_refiner", "fluid_mixer",
            "solid_fluid_mixer", "heat", "gas_chamber", "primal_fluid_integrator", "smeltery",
            "empty", "ingot_casting", "gear_casting", "solid_fluid_integrator",
            "single_fluid_adapter", "biomass", "refractory_furnace", "mini_smeltery", "incubator",
            "insulator", "rna_collector", "mutatron", "reverse_transcriptor",
            "genetic_stabilizer", "genetic_replicator"
    );

    // ==================== IU 硬编码谓词族（BE 类名 → machineId 俗名）====================
    // 判定走通用三咽喉点（addTank 谓词包装 / acceptsFluid / InventoryFluidByList），
    // 运行时以"原生谓词接受哪个模板流体"定位输入口，无需逐罐表。
    public static final Map<String, String> IU_BE_TO_MACHINE = Map.ofEntries(
            Map.entry("BlockEntityBioFuelGenerator", "industrialupgrade:biofuel_generator"),
            Map.entry("BlockEntityDieselGenerator", "industrialupgrade:diesel_generator"),
            Map.entry("BlockEntityPetrolGenerator", "industrialupgrade:petrol_generator"),
            Map.entry("BlockEntityGasGenerator", "industrialupgrade:gas_generator"),
            Map.entry("BlockEntityHydrogenGenerator", "industrialupgrade:hydrogen_generator"),
            Map.entry("BlockEntitySteamGenerator", "industrialupgrade:steam_generator"),
            Map.entry("BlockEntityGeoGenerator", "industrialupgrade:geo_generator"),
            Map.entry("BlockEntityBaseHeatMachine", "industrialupgrade:heat_machine"),
            Map.entry("BlockEntitySmelteryFuelTank", "industrialupgrade:smeltery_fuel_tank"),
            Map.entry("BlockEntitySmelteryController", "industrialupgrade:smeltery_controller"),
            Map.entry("BlockEntitySteamBoiler", "industrialupgrade:steam_boiler"),
            Map.entry("BlockEntitySteamControllerBoiler", "industrialupgrade:steam_controller_boiler"),
            Map.entry("BlockEntityBaseSteamTurbineTank", "industrialupgrade:steam_turbine_tank"),
            Map.entry("BlockEntityBaseSteamTurbineCoolant", "industrialupgrade:steam_turbine_coolant"),
            Map.entry("BlockEntityCyclotronCoolant", "industrialupgrade:cyclotron_coolant"),
            Map.entry("BlockEntityCyclotronCryogen", "industrialupgrade:cyclotron_cryogen"),
            Map.entry("BlockEntityGeothermalExchanger", "industrialupgrade:geothermal_exchanger"),
            Map.entry("BlockEntityGeothermalGenerator", "industrialupgrade:geothermal_generator"),
            Map.entry("BlockEntityGasTurbineTank", "industrialupgrade:gas_turbine_tank"),
            Map.entry("BlockEntityChemicalPlantExchanger", "industrialupgrade:chemical_plant_exchanger"),
            Map.entry("BlockEntityChemicalPlantSeparate", "industrialupgrade:chemical_plant_separate"),
            Map.entry("BlockEntityChemicalPlantWaste", "industrialupgrade:chemical_plant_waste"),
            Map.entry("BlockEntitySteamConverter", "industrialupgrade:steam_converter"),
            Map.entry("BlockEntitySteamCrystalCharge", "industrialupgrade:steam_crystal_charge"),
            Map.entry("BlockEntitySteamDryer", "industrialupgrade:steam_dryer"),
            Map.entry("BlockEntitySteamElectrolyzer", "industrialupgrade:steam_electrolyzer"),
            Map.entry("BlockEntitySteamFluidHeater", "industrialupgrade:steam_fluid_heater"),
            Map.entry("BlockEntitySteamHandlerHeavyOre", "industrialupgrade:steam_handler_heavy_ore"),
            Map.entry("BlockEntitySteamPeatGenerator", "industrialupgrade:steam_peat_generator"),
            Map.entry("BlockEntitySteamPressureConverter", "industrialupgrade:steam_pressure_converter"),
            Map.entry("BlockEntitySteamAmpereGenerator", "industrialupgrade:steam_ampere_generator"),
            Map.entry("BlockEntityAdvSteamQuarry", "industrialupgrade:adv_steam_quarry"),
            Map.entry("BlockEntitySteamQuarry", "industrialupgrade:steam_quarry"),
            Map.entry("BlockEntitySteamPump", "industrialupgrade:steam_pump"),
            Map.entry("BlockEntitySteamSharpener", "industrialupgrade:steam_sharpener"),
            Map.entry("BlockEntitySteamSolidFluidMixer", "industrialupgrade:steam_solid_fluid_mixer"),
            Map.entry("BlockEntitySteamSqueezer", "industrialupgrade:steam_squeezer"),
            Map.entry("BlockEntitySteamWireInsulator", "industrialupgrade:steam_wire_insulator"),
            Map.entry("BlockEntitySteamStorage", "industrialupgrade:steam_storage"),
            Map.entry("BlockEntityBaseSolarDestiller", "industrialupgrade:solar_distiller"),
            Map.entry("BlockEntityBarrel", "industrialupgrade:barrel"),
            Map.entry("BlockEntityMultiCrop", "industrialupgrade:multi_crop"),
            Map.entry("BlockEntityFieldCleaner", "industrialupgrade:field_cleaner"),
            Map.entry("BlockEntityBaseReplicator", "industrialupgrade:replicator"),
            Map.entry("BlockEntityWirelessMatterCollector", "industrialupgrade:wireless_matter_collector"),
            Map.entry("BlockEntityWirelessGasPump", "industrialupgrade:wireless_gas_pump"),
            Map.entry("BlockEntityWirelessOilPump", "industrialupgrade:wireless_oil_pump"),
            Map.entry("BlockEntityFluidCooling", "industrialupgrade:fluid_cooling"),
            Map.entry("BlockEntityRefrigeratorCoolant", "industrialupgrade:refrigerator_coolant"),
            Map.entry("BlockEntityGasWellTank", "industrialupgrade:gas_well_tank"),
            Map.entry("BlockEntityElectricLiquidTankInventory", "industrialupgrade:electric_liquid_tank"),
            Map.entry("BlockEntityLiquidTankInventory", "industrialupgrade:liquid_tank"),
            Map.entry("BlockEntityLiquedTank", "industrialupgrade:liqued_tank")
    );

    // ==================== IF 硬编码 isSame 族（BE 类名 → 方块注册名，3.5.22 实证）====================
    // 经验 tag 判定机器（MobCrusher/MobDuplicator/Enchantment×3）不在此表：原生就是 tag
    // 驱动，加 tag 即生效，配置打到它们身上会在 isKnownMachine 校验时报错（fail-fast）。
    public static final Map<String, String> IF_BE_TO_MACHINE = Map.ofEntries(
            Map.entry("BioReactorTile", "industrialforegoing:bioreactor"),
            Map.entry("BiofuelGeneratorTile", "industrialforegoing:biofuel_generator"),
            Map.entry("LatexProcessingUnitTile", "industrialforegoing:latex_processing_unit"),
            Map.entry("HydroponicBedTile", "industrialforegoing:hydroponic_bed"),
            Map.entry("PlantGathererTile", "industrialforegoing:plant_gatherer"),
            Map.entry("SewageComposterTile", "industrialforegoing:sewage_composter"),
            Map.entry("SlaughterFactoryTile", "industrialforegoing:mob_slaughter_factory"),
            Map.entry("FermentationStationTile", "industrialforegoing:fermentation_station"),
            Map.entry("FluidSievingMachineTile", "industrialforegoing:fluid_sieving_machine"),
            Map.entry("WashingFactoryTile", "industrialforegoing:washing_factory"),
            Map.entry("MechanicalDirtTile", "industrialforegoing:mechanical_dirt"),
            Map.entry("MaterialStoneWorkFactoryTile", "industrialforegoing:material_stonework_factory"),
            Map.entry("PotionBrewerTile", "industrialforegoing:potion_brewer"),
            Map.entry("SludgeRefinerTile", "industrialforegoing:sludge_refiner"),
            Map.entry("SporesRecreatorTile", "industrialforegoing:spores_recreator"),
            Map.entry("WaterCondensatorTile", "industrialforegoing:water_condensator"),
            Map.entry("MycelialGeneratorTile", "industrialforegoing:mycelial_reactor")
    );

    // ==================== IF datapack 配方族（vanilla RecipeManager，经 KubeJS 管线落配方）====================
    // 仅 dissolution_chamber 有流体输入；其余成员列在表里仅为了校验报错信息友好。
    public static final Set<String> IF_DATAPACK_MACHINES = Set.of(
            "industrialforegoing:dissolution_chamber",
            "industrialforegoing:fluid_extractor",
            "industrialforegoing:laser_drill",
            "industrialforegoing:material_stonework_factory"
    );

    // ==================== IC2（方块注册名，0.6 jar 资产实证）====================
    // 判定点 = 每机硬编码谓词/内联 == 点，由 ic2.mixin 包内逐点 mixin 咨询。
    // 只收有<b>流体输入口</b>的机器；无输入口的（泵/装罐机=任意流体、焦炉/焦炉炉箅
    // =仅输出、UU物质发生器=仅输出、半流质发电机=tag 驱动）不在此表——配置打到
    // 它们身上会在校验时报错（fail-fast），半流质发电机请直接用 KubeJS tag。
    public static final Set<String> IC2_MACHINES = Set.of(
            "ic2_120:steam_generator",
            "ic2_120:condenser",
            "ic2_120:solar_distiller",
            "ic2_120:water_generator",
            "ic2_120:liquid_heat_exchanger",
            "ic2_120:fermenter",
            "ic2_120:blast_furnace",
            "ic2_120:geo_generator",
            "ic2_120:ore_washing_plant",
            "ic2_120:miner",
            "ic2_120:nuclear_reactor",
            "ic2_120:replicator",
            "ic2_120:animalmatron",
            "ic2_120:cropmatron",
            "ic2_120:compressor",
            "ic2_120:steam_kinetic_generator"
    );

    private MachineAdapters() {
    }

    /** 配置校验入口：machineId 必须落在已知机器表内。 */
    public static boolean isKnownMachine(String machineId) {
        if (machineId == null) {
            return false;
        }
        if (machineId.startsWith("industrialupgrade:")) {
            String name = machineId.substring("industrialupgrade:".length());
            return IU_RECIPE_MANAGERS.contains(name) || IU_BE_TO_MACHINE.containsValue(machineId);
        }
        if (machineId.startsWith("industrialforegoing:")) {
            return IF_BE_TO_MACHINE.containsValue(machineId) || IF_DATAPACK_MACHINES.contains(machineId);
        }
        if (machineId.startsWith("ic2_120:")) {
            return IC2_MACHINES.contains(machineId);
        }
        return false;
    }

    /** 机器 id 是否属于 IU 配方族（配方表驱动）。 */
    public static boolean isIuRecipeMachine(String machineId) {
        return machineId.startsWith("industrialupgrade:")
                && IU_RECIPE_MANAGERS.contains(machineId.substring("industrialupgrade:".length()));
    }

    /** 机器 id 是否属于 IF datapack 配方族（vanilla RecipeManager 驱动）。 */
    public static boolean isIfDatapackMachine(String machineId) {
        return IF_DATAPACK_MACHINES.contains(machineId);
    }

    /** BE 类 → machineId（沿类层级向上查，基类规则覆盖子类）。IU/IF 通用探针路径用。 */
    public static String machineIdOf(Class<?> beClass) {
        Class<?> c = beClass;
        while (c != null && c != Object.class) {
            String simple = c.getSimpleName();
            String iu = IU_BE_TO_MACHINE.get(simple);
            if (iu != null) {
                return iu;
            }
            String ifm = IF_BE_TO_MACHINE.get(simple);
            if (ifm != null) {
                return ifm;
            }
            c = c.getSuperclass();
        }
        return null;
    }
}
