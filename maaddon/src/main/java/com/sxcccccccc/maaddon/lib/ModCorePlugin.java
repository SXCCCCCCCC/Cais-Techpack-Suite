package com.sxcccccccc.maaddon.lib;

import com.blakebr0.mysticalagriculture.api.IMysticalAgriculturePlugin;
import com.blakebr0.mysticalagriculture.api.MysticalAgriculturePlugin;
import com.blakebr0.mysticalagriculture.api.lib.PluginConfig;
import com.blakebr0.mysticalagriculture.api.registry.ICropRegistry;
import com.sxcccccccc.maaddon.init.ModCrops;

import static com.sxcccccccc.maaddon.MaAddon.MOD_ID;

@MysticalAgriculturePlugin
public final class ModCorePlugin implements IMysticalAgriculturePlugin {
	@Override
	public void configure(PluginConfig config) {
		config.setModId(MOD_ID);
		config.disableDynamicSeedCraftingRecipes();
		config.disableDynamicSeedInfusionRecipes();
		config.disableDynamicSeedReprocessingRecipes();
	}

	@Override
	public void onRegisterCrops(ICropRegistry registry) {
		ModCrops.onRegisterCrops(registry);
	}
}
