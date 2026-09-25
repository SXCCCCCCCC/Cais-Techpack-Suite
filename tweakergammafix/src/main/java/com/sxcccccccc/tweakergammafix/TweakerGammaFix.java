package com.sxcccccccc.tweakergammafix;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TweakerGammaFix.MOD_ID)
public class TweakerGammaFix {
    public static final String MOD_ID = "tweakergammafix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public TweakerGammaFix() {
        LOGGER.info("Tweakerge Gamma Fix loaded");
    }
}
