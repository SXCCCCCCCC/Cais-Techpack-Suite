package com.sxcccccccc.kjsexportcmd;

import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.recipe.RecipeKey;
import dev.latvian.mods.kubejs.recipe.component.ItemComponents;
import dev.latvian.mods.kubejs.recipe.component.NumberComponent;
import dev.latvian.mods.kubejs.recipe.schema.RecipeSchema;
import dev.latvian.mods.kubejs.recipe.schema.RegisterRecipeSchemasEvent;
import net.minecraft.resources.ResourceLocation;

/**
 * 让 KubeJS 认识 ic2_120:compressing 配方类型（IC2 Fabric 版无 KubeJS 集成）。
 *
 * <p>IC2 压缩机配方 JSON：{type, ingredient: {item|tag}, input_count: int, result: {count, item}}。
 * 注册 schema 后 recipes 事件可遍历/删除/覆写压缩机配方（否则 unknown type 被 KubeJS 静默跳过）。
 */
public class Ic2RecipeSchemaPlugin extends KubeJSPlugin {

    @Override
    public void registerRecipeSchemas(RegisterRecipeSchemasEvent event) {
        event.register(new ResourceLocation("ic2_120", "compressing"), new RecipeSchema(
                new RecipeKey(ItemComponents.INPUT, "ingredient"),
                new RecipeKey(NumberComponent.INT, "input_count"),
                new RecipeKey(ItemComponents.OUTPUT, "result")
        ));
    }
}
