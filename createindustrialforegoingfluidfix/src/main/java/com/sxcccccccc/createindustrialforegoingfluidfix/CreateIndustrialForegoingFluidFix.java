package com.sxcccccccc.createindustrialforegoingfluidfix;

import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(CreateIndustrialForegoingFluidFix.MODID)
public class CreateIndustrialForegoingFluidFix {
    public static final String MODID = "createindustrialforegoingfluidfix";
    public static final Logger LOGGER = LogManager.getLogger("CreateIndustrialForegoingFluidFix");

    public CreateIndustrialForegoingFluidFix() {
        LOGGER.info("Create x Industrial Foregoing Fluid Fix loaded. Patching FluidHelper.convertToStill/convertToFlowing for IF fluids...");
    }
}
