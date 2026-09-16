package com.sxcccccccc.tecend;

import com.mojang.logging.LogUtils;
import com.sxcccccccc.tecend.compat.Compat;
import com.sxcccccccc.tecend.compat.TecendMaterials;
import com.sxcccccccc.tecend.registry.ModBlockEntities;
import com.sxcccccccc.tecend.registry.ModBlocks;
import com.sxcccccccc.tecend.registry.ModChemicals;
import com.sxcccccccc.tecend.registry.ModFluids;
import com.sxcccccccc.tecend.registry.ModItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Thank You for Playing Cai's Modpack —— 1.20.1-Tec 整合包的终局 mod。
 * 流程设计见 docs/设计流程.md，内容从这里往下加。
 */
@Mod(TecEnd.MOD_ID)
public class TecEnd {
    public static final String MOD_ID = "tecend";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TecEnd() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModFluids.register(modBus);

        modBus.addListener(this::commonSetup);

        // GTCEu 的材质注册事件（mod bus）。手动注册：缺 GTCEu 时不能加载那个类（它的方法签名引用了 GT 类型）。
        if (Compat.isLoaded(Compat.GTCEU)) {
            modBus.register(TecendMaterials.class);
        }

        // Mek 化学品（浆液）。同样要过门禁：ModChemicals 的字段类型就是 Mek 的类。
        if (Compat.isLoaded(Compat.MEK)) {
            ModChemicals.register(modBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Compat::report);
    }
}
