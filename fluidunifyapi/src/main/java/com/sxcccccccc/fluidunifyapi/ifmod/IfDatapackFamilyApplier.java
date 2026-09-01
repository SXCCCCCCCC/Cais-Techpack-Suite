package com.sxcccccccc.fluidunifyapi.ifmod;

import com.buuz135.industrial.module.ModuleCore;
import com.buuz135.industrial.recipe.DissolutionChamberRecipe;
import com.sxcccccccc.fluidunifyapi.FluidUnifyApi;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch.Mode;
import com.sxcccccccc.fluidunifyapi.core.MachineAdapters;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.ifmod.mixin.RecipeManagerAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IF datapack 配方族应用器（v1 唯一成员 = dissolution_chamber，IF 3.5.22 里唯一
 * 带流体输入的 datapack 配方机；流体提取机/激光钻是输出侧，用户裁决不碰）。
 *
 * <p>双通道落地（vanilla RecipeManager 两侧各自独立，必须双写）：</p>
 * <ul>
 *   <li>服务端：TagsUpdatedEvent 时直写 {@code server.getRecipeManager().byName/
 *       recipes}——KubeJS 配方管线在服务端跑得太早（补丁尚未生成），只能事后直写；</li>
 *   <li>客户端：KubeJS 插件 {@code injectRuntimeRecipes} 钩子（配方装配管线末尾，
 *       单人档客户端管线晚于服务端 unify 事件、同 JVM 静态共享 → 补丁表已就绪）
 *       → 客户端 RecipeManager 与 JEI 同步看到新配方。</li>
 * </ul>
 *
 * <p>幂等：新配方 id 由（模板流体 id + 新流体 id）确定性生成，put 覆盖；
 * REPLACE 的删除是集合 remove（重复执行无副作用、模板消失后跳过不抛错）。</p>
 */
public final class IfDatapackFamilyApplier {

    private IfDatapackFamilyApplier() {
    }

    /** 服务端直写（FluidUnifyListener 在 applyAll 之后调用）。 */
    public static void applyServerSide(List<FluidPatch> patches) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        RecipeManager rm = server.getRecipeManager();
        boolean anyDissolution = patches.stream()
                .anyMatch(p -> p.machineId.equals("industrialforegoing:dissolution_chamber"));
        if (!anyDissolution) {
            return;
        }
        // byName 与 recipes（byType 索引）必须同步改——JEI/配方查找走两边
        RecipeManagerAccessor accessor = (RecipeManagerAccessor) rm;
        Map<ResourceLocation, Recipe<?>> byName = new HashMap<>(accessor.fluidunifyapi$getByName());
        applyToRecipeMap(byName);
        Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> byType = new HashMap<>();
        for (var entry : byName.entrySet()) {
            byType.computeIfAbsent(entry.getValue().getType(), k -> new HashMap<>())
                    .put(entry.getKey(), entry.getValue());
        }
        accessor.fluidunifyapi$setByName(byName);
        accessor.fluidunifyapi$setByType(byType);
    }

    /** 客户端钩子（FluidUnifyKubeJSPlugin.injectRuntimeRecipes）与上共用同一实现。 */
    public static void applyToRecipeMap(Map<ResourceLocation, Recipe<?>> recipesByName) {
        for (FluidPatch patch : UnifiedFluidRegistry.allRawPatches()) {
            if (!MachineAdapters.isIfDatapackMachine(patch.machineId)) {
                continue;
            }
            if (!patch.machineId.equals("industrialforegoing:dissolution_chamber")) {
                continue; // v1 只做有流体输入的溶解室；其余 datapack 成员报错留给校验层
            }
            Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
            if (template == null) {
                continue;
            }
            List<Map.Entry<ResourceLocation, Recipe<?>>> templateEntries = new ArrayList<>();
            for (var e : recipesByName.entrySet()) {
                if (e.getValue() instanceof DissolutionChamberRecipe r
                        && r.inputFluid != null && !r.inputFluid.isEmpty()
                        && r.inputFluid.getFluid() == template) {
                    templateEntries.add(e);
                }
            }
            if (templateEntries.isEmpty()) {
                if (patch.mode == Mode.REPLACE) {
                    continue; // 模板配方已删（幂等重跑/客户端第二次进入）
                }
                throw new IllegalArgumentException(
                        "[fluidunifyapi] dissolution_chamber has no recipe taking fluid "
                                + patch.templateFluid + " — template fluid does not exist on this machine");
            }
            if (patch.mode == Mode.REPLACE) {
                for (var e : templateEntries) {
                    recipesByName.remove(e.getKey());
                }
                FluidUnifyApi.LOGGER.info("[fluidunifyapi] IF -recipe dissolution_chamber: removed {} recipes taking {}",
                        templateEntries.size(), templateFluidKey(template));
            }
            for (Fluid newFluid : expandAdded(patch)) {
                if (newFluid == template) {
                    continue;
                }
                for (var e : templateEntries) {
                    DissolutionChamberRecipe tr = (DissolutionChamberRecipe) e.getValue();
                    if (tr.inputFluid.getFluid() == newFluid) {
                        continue;
                    }
                    ResourceLocation newId = new ResourceLocation("fluidunifyapi",
                            "if_dissolution_" + templateFluidKey(template) + "_" + fluidKey(newFluid));
                    recipesByName.put(newId, new DissolutionChamberRecipe(
                            newId,
                            tr.input,
                            new FluidStack(newFluid, tr.inputFluid.getAmount()),
                            tr.processingTime,
                            tr.output,
                            tr.outputFluid
                    ));
                    FluidUnifyApi.LOGGER.info("[fluidunifyapi] IF +recipe dissolution_chamber: {} -> {}",
                            templateFluidKey(template), fluidKey(newFluid));
                }
            }
        }
    }

    private static Set<Fluid> expandAdded(FluidPatch patch) {
        if (patch.matchType == FluidPatch.MatchType.TAG) {
            return UnifiedFluidRegistry.expandTag(net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.FLUID,
                    new ResourceLocation(patch.tag)));
        }
        Set<Fluid> out = new java.util.HashSet<>();
        for (String id : patch.fluids) {
            Fluid f = UnifiedFluidRegistry.resolveFluidId(id);
            if (f != null) {
                out.add(f);
            }
        }
        return out;
    }

    private static String fluidKey(Fluid fluid) {
        var loc = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid);
        return loc == null ? String.valueOf(fluid) : loc.toString().replace(':', '_').replace('/', '_');
    }

    private static String templateFluidKey(Fluid fluid) {
        return fluidKey(fluid);
    }

    @SuppressWarnings("unused")
    private static RecipeType<?> dissolutionType() {
        return ModuleCore.DISSOLUTION_TYPE.get();
    }
}
