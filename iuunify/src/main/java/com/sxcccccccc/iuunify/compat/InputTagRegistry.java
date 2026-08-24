package com.sxcccccccc.iuunify.compat;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blockentity.base.BlockEntityBaseHeatMachine;
import com.denfop.blockentity.cokeoven.BlockEntityCokeOvenInputFluid;
import com.denfop.blockentity.crop.BlockEntityMultiCrop;
import com.denfop.blockentity.cyclotron.BlockEntityCyclotronCoolant;
import com.denfop.blockentity.geothermalpump.BlockEntityGeothermalExchanger;
import com.denfop.blockentity.mechanism.BlockEntityBioGenerator;
import com.denfop.blockentity.mechanism.BlockEntityFieldCleaner;
import com.denfop.blockentity.mechanism.BlockEntitySingleFluidAdapter;
import com.denfop.blockentity.mechanism.BlockEntitySteamGenerator;
import com.denfop.blockentity.mechanism.blastfurnace.block.BlockEntityBlastFurnaceMain;
import com.denfop.blockentity.mechanism.generator.energy.fluid.BlockEntityBioFuelGenerator;
import com.denfop.blockentity.mechanism.steam.BlockEntityAdvSteamQuarry;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamAmpereGenerator;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamBioGenerator;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamBoiler;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamConverter;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamCrystalCharge;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamDryer;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamElectrolyzer;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamFluidHeater;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamHandlerHeavyOre;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamPeatGenerator;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamPump;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamQuarry;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamSharpener;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamSolidFluidMixer;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamSqueezer;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamStorage;
import com.denfop.blockentity.mechanism.steam.BlockEntitySteamWireInsulator;
import com.denfop.blockentity.mechanism.steamturbine.coolant.BlockEntityBaseSteamTurbineCoolant;
import com.denfop.blockentity.smeltery.BlockEntitySmelteryFuelTank;
import com.denfop.componets.Fluids;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.WeakHashMap;

/**
 * IU 机器输入罐的 tag 宽限注册表（阶段二 C 线：输入侧 tag 化）。
 *
 * <p>机制：IU 全部流体罐的接受判定汇聚于
 * {@link Fluids.InternalFluidTank#acceptsFluid(Fluid)}（FluidHandler.isFluidValid、
 * FluidTank.fill→isFluidValid 都走它，见 {@code com.denfop.componets.Fluids}
 * 字节码实证）。本类在 {@code Fluids.addTank(InternalFluidTank)} 时按
 * （owner BE 类 + 罐 identifier）解析出该罐应宽限的 tag，登记进弱引用表；接受判定时
 * 若流体命中 tag 即放行，否则回落到机器原始精确 id 谓词（输出侧/判定侧零影响）。
 *
 * <p>tag 命名空间取 {@code forge:}——取证结论：包内全部 Forge mod（Mek/GT/Create/
 * IE/PNC/Thermal/IF）流体 tag 惯例均为 forge:，IC2R jar 对自家 9 流体同时发布了
 * c:/forge: 双命名空间，IU 是 Forge 机器，按包内 Forge 惯例消费 forge:。
 *
 * <p>SOP 铁律：mixin 包内只许放 mixin 类——本 helper 放 compat 子包。
 */
public final class InputTagRegistry {

    // ---- forge: 命名空间 tag（12 流体全部由 iuunify datapack + IC2 jar 发布） ----
    public static final TagKey<Fluid> STEAM = tag("steam");
    public static final TagKey<Fluid> BIOMASS = tag("biomass");
    public static final TagKey<Fluid> WEED_EX = tag("weed_ex");
    public static final TagKey<Fluid> COOLANT = tag("coolant");
    public static final TagKey<Fluid> HOT_COOLANT = tag("hot_coolant");
    public static final TagKey<Fluid> PAHOEHOE_LAVA = tag("pahoehoe_lava");
    public static final TagKey<Fluid> DISTILLED_WATER = tag("distilled_water");

    /** 罐 → 应宽限的 tag（弱引用：区块卸载、BE 回收后自动清除，无泄漏）。 */
    private static final WeakHashMap<Fluids.InternalFluidTank, TagKey<Fluid>> TANK_TAGS = new WeakHashMap<>();

