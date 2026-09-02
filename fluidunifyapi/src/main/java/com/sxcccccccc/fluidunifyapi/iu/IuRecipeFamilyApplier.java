package com.sxcccccccc.fluidunifyapi.iu;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseFluidMachineRecipe;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.Input;
import com.denfop.api.recipe.InputFluid;
import com.denfop.api.recipe.IInputFluid;
import com.denfop.api.recipe.IRecipeInputFluidStack;
import com.denfop.api.recipe.RecipeOutput;
import com.denfop.api.recipe.RecipesFluidCore;
import com.denfop.recipe.IInputItemStack;
import com.sxcccccccc.fluidunifyapi.FluidUnifyApi;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch.Mode;
import com.sxcccccccc.fluidunifyapi.core.MachineAdapters;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * IU 配方族应用器：把补丁落成运行时配方表里的真实配方（45 台 FluidHandlerRecipe
 * 机器的通用实现，零 mixin，纯公开 API + 公开字段）。
 *
 * <p>机制（3.4.0.10 dev 树实证）：</p>
 * <ul>
 *   <li>ADD：按模板配方克隆——输入流体换新流体，输出照抄（只改输入），物品侧
 *       与流体侧配方成对维护（IU 约定两侧共存，SOP 教训）；</li>
 *   <li>REPLACE：先删除输入含模板流体的配方（流体侧 map 手术 + 物品侧按输入流体
 *       手术 + map_recipe_managers_itemStack 维护），再按 ADD 添加新配方；</li>
 *   <li>幂等：添加前查重（输入/输出全等跳过）；删除/添加都是可重入的集合操作；
 *       单人档 TagsUpdatedEvent 双端触发 + /reload 重复执行安全；</li>
 *   <li>新机器语义：补丁在世界加载期应用，之后放置的机器 BE 构造时谓词/物品槽
 *       快照自然包含新流体（FluidHandlerRecipe 在构造时读配方表，无需刷新逻辑）；</li>
 *   <li>JEI：IU 的 JEI handler 活读本配方表（如 FluidMixerHandler.initRecipes），
 *       新配方自动显示——硬性要求零代码满足。</li>
 * </ul>
 */
public final class IuRecipeFamilyApplier {

    private IuRecipeFamilyApplier() {
    }

    /** 仅当补丁目标属于 IU 配方族时应用；否则直接返回（谓词族走 mixin 探针路径）。 */
    public static void applyIfRecipeMachine(FluidPatch patch) {
        if (!MachineAdapters.isIuRecipeMachine(patch.machineId)) {
            return;
        }
        String name = patch.machineId.substring("industrialupgrade:".length());
        if (MachineAdapters.isIuItemRecipeMachine(patch.machineId)) {
            applyItemSidePatch(patch, name);
            return;
        }
        applyFluidSidePatch(patch, name);
    }

    /**
     * 物品侧管理器机型（elec_refractory_furnace/plastic/plasticplate）：管理器仅存在于
     * RecipesCore（物品侧），流体内嵌在 BaseMachineRecipe.input 的 FluidStack 里，
     * 无流体侧配方可克隆。补丁落法 = 物品侧配方克隆（输入流体换新流体，物品与输出照抄）。
     */
    private static void applyItemSidePatch(FluidPatch patch, String name) {
        List<BaseMachineRecipe> recipes = Recipes.recipes.getRecipeList(name);
        if (recipes == null || recipes.isEmpty()) {
            throw new IllegalArgumentException(
                    "[fluidunifyapi] IU item-side machine " + patch.machineId
                            + " has no item recipes registered");
        }
        Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
        if (template == null) {
            return; // 解析失败已由注册表打日志
        }
        List<BaseMachineRecipe> templateRecipes = new ArrayList<>();
        for (BaseMachineRecipe r : recipes) {
            if (itemInputHasTemplate(r.input, template)) {
                templateRecipes.add(r);
            }
        }
        if (templateRecipes.isEmpty()) {
            if (patch.mode == Mode.REPLACE) {
                // REPLACE 重放（/reload、双端重复触发）：模板配方已被上次应用删除，
                // 视为已应用完成直接返回——重放安全（ADD 仍 fail-fast 揪拼写错误）。
                FluidUnifyApi.LOGGER.warn("[fluidunifyapi] IU {} REPLACE: no template recipes left, "
                        + "assuming already applied (replay), skip", name);
                return;
            }
            throw new IllegalArgumentException(
                    "[fluidunifyapi] IU machine " + patch.machineId + " has no recipe taking fluid "
                            + patch.templateFluid + " — template fluid does not exist on this machine");
        }
        if (patch.mode == Mode.REPLACE) {
            recipes.removeAll(templateRecipes);
            FluidUnifyApi.LOGGER.info("[fluidunifyapi] IU -recipe {}: removed {} item recipes taking {}",
                    name, templateRecipes.size(), ForgeKey(template));
        }
        for (Fluid newFluid : expandAddedFluids(patch)) {
            if (newFluid == template) {
                continue;
            }
            for (BaseMachineRecipe templateRecipe : templateRecipes) {
                if (itemInputHasTemplate(templateRecipe.input, newFluid)) {
                    continue; // 已有该流体配方（幂等）
                }
                BaseMachineRecipe cloned = cloneItemRecipe(templateRecipe, template, newFluid);
                // BaseMachineRecipe 无 equals（3.4.0.10 反编译实证），contains 恒 false；
                // 用输入流体（id+数量）自定义等价——我们的克隆只改流体，输入流体相同即重复。
                if (!containsItemEquivalent(recipes, cloned)) {
                    Recipes.recipes.addRecipe(name, cloned);
                    FluidUnifyApi.LOGGER.info("[fluidunifyapi] IU +recipe {}: input fluid {} -> {}",
                            name, patch.templateFluid, ForgeKey(newFluid));
                }
            }
        }
    }

