package com.sxcccccccc.tecend.compat;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.sxcccccccc.tecend.TecEnd;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 注册「残片」GT 材质 —— 让 GT 按颜色生成 原矿/粉碎/洗净/精炼/粉末/脏粉/净粉 那套贴图。
 *
 * <p>为什么必须用 Java 而不是 KubeJS：GTCEu 的 KubeJS 集成只有 {@code materialModification}
 * （改现有材质），**没有新建材质的入口**（GTCEuStartupEvents 里逐个 event 看过）。</p>
 *
 * <p>颜色空间：#3D392F —— 从远古奖杯贴图 {@code trophy_ancient.png} 的左上 1/4（32×32 区域）
 * 采样的最常见色（占 40%）；同区域平均值是 #3F3D36。</p>
 *
 * <p>部件由"属性"决定（TagPrefix 的 generationCondition）：{@code ore()} 给
 * rawOre / crushed / crushedPurified / crushedRefined / dustImpure / dustPure，
 * {@code dust()} 给 dust / tinyDust。想加/减部件就改这两行。</p>
 *
 * <p>本类**不加** {@code @Mod.EventBusSubscriber}：Forge 会立刻反射它的方法签名，
 * 缺 GTCEu 时会 NoClassDefFoundError。改为在 {@code TecEnd} 里过 Compat 门禁后手动
 * {@code modBus.register(...)}。</p>
 */
public final class TecendMaterials {
    private TecendMaterials() {}

    /** 从远古奖杯贴图左上 1/4 采样得到的颜色（最常见色） */
    public static final int TROPHY_FRAGMENT_COLOR = 0x3D392F;

    /** 注册后的材质；缺 GTCEu 时为 null */
    public static Material TROPHY_FRAGMENT;

    @SubscribeEvent
    public static void onMaterialEvent(MaterialEvent event) {
        TROPHY_FRAGMENT = new Material.Builder(new ResourceLocation(TecEnd.MOD_ID, "trophy_fragment"))
                .color(TROPHY_FRAGMENT_COLOR)
                .iconSet(MaterialIconSet.DULL)
                .ore()      // raw_%s / crushed_%s_ore / purified_%s_ore / refined_%s_ore
                .dust()     // %s_dust / small_%s_dust / tiny_%s_dust / impure_%s_dust / pure_%s_dust
                .gem()      // %s_gem / chipped / flawed / flawless / exquisite
                // 不给 JEI 出「矿物处理流程图」：GT 的图条目筛选条件是
                // hasProperty(ORE) && !hasFlag(NO_ORE_PROCESSING_TAB)（GTOreProcessingJeiCategory 源码），
                // 打上这个标记就把本材质排除掉，其它材质的图不受影响。
                .flags(MaterialFlags.NO_ORE_PROCESSING_TAB)
                .buildAndRegister();
    }
}
