package com.sxcccccccc.mafgllibfix;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MafgLibFix.MOD_ID)
public class MafgLibFix {
    public static final String MOD_ID = "mafgllibfix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public MafgLibFix() {
        LOGGER.info("MaFgLib Fix loaded");
    }
}