    /**
     * 规则表：(owner 类, 罐 identifier, tag)。instanceof 匹配——基类规则自动覆盖子类
     * （BlockEntityBaseHeatMachine 覆盖全部热机、BlockEntityBaseSteamTurbineCoolant
     * 覆盖 4 档汽轮机冷却液）。规则类之间无继承重叠（已核对 jar 类层级）。
     */
    private static final Rule[] RULES = {
            // ---- 蒸汽输入（19 台蒸汽机器 + 焦炉 2 族 + 高炉）→ forge:steam ----
            rule(BlockEntitySteamGenerator.class, "fluidTank2", STEAM),
            rule(BlockEntityAdvSteamQuarry.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamAmpereGenerator.class, "fluidTank1", STEAM),
            rule(BlockEntitySteamBioGenerator.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamBoiler.class, "fluidTank1", STEAM),
            rule(BlockEntitySteamConverter.class, "fluidTank1", STEAM),
            rule(BlockEntitySteamCrystalCharge.class, "fluidTank6", STEAM),
            rule(BlockEntitySteamDryer.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamElectrolyzer.class, "fluidTank4", STEAM),
            rule(BlockEntitySteamFluidHeater.class, "fluidTank1", STEAM),
            rule(BlockEntitySteamHandlerHeavyOre.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamPeatGenerator.class, "fluidTank1", STEAM),
            rule(BlockEntitySteamPump.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamQuarry.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamSharpener.class, "fluidTank6", STEAM),
            rule(BlockEntitySteamSolidFluidMixer.class, "fluidTank6", STEAM),
            rule(BlockEntitySteamSqueezer.class, "fluidTank2", STEAM),
            rule(BlockEntitySteamStorage.class, "fluidTank", STEAM),
            rule(BlockEntitySteamWireInsulator.class, "fluidTank6", STEAM),
            rule(com.denfop.blockentity.adv_cokeoven.BlockEntityCokeOvenInputFluid.class, "tank", STEAM),
            rule(BlockEntityCokeOvenInputFluid.class, "tank", STEAM),
            rule(BlockEntityBlastFurnaceMain.class, "tank", STEAM),

            // ---- 生物质输入 → forge:biomass ----
            rule(BlockEntityBioGenerator.class, "fluidTank1", BIOMASS),
            rule(BlockEntitySteamBioGenerator.class, "fluidTank1", BIOMASS),
            rule(BlockEntityBioFuelGenerator.class, "fluidTank", BIOMASS),

            // ---- 除草剂输入 → forge:weed_ex ----
            rule(BlockEntityMultiCrop.class, "pestTank", WEED_EX),
            rule(BlockEntityFieldCleaner.class, "tank", WEED_EX),

            // ---- 冷却液输入 → forge:coolant ----
            rule(BlockEntityCyclotronCoolant.class, "fluids", COOLANT),
            rule(BlockEntityBaseSteamTurbineCoolant.class, "tank", COOLANT),

            // ---- 热冷却液输入 → forge:hot_coolant（tag 内当前仅 ic2 热冷却液，无行为放宽）----
            rule(BlockEntityGeothermalExchanger.class, "fluids", HOT_COOLANT),

            // ---- 热源（熔岩族）→ forge:pahoehoe_lava（多流体列表，union 语义）----
            rule(BlockEntityBaseHeatMachine.class, "fluidTank", PAHOEHOE_LAVA),
            rule(BlockEntitySmelteryFuelTank.class, "fluids", PAHOEHOE_LAVA),

            // ---- 蒸馏水输入 → forge:distilled_water ----
            rule(BlockEntitySingleFluidAdapter.class, "fluidTank1", DISTILLED_WATER),
    };

    private InputTagRegistry() {
    }

    /**
     * 由 {@code FluidsTankRegisterMixin.addTank} TAIL 调用：登记罐→tag 映射。
     * owner 由 mixin 经 {@code AbstractComponent.getParent()} 取好传入
     * （mixin handler 签名规则：参数位只允许目标方法参数 + CallbackInfo，见
     * CallbackInjector$Callback.getDescriptor 字节码实证，故不能把 Fluids 实例
     * 作为参数传入，改用 @Shadow getParent() 在 mixin 内取）。
     */
    public static void register(BlockEntityBase owner, Fluids.InternalFluidTank tank) {
        if (owner == null) {
            return;
        }
        TagKey<Fluid> tag = resolve(owner, tank.getIdentifier());
        if (tag != null) {
            synchronized (TANK_TAGS) {
                TANK_TAGS.put(tank, tag);
            }
        }
    }

    /**
     * 由 {@code InternalFluidTankAcceptMixin.acceptsFluid} HEAD 调用：取该罐应宽限的
     * tag（无则 null）。参数声明为 Object：mixin handler 的 this 是目标类实例
     * （InternalFluidTank），参数位不允许携带 this（见 register 注释），直接传 this
     * 自动向上转型为 Object；WeakHashMap.get(Object) 天然兼容。
     */
    public static TagKey<Fluid> tagFor(Object tank) {
        synchronized (TANK_TAGS) {
            return TANK_TAGS.get(tank);
        }
    }

    private static TagKey<Fluid> resolve(BlockEntityBase owner, String identifier) {
        for (Rule rule : RULES) {
            if (rule.owner.isInstance(owner) && rule.identifier.equals(identifier)) {
                return rule.tag;
            }
        }
        return null;
    }

    private static Rule rule(Class<?> owner, String identifier, TagKey<Fluid> tag) {
        return new Rule(owner, identifier, tag);
    }

    private static TagKey<Fluid> tag(String path) {
        return TagKey.create(Registries.FLUID, new ResourceLocation("forge", path));
    }

    private static final class Rule {
        final Class<?> owner;
        final String identifier;
        final TagKey<Fluid> tag;

        Rule(Class<?> owner, String identifier, TagKey<Fluid> tag) {
            this.owner = owner;
            this.identifier = identifier;
            this.tag = tag;
        }
    }
}
