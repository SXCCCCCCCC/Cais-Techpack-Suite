package com.sxcccccccc.mekmmuufix.mixin;

import com.sxcccccccc.mekmmuufix.compat.UuCompat;
import com.jerry.mekmm.client.jei.MoreMachineRecipeRegistryHelper;
import com.jerry.mekmm.common.tile.machine.TileEntityFluidReplicator;
import com.jerry.mekmm.common.tile.machine.TileEntityReplicator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * JEI 注册入口：mekmm 的 JEI 助手直接遍历 {@code customRecipeMap} 且不判空——
 * config 空列表时表为 null，JEI 注册必 NPE。HEAD 注入先触发填充（顺带让 JEI
 * 展示 IC2 成本体系下的全部复制配方），仍为 null（IC2 未装 / 全包无成本）时
 * cancel 跳过注册，避免崩溃。
 *
 * <p>另：IC2 索引在 SERVER_STARTED 异步构建，JEI 注册与索引就绪（indexReady
 * 翻转）之间存在竞态——注册循环里的 containsKey 读旧表（ready=false 白名单成本，
 * 229 条），随后的 getRecipe 触发表重填并读到新表（ready=true 求解成本，2473 条）；
 * 白名单静态成本 ≤90B 但求解成本 >90B（或被截断）的条目只存在于旧表，getRecipe
 * 对新表查得 0 → 返回 null → JEI 的 addRecipes 抛 "recipes must not contain
 * null values" NPE，且连锁中止 registerFluidReplicator（流体表停在 0 条）。
 * 故对两个注册方法里的 {@code list.add(...)} 做 @Redirect 滤空，保证 null
 * 永远进不了 JEI 配方表（机器侧 getRecipe 语义不变：>90B 条目照常被拒）。
 *
 * <p>注意：本 mixin 仅客户端（类在 mekmm 的 client 包，引 mezz.jei），
 * 配置写在 mixins.json 的 "client" 段。</p>
 */
@Mixin(value = MoreMachineRecipeRegistryHelper.class, remap = false)
public abstract class MoreMachineRecipeRegistryHelperMixin {

    @Inject(method = "registerItemReplicator", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mekmmuufix$ensureItemMapJei(CallbackInfo ci) {
        UuCompat.ensureItemMaps();
        if (TileEntityReplicator.customRecipeMap == null) {
            ci.cancel();
        }
    }

    @Inject(method = "registerFluidReplicator", at = @At("HEAD"), cancellable = true, remap = false)
    private static void mekmmuufix$ensureFluidMapJei(CallbackInfo ci) {
        UuCompat.ensureFluidMap();
        if (TileEntityFluidReplicator.customRecipeMap == null) {
            ci.cancel();
        }
    }

    @Redirect(method = "registerItemReplicator", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
    private static boolean mekmmuufix$addItemRecipeNotNull(List<Object> list, Object recipe) {
        if (recipe == null) {
            return false;
        }
        return list.add(recipe);
    }

    @Redirect(method = "registerFluidReplicator", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"), remap = false)
    private static boolean mekmmuufix$addFluidRecipeNotNull(List<Object> list, Object recipe) {
        if (recipe == null) {
            return false;
        }
        return list.add(recipe);
    }
}
