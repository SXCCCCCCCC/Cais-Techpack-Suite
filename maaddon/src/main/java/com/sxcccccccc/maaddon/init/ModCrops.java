package com.sxcccccccc.maaddon.init;

import com.blakebr0.mysticalagriculture.api.crop.Crop;
import com.blakebr0.mysticalagriculture.api.crop.CropTier;
import com.blakebr0.mysticalagriculture.api.crop.CropType;
import com.blakebr0.mysticalagriculture.api.lib.LazyIngredient;
import com.blakebr0.mysticalagriculture.api.registry.ICropRegistry;
import net.minecraft.resources.ResourceLocation;

import static com.sxcccccccc.maaddon.MaAddon.MOD_ID;

public final class ModCrops {
	public static final Crop SALT = new Crop(new ResourceLocation(MOD_ID, "salt"), CropTier.ONE, CropType.RESOURCE, LazyIngredient.item("mekanism:salt"));
	public static final Crop PLASTIC = new Crop(new ResourceLocation(MOD_ID, "plastic"), CropTier.TWO, CropType.RESOURCE, LazyIngredient.tag("forge:plastic"));
	public static final Crop PINK_SLIME = new Crop(new ResourceLocation(MOD_ID, "pink_slime"), CropTier.FOUR, CropType.RESOURCE, LazyIngredient.item("industrialforegoing:pink_slime"));

	public static void onRegisterCrops(ICropRegistry registry) {
		registry.register(SALT);
		registry.register(PLASTIC);
		registry.register(PINK_SLIME);
	}
}
