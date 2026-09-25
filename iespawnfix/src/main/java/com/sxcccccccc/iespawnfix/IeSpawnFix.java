package com.sxcccccccc.iespawnfix;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(IeSpawnFix.MOD_ID)
public class IeSpawnFix {
    public static final String MOD_ID = "iespawnfix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public IeSpawnFix() {
        LOGGER.info("IE Spawn Interdiction Fix loaded");
    }
}