    /** 克隆物品侧配方：输入里模板流体换新流体（同数量+NBT），物品输入与输出照抄。 */
    private static BaseMachineRecipe cloneItemRecipe(BaseMachineRecipe template, Fluid from, Fluid to) {
        com.denfop.api.recipe.IInput in = template.input;
        FluidStack single = in.getFluid();
        if (single != null && !single.isEmpty() && single.getFluid() == from) {
            return new BaseMachineRecipe(
                    new Input(new FluidStack(to, single.getAmount(), single.getTag()),
                            in.getInputs().toArray(new IInputItemStack[0])),
                    template.getOutput());
        }
        List<FluidStack> fluids = in.getFluidInputs();
        if (fluids == null || fluids.isEmpty()) {
            throw new IllegalArgumentException("[fluidunifyapi] item recipe input has no fluid to replace");
        }
        FluidStack[] replaced = new FluidStack[fluids.size()];
        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fs = fluids.get(i);
            replaced[i] = fs != null && fs.getFluid() == from
                    ? new FluidStack(to, fs.getAmount(), fs.getTag())
                    : fs.copy();
        }
        return new BaseMachineRecipe(new Input(replaced), template.getOutput());
    }

    /** 流体侧配方族主路径（原有逻辑）。 */
    private static void applyFluidSidePatch(FluidPatch patch, String name) {
        RecipesFluidCore core = Recipes.recipes.getRecipeFluid();
        List<BaseFluidMachineRecipe> recipes = core.getRecipeList(name);

        Fluid template = UnifiedFluidRegistry.resolveFluidId(patch.templateFluid);
        if (template == null) {
            return; // 解析失败已由注册表打日志
        }

        // 模板配方 = 输入流体列表里含模板流体的配方（模板流体定位"输入口"）
        List<BaseFluidMachineRecipe> templateRecipes = new ArrayList<>();
        for (BaseFluidMachineRecipe r : recipes) {
            if (inputHas(r.input, template)) {
                templateRecipes.add(r);
            }
        }
        if (templateRecipes.isEmpty()) {
            throw new IllegalArgumentException(
                    "[fluidunifyapi] IU machine " + patch.machineId + " has no recipe taking fluid "
                            + patch.templateFluid + " — template fluid does not exist on this machine");
        }

        if (patch.mode == Mode.REPLACE) {
            removeTemplateRecipes(name, core, templateRecipes, template);
        }

        for (Fluid newFluid : expandAddedFluids(patch)) {
            if (newFluid == template) {
                continue; // 等于模板流体的"新增"由模板配方原样覆盖
            }
            for (BaseFluidMachineRecipe templateRecipe : templateRecipes) {
                if (inputHas(templateRecipe.input, newFluid)) {
                    continue; // 已有该流体配方（幂等）
                }
                BaseFluidMachineRecipe cloned = cloneFluidRecipe(templateRecipe, template, newFluid);
                if (!containsEquivalent(core.getRecipeList(name), cloned)) {
                    core.addRecipe(name, cloned);
                    addItemSidePair(name, templateRecipe, template, newFluid);
                    FluidUnifyApi.LOGGER.info("[fluidunifyapi] IU +recipe {}: input fluid {} -> {}",
                            name, patch.templateFluid, ForgeKey(newFluid));
                }
            }
        }
    }

    // ==================== 删除（REPLACE） ====================

    private static void removeTemplateRecipes(String name, RecipesFluidCore core,
                                              List<BaseFluidMachineRecipe> templateRecipes, Fluid template) {
        List<BaseFluidMachineRecipe> list = core.map_recipes_fluid.get(name);
        if (list != null) {
            list.removeAll(templateRecipes);
        }
        // map_recipe_managers_itemStack 维护（输入快照表；配套删除否则残留脏数据）
        List<IRecipeInputFluidStack> inputStacks = core.map_recipe_managers_itemStack.get(name);
        if (inputStacks != null) {
            inputStacks.removeIf(s -> stackListHasTemplate(s.getItemStack(), template));
        }
        // 物品侧配方：按"输入流体含模板"手术（BaseMachineRecipe.input.getFluid()/getFluidInputs()）
        List<BaseMachineRecipe> itemRecipes = Recipes.recipes.getRecipeList(name);
        if (itemRecipes != null) {
            itemRecipes.removeIf(r -> itemInputHasTemplate(r.input, template));
        }
        FluidUnifyApi.LOGGER.info("[fluidunifyapi] IU -recipe {}: removed {} recipes taking {}",
                name, templateRecipes.size(), ForgeKey(template));
    }

    private static boolean stackListHasTemplate(List<FluidStack> stacks, Fluid template) {
        if (stacks == null) {
            return false;
        }
        for (FluidStack s : stacks) {
            if (s != null && s.getFluid() == template) {
                return true;
            }
        }
        return false;
    }

    private static boolean itemInputHasTemplate(com.denfop.api.recipe.IInput input, Fluid template) {
        FluidStack single = input.getFluid();
        if (single != null && !single.isEmpty() && single.getFluid() == template) {
            return true;
        }
        return stackListHasTemplate(input.getFluidInputs(), template);
    }

    // ==================== 添加（ADD / REPLACE 的补加） ====================

    private static Set<Fluid> expandAddedFluids(FluidPatch patch) {
        if (patch.matchType == FluidPatch.MatchType.TAG) {
            TagKey<Fluid> key = TagKey.create(net.minecraft.core.registries.Registries.FLUID,
                    new net.minecraft.resources.ResourceLocation(patch.tag));
            Set<Fluid> out = UnifiedFluidRegistry.expandTag(key);
            if (out.isEmpty()) {
                FluidUnifyApi.LOGGER.warn("[fluidunifyapi] tag {} expands to no fluids (typo? empty tag?) — "
                        + "no recipes added for {}", patch.tag, patch.machineId);
            }
            return out;
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

    /** 克隆流体侧配方：输入里模板流体换新流体（同数量），其余输入与全部输出照抄。 */
    private static BaseFluidMachineRecipe cloneFluidRecipe(BaseFluidMachineRecipe template, Fluid from, Fluid to) {
        IInputFluid oldInput = template.input;
        List<FluidStack> oldFluids = oldInput.getInputs();
        List<FluidStack> newFluids = new ArrayList<>(oldFluids.size());
        for (FluidStack fs : oldFluids) {
            newFluids.add(fs.getFluid() == from
                    ? new FluidStack(to, fs.getAmount(), fs.getTag())
                    : fs.copy());
        }
        List<FluidStack> outFluids = new ArrayList<>(template.output_fluid.size());
        for (FluidStack fs : template.output_fluid) {
            outFluids.add(fs.copy());
        }
        InputFluid newInput = oldInput.getStack() != null
                ? new InputFluid(oldInput.getStack(), newFluids.toArray(new FluidStack[0]))
                : new InputFluid(newFluids.toArray(new FluidStack[0]));
        RecipeOutput out = template.getOutput();
        if (out != null) {
            return new BaseFluidMachineRecipe(newInput, out, outFluids);
        }
        return new BaseFluidMachineRecipe(newInput, outFluids);
    }

    /** 物品侧配方成对添加：输入流体换新流体，物品输入与输出照抄。 */
    private static void addItemSidePair(String name, BaseFluidMachineRecipe template,
                                        Fluid from, Fluid to) {
        List<BaseMachineRecipe> itemRecipes = Recipes.recipes.getRecipeList(name);
        if (itemRecipes == null || itemRecipes.isEmpty()) {
            return; // 纯流体机（如 fluid_mixer）无物品侧
        }
        // 找到与模板配方对应的物品侧配方（输入流体 == 模板）
        for (BaseMachineRecipe itemRecipe : itemRecipes) {
            com.denfop.api.recipe.IInput in = itemRecipe.input;
            FluidStack single = in.getFluid();
            if (single != null && !single.isEmpty() && single.getFluid() == from) {
                BaseMachineRecipe cloned = new BaseMachineRecipe(
                        new Input(new FluidStack(to, single.getAmount(), single.getTag()),
                                in.getInputs().toArray(new IInputItemStack[0])),
                        itemRecipe.getOutput()
                );
                if (!itemRecipes.contains(cloned)) {
                    Recipes.recipes.addRecipe(name, cloned);
                }
                return;
            }
            List<FluidStack> fluids = in.getFluidInputs();
            if (fluids != null && !fluids.isEmpty() && stackListHasTemplate(fluids, from)) {
                FluidStack[] replaced = new FluidStack[fluids.size()];
                for (int i = 0; i < fluids.size(); i++) {
                    FluidStack fs = fluids.get(i);
                    replaced[i] = fs != null && fs.getFluid() == from
                            ? new FluidStack(to, fs.getAmount(), fs.getTag())
                            : fs.copy();
                }
                BaseMachineRecipe cloned = new BaseMachineRecipe(new Input(replaced), itemRecipe.getOutput());
                if (!itemRecipes.contains(cloned)) {
                    Recipes.recipes.addRecipe(name, cloned);
                }
                return;
            }
        }
    }

    /** 物品侧配方查重（幂等守卫）：输入流体（id+数量）与输入物品（数量+每元素
     * 输入列表 id 序列）全等（BaseMachineRecipe 无 equals；同流体不同物品的配方
     * 在电耐火炉等机型大量存在，只比流体会把第二个及以后的克隆误判为重复）。 */
    private static boolean containsItemEquivalent(List<BaseMachineRecipe> recipes, BaseMachineRecipe candidate) {
        for (BaseMachineRecipe r : recipes) {
            if (itemInputFluidsEqual(r.input, candidate.input)
                    && itemInputsEqual(r.input.getInputs(), candidate.input.getInputs())) {
                return true;
            }
        }
        return false;
    }

    private static boolean itemInputsEqual(List<IInputItemStack> a, List<IInputItemStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            IInputItemStack x = a.get(i);
            IInputItemStack y = b.get(i);
            if (x.getAmount() != y.getAmount()) {
                return false;
            }
            List<ItemStack> xi = x.getInputs();
            List<ItemStack> yi = y.getInputs();
            if (xi.size() != yi.size()) {
                return false;
            }
            for (int j = 0; j < xi.size(); j++) {
                if (xi.get(j).getItem() != yi.get(j).getItem()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean itemInputFluidsEqual(com.denfop.api.recipe.IInput a, com.denfop.api.recipe.IInput b) {
        FluidStack sa = a.getFluid();
        FluidStack sb = b.getFluid();
        if (sa != null && sb != null && !sa.isEmpty() && !sb.isEmpty()) {
            return sa.getFluid() == sb.getFluid() && sa.getAmount() == sb.getAmount();
        }
        List<FluidStack> la = a.getFluidInputs();
        List<FluidStack> lb = b.getFluidInputs();
        if (la == null || lb == null || la.size() != lb.size()) {
            return false;
        }
        for (int i = 0; i < la.size(); i++) {
            FluidStack x = la.get(i);
            FluidStack y = lb.get(i);
            if (x == null || y == null || x.getFluid() != y.getFluid() || x.getAmount() != y.getAmount()) {
                return false;
            }
        }
        return true;
    }

    /** 配方查重（幂等守卫）：输入流体（id+数量）与输出（id+数量）全等。 */
    private static boolean containsEquivalent(List<BaseFluidMachineRecipe> recipes, BaseFluidMachineRecipe candidate) {
        for (BaseFluidMachineRecipe r : recipes) {
            if (fluidsEqual(r.input.getInputs(), candidate.input.getInputs())
                    && fluidsEqual(r.output_fluid, candidate.output_fluid)) {
                return true;
            }
        }
        return false;
    }

    private static boolean fluidsEqual(List<FluidStack> a, List<FluidStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            FluidStack x = a.get(i);
            FluidStack y = b.get(i);
            if (x.getFluid() != y.getFluid() || x.getAmount() != y.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean inputHas(IInputFluid input, Fluid fluid) {
        List<FluidStack> stacks = input.getInputs();
        for (FluidStack s : stacks) {
            if (s != null && s.getFluid() == fluid) {
                return true;
            }
        }
        return false;
    }

    private static String ForgeKey(Fluid fluid) {
        var loc = net.minecraftforge.registries.ForgeRegistries.FLUIDS.getKey(fluid);
        return loc == null ? String.valueOf(fluid) : loc.toString();
    }

    // 保留引用以免误删（ItemStack 仅用于将来物品侧克隆扩展）
    @SuppressWarnings("unused")
    private static ItemStack copyStack(ItemStack stack) {
        return stack.copy();
    }
}
