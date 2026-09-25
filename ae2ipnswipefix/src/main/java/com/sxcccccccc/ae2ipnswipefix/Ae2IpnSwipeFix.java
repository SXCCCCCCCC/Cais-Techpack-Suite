package com.sxcccccccc.ae2ipnswipefix;

import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Ae2IpnSwipeFix.MOD_ID)
public class Ae2IpnSwipeFix {
    public static final String MOD_ID = "ae2ipnswipefix";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Ae2IpnSwipeFix() {
        LOGGER.info("AE2 IPN Swipe Fix loaded");
    }
}
