package com.sxcccccccc.constructionstickfix;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ConstructionStickFix.MODID)
public class ConstructionStickFix {
    public static final String MODID = "constructionstickfix";
    public static final Logger LOGGER = LogManager.getLogger("ConstructionStickFix");

    public ConstructionStickFix() {
        LOGGER.info("Construction Stick Fix loaded. Patching StickUpgradesSelectable.populateList() null-safety...");
    }
}
