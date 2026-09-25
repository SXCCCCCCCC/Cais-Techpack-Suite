package com.sxcccccccc.cofhlightfix;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CofhLightFix.MODID)
public class CofhLightFix {
    public static final String MODID = "cofhlightfix";
    public static final Logger LOGGER = LogManager.getLogger("CofhLightFix");

    public CofhLightFix() {
        LOGGER.info("CoFH Transient Light Fix loaded. Wrapping TransientLightManager getStoredLevel calls with null-safety...");
    }
}
