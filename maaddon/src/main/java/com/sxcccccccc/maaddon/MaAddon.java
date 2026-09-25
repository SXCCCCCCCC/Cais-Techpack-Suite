package com.sxcccccccc.maaddon;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MaAddon.MOD_ID)
public final class MaAddon {
	public static final String MOD_ID = "maaddon";
	public static final String NAME = "Mystical Agriculture Addon";
	public static final Logger LOGGER = LoggerFactory.getLogger(NAME);

	public MaAddon() {
		LOGGER.info("{} loaded successfully", NAME);
	}
}
