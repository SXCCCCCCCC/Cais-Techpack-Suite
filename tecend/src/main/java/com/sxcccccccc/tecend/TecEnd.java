package com.sxcccccccc.tecend;

import com.mojang.logging.LogUtils;
import com.sxcccccccc.tecend.compat.Compat;
import com.sxcccccccc.tecend.compat.CreateTrophy;
import com.sxcccccccc.tecend.compat.IuRecipes;
import com.sxcccccccc.tecend.compat.TecendMaterials;
import com.sxcccccccc.tecend.config.TecendConfig;
import com.sxcccccccc.tecend.registry.ModBlockEntities;
import com.sxcccccccc.tecend.registry.ModBlocks;
import com.sxcccccccc.tecend.registry.ModChemicals;
import com.sxcccccccc.tecend.registry.ModFluids;
import com.sxcccccccc.tecend.registry.ModItems;
import com.sxcccccccc.tecend.registry.TrophyItemOverride;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
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

        // 充能数值（第 15 步），设计稿的数字都在这里暴露出去
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, TecendConfig.SPEC);

        ModItems.register(modBus);
        ModBlocks.register(modBus);
        ModBlockEntities.register(modBus);
        ModFluids.register(modBus);

        modBus.addListener(this::commonSetup);
        modBus.addListener(this::loadComplete);

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
        // 奖杯物品是抢注来的（注册表覆盖），这里确认一下覆盖真的生效了
        event.enqueueWork(TrophyItemOverride::verify);
        // Create 的应力账本里给奖杯方块登记一笔（SU/RPM）。引用 Create 类型的调用放在 lambda 里，
        // 缺 Create 时不会解析那个类。
        if (Compat.isLoaded(Compat.CREATE)) {
            event.enqueueWork(() -> CreateTrophy.registerStress());
        }
    }

    /**
     * 所有 mod 初始化完毕后的最后一站 —— 往上游的运行时配方表里补条目。
     *
     * <p>这些上游（IU 等）把配方写死在代码里、数据包塞不进去，但开放了运行时配方 API；
     * 注入必须等它们自己灌完内建配方并重建索引，所以挂在这里而不是 commonSetup。
     * 每个入口自己带门禁（{@code Compat.isLoaded}），缺上游时静默跳过。</p>
     */
    private void loadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            if (Compat.isLoaded(Compat.IU)) {
                IuRecipes.register();
            }
        });
    }
}
