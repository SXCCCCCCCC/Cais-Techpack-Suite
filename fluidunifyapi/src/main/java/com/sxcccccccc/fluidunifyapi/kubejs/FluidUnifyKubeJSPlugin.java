package com.sxcccccccc.fluidunifyapi.kubejs;

import com.sxcccccccc.fluidunifyapi.ifmod.IfDatapackFamilyApplier;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipesEventJS;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.util.ClassFilter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.Map;

/**
 * 本 mod 的 KubeJS 插件（经 jar 根 {@code kubejs.plugins.txt} 装载，GTCEu 同款机制）。
 *
 * <p>职责：</p>
 * <ul>
 *   <li>{@link #registerEvents()}：注册 FluidUnifyEvents 事件组（脚本自动获得绑定）；</li>
 *   <li>{@link #injectRuntimeRecipes(...)}：IF datapack 配方族（dissolution_chamber）
 *       补丁的<b>客户端</b>落地——KubeJS 在每侧配方装配管线的末尾调用本钩子，
 *       单人档里客户端管线晚于服务端 unify 事件（同 JVM 静态共享，补丁表已就绪），
 *       由此客户端 RecipeManager 与 JEI 同步看到新配方；服务端侧由
 *       FluidUnifyListener 在 TagsUpdatedEvent 时直写 RecipeManager（管线里补丁
 *       尚未应用、客户端管线又跑得太晚，两端都靠不上，只能两侧各打一次且幂等）。</li>
 *   <li>{@link #registerClasses(...)}：放开本 mod 核心包给脚本反射
 *       （Java.loadClass 兜底路径，iucore RecipeHelper 先例）。</li>
 * </ul>
 */
public class FluidUnifyKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void registerEvents() {
        FluidUnifyEvents.GROUP.register();
    }

    @Override
    public void registerClasses(dev.latvian.mods.kubejs.script.ScriptType type, ClassFilter filter) {
        // Java.loadClass 兜底路径：允许脚本直接调用核心 API
        filter.allow("com.sxcccccccc.fluidunifyapi.core");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        // 事件组绑定由 KubeJS 插件机制自动处理（EventGroup.register 后脚本侧
        // 自动出现 FluidUnifyEvents.unify），无需手动 add。
    }

    @Override
    public void injectRuntimeRecipes(RecipesEventJS event, RecipeManager manager, Map<ResourceLocation, Recipe<?>> recipesByName) {
        IfDatapackFamilyApplier.applyToRecipeMap(recipesByName);
    }
}
