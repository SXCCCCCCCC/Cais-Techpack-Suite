package com.sxcccccccc.iffermentfix;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(IfFermentFix.MODID)
public class IfFermentFix {
    public static final String MODID = "iffermentfix";
    public static final Logger LOGGER = LogManager.getLogger("IfFermentFix");

    public IfFermentFix() {
        LOGGER.info("IF Fermentation Fix loaded. Fermentation Station now halts when its output tank holds fluid with mismatched NBT instead of voiding the product.");
    }
}
