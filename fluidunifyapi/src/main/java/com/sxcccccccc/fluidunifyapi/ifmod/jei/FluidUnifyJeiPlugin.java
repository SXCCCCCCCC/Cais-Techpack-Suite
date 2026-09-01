package com.sxcccccccc.fluidunifyapi.ifmod.jei;

import com.buuz135.industrial.api.recipe.ore.OreFluidEntryFermenter;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntryRaw;
import com.buuz135.industrial.api.recipe.ore.OreFluidEntrySieve;
import com.buuz135.industrial.fluid.OreTitaniumFluidType;
import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.plugin.jei.IndustrialRecipeTypes;
import com.buuz135.industrial.plugin.jei.category.BioReactorRecipeCategory.ReactorRecipeWrapper;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 本 mod 的 JEI 插件：给 IF 现有 JEI 分类补"新输入流体"条目（只改输入——输出
 * 一律照抄原生条目，矿石族连矿种 NBT 都原样保留）。
 *
 * <p>覆盖矩阵（IF 3.5.22 反编译实证，只有这些机器有 JEI 分类；其余 isSame
 * 机器无分类，无条目可补）：</p>
 * <ul>
 *   <li>bioreactor → IndustrialRecipeTypes.BIOREACTOR：条目 = 原生物品 tag × 新输入流体；</li>
 *   <li>washing_factory → ORE_WASHER：枚举 forge:raw_materials/* tag，输入流体换新、
 *       输出肉汤（含矿种 NBT）照抄；</li>
 *   <li>fermentation_station → FERMENTER：同上，输入 raw ore meat（含矿种 NBT）；</li>
 *   <li>fluid_sieving_machine → ORE_SIEVE：同上，输入 fermented ore meat（含矿种 NBT）。</li>
 * </ul>
 *
 * <p>注：IU 的 JEI 不需要本插件（IU JEI handler 活读运行时配方表，配方族补丁
 * 加进去自动显示）；dissolution_chamber 的 JEI 走 RecipeManager 活读 + 客户端
 * injectRuntimeRecipes 双写，也不需要本插件。</p>
 */
