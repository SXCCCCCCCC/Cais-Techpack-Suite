package com.sxcccccccc.fluidunifyapi.ifmod.mixin;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * RecipeManager 私有字段 accessor（1.20.1 原版类，remap 按默认 true，
 * official 映射名 byName/recipes）。IF datapack 族服务端直写用
 * （KubeJS 的 RecipeManagerAccessor 同款手法，但那是它内部的）。
 */
@Mixin(RecipeManager.class)
public interface RecipeManagerAccessor {

    @Accessor("byName")
    Map<ResourceLocation, Recipe<?>> fluidunifyapi$getByName();

    @Accessor("byName")
    void fluidunifyapi$setByName(Map<ResourceLocation, Recipe<?>> map);

    @Accessor("recipes")
    Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> fluidunifyapi$getByType();

    @Accessor("recipes")
    void fluidunifyapi$setByType(Map<RecipeType<?>, Map<ResourceLocation, Recipe<?>>> map);
}
