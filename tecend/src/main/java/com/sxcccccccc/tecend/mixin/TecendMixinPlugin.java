package com.sxcccccccc.tecend.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Mixin 配置插件：有几个 mixin 实现的是别家 mod 的接口（Create 的 {@code IRotate}、
 * IC2 的 {@code IBatteryItem}、AE2 的 {@code IAEItemPowerStorage}、IU 的 {@code EnergyItem}、
 * PnC 的 {@code IPressurizableItem}），那家不在场时这些类连解析都做不到 —— 用插件把它们整个滤掉。
 *
 * <p>本 mod 的 mixin 配置是 {@code required = true}（挂了要炸），所以不能靠"让它失败"来跳过。
 * 判在 {@code LoadingModList} 上是因为插件跑得比 ModList 就绪还早，那时只能问加载列表。</p>
 */
public class TecendMixinPlugin implements IMixinConfigPlugin {

    /** mixin 类名 → 它依赖的 mod id（模组 id 写字面量：这个阶段还不能碰 Compat 的 ModList） */
    private static final Map<String, String> GATED = Map.of(
            "com.sxcccccccc.tecend.mixin.TrophyRotateMixin", "create",
            "com.sxcccccccc.tecend.mixin.TrophyIc2Mixin", "ic2_120",
            "com.sxcccccccc.tecend.mixin.TrophyAeMixin", "ae2",
            "com.sxcccccccc.tecend.mixin.TrophyIuMixin", "industrialupgrade",
            "com.sxcccccccc.tecend.mixin.TrophyPncMixin", "pneumaticcraft",
            "com.sxcccccccc.tecend.mixin.QuantumQuarryFragmentMixin", "industrialupgrade");

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String modId = GATED.get(mixinClassName);
        if (modId == null) {
            return true;
        }
        LoadingModList mods = LoadingModList.get();
        return mods != null && mods.getModFileById(modId) != null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