@JeiPlugin
public class FluidUnifyJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return new ResourceLocation("fluidunifyapi", "jei");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ReactorRecipeWrapper> bioreactor = new ArrayList<>();
        List<OreFluidEntryRaw> washer = new ArrayList<>();
        List<OreFluidEntryFermenter> fermenter = new ArrayList<>();
        List<OreFluidEntrySieve> sieve = new ArrayList<>();

        for (FluidPatch patch : UnifiedFluidRegistry.allRawPatches()) {
            String machine = patch.machineId;
            List<Fluid> added = addedFluids(patch);
            switch (machine) {
                case "industrialforegoing:bioreactor" -> {
                    Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
                    if (template == null || !isNativeInput("industrialforegoing:biofuel", template)) {
                        continue;
                    }
                    FluidStack nativeInput = new FluidStack(ModuleCore.BIOFUEL.getSourceFluid().get(), 80);
                    for (Fluid f : added) {
                        for (TagKey<Item> itemTag : nativeBioreactorTags()) {
                            bioreactor.add(new ReactorRecipeWrapper(itemTag, new FluidStack(f, nativeInput.getAmount())));
                        }
                    }
                }
                case "industrialforegoing:washing_factory",
                     "industrialforegoing:fermentation_station",
                     "industrialforegoing:fluid_sieving_machine" -> {
                    Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
                    if (template == null) {
                        continue;
                    }
                    boolean isWasher = machine.equals("industrialforegoing:washing_factory");
                    boolean isFermenter = machine.equals("industrialforegoing:fermentation_station");
                    Fluid nativeFluid = isWasher
                            ? (Fluid) ModuleCore.MEAT.getSourceFluid().get()
                            : isFermenter ? ModuleCore.RAW_ORE_MEAT.getSourceFluid()
                            : ModuleCore.FERMENTED_ORE_MEAT.getSourceFluid();
                    if (!isNativeInput(ForgeRegistries.FLUIDS.getKey(nativeFluid).toString(), template)) {
                        continue;
                    }
                    for (Fluid f : added) {
                        for (ResourceLocation oreTagId : oreTags()) {
                            // 输出侧照抄原生条目（含矿种 NBT 的肉汤/矿粉），只换输入流体
                            if (isWasher) {
                                washer.add(new OreFluidEntryRaw(
                                        itemTag(oreTagId),
                                        new FluidStack(f, 100),
                                        OreTitaniumFluidType.getFluidWithTag(
                                                ModuleCore.RAW_ORE_MEAT, 100, oreTagId)
                                ));
                            } else if (isFermenter) {
                                FluidStack in = new FluidStack(f, 100);
                                in.getOrCreateTag().putString(OreTitaniumFluidType.NBT_TAG, oreTagId.toString());
                                fermenter.add(new OreFluidEntryFermenter(
                                        in,
                                        OreTitaniumFluidType.getFluidWithTag(
                                                ModuleCore.FERMENTED_ORE_MEAT, 200, oreTagId)
                                ));
                            } else {
                                FluidStack in = new FluidStack(f, 100);
                                in.getOrCreateTag().putString(OreTitaniumFluidType.NBT_TAG, oreTagId.toString());
                                sieve.add(new OreFluidEntrySieve(
                                        in,
                                        OreTitaniumFluidType.getOutputDust(in),
                                        net.minecraft.tags.ItemTags.create(new ResourceLocation("forge", "sand"))
                                ));
                            }
                        }
                    }
                }
                default -> {
                    // 其余机器无 JEI 分类或由其他通道（RecipeManager/IU 活读）覆盖
                }
            }
        }

        if (!bioreactor.isEmpty()) {
            registration.addRecipes(IndustrialRecipeTypes.BIOREACTOR, bioreactor);
        }
        if (!washer.isEmpty()) {
            registration.addRecipes(IndustrialRecipeTypes.ORE_WASHER, washer);
        }
        if (!fermenter.isEmpty()) {
            registration.addRecipes(IndustrialRecipeTypes.FERMENTER, fermenter);
        }
        if (!sieve.isEmpty()) {
            registration.addRecipes(IndustrialRecipeTypes.ORE_SIEVE, sieve);
        }
    }

    // ==================== 原生条目枚举 ====================

    /** 与 IF JEI 插件同款枚举：forge:raw_materials/* 且对应 forge:dusts/* 非空。 */
    private static List<ResourceLocation> oreTags() {
        List<ResourceLocation> out = new ArrayList<>();
        for (var holder : ForgeRegistries.ITEMS.tags().getTagNames().toList()) {
            ResourceLocation id = holder.location();
            if (id.toString().startsWith("forge:raw_materials/") && OreTitaniumFluidType.isValid(id)) {
                out.add(id);
            }
        }
        return out;
    }

    /** BioReactorTile.VALID 的反射读取（客户端只读枚举，避免引 BioReactorTile 的其余依赖）。 */
    @SuppressWarnings("unchecked")
    private static List<TagKey<Item>> nativeBioreactorTags() {
        try {
            Class<?> cls = Class.forName("com.buuz135.industrial.block.generator.tile.BioReactorTile");
            java.lang.reflect.Field field = cls.getField("VALID");
            return (List<TagKey<Item>>) field.get(null);
        } catch (Throwable t) {
            return List.of();
        }
    }

    private static TagKey<Item> itemTag(ResourceLocation id) {
        return TagKey.create(Registries.ITEM, id);
    }

    private static List<Fluid> addedFluids(FluidPatch patch) {
        if (patch.matchType == FluidPatch.MatchType.TAG) {
            return new ArrayList<>(UnifiedFluidRegistry.expandTag(TagKey.create(
                    Registries.FLUID, new ResourceLocation(patch.tag))));
        }
        List<Fluid> out = new ArrayList<>();
        for (String id : patch.fluids) {
            Fluid f = UnifiedFluidRegistry.resolveFluidId(id);
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    /** 模板流体是否机器原生输入（防御：非原生模板不应在 JEI 出现）。 */
    private static boolean isNativeInput(String nativeFluidId, Fluid template) {
        var loc = ForgeRegistries.FLUIDS.getKey(template);
        return loc != null && loc.toString().equals(nativeFluidId);
    }
}
