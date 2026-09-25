package com.sxcccccccc.tecend.registry;

import com.sxcccccccc.tecend.TecEnd;
import mekanism.api.MekanismAPI;
import mekanism.api.chemical.slurry.Slurry;
import mekanism.api.chemical.slurry.SlurryBuilder;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * Mek 化学品注册：第 ③ 步的两道浆液。
 *
 * <ul>
 *   <li>{@code tecend:dirty_fragment_slurry} 污浊的残片浆液 —— 基色偏黑 {@code #25221C}</li>
 *   <li>{@code tecend:clean_fragment_slurry} 纯净的残片浆液 —— 基色偏白 {@code #656157}</li>
 * </ul>
 *
 * <p>Mek 的 {@link SlurryBuilder} 自带 {@code dirty()} / {@code clean()} 两套默认材质
 * （Mek 自己的贴图，引用不复制），{@code tint(int)} 就是染色 —— 所以这两件不用画图。</p>
 *
 * <p>注册表 key 是 {@code MekanismAPI.SLURRY_REGISTRY_NAME}（{@code mekanism:slurry}）；
 * Mek 自带的 {@code SlurryDeferredRegister} 是"一次注册 dirty_X + clean_X 一对、共用同一个 build 参数"，
 * 我们两件颜色不同，所以直接用 Forge 的 DeferredRegister 分别注册。
 * 本类引用 Mek 类型（字段类型），所以只能过 Compat 门禁后加载。</p>
 */
public final class ModChemicals {
    private ModChemicals() {}

    public static final DeferredRegister<Slurry> SLURRIES =
            DeferredRegister.create(MekanismAPI.SLURRY_REGISTRY_NAME, TecEnd.MOD_ID);

    /** 污浊的残片浆液 */
    public static final RegistryObject<Slurry> DIRTY_FRAGMENT_SLURRY =
            SLURRIES.register("dirty_fragment_slurry",
                    () -> new Slurry(SlurryBuilder.dirty().tint(0x25221C)));

    /** 纯净的残片浆液 */
    public static final RegistryObject<Slurry> CLEAN_FRAGMENT_SLURRY =
            SLURRIES.register("clean_fragment_slurry",
                    () -> new Slurry(SlurryBuilder.clean().tint(0x656157)));

    public static void register(IEventBus modBus) {
        SLURRIES.register(modBus);
    }
}
