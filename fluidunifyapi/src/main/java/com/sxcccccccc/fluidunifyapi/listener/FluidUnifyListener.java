package com.sxcccccccc.fluidunifyapi.listener;

import com.sxcccccccc.fluidunifyapi.FluidUnifyApi;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch;
import com.sxcccccccc.fluidunifyapi.core.UnifiedFluidRegistry;
import com.sxcccccccc.fluidunifyapi.ifmod.IfDatapackFamilyApplier;
import com.sxcccccccc.fluidunifyapi.iu.IuRecipeFamilyApplier;
import com.sxcccccccc.fluidunifyapi.kubejs.FluidUnifyEventJS;
import com.sxcccccccc.fluidunifyapi.kubejs.FluidUnifyEvents;
import dev.latvian.mods.kubejs.script.ScriptType;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 补丁应用的总编排点：Forge TagsUpdatedEvent（priority = LOWEST）。
 *
 * <p>时机论证（2026-09-01 三 mod 源码实证）：</p>
 * <ol>
 *   <li>KubeJS tag 装载（TagLoaderKJS.kjs$customTags）先完成 → tag 已就绪；</li>
 *   <li>IU 原生配方在 IUCore.getore（默认优先级 TagsUpdatedEvent 监听器）注册完
 *       → 模板配方可枚举；</li>
 *   <li>LOWEST 优先级垫底执行 → post unify 事件 → 脚本收集补丁 → applyAll；</li>
 *   <li>单人档中玩家入世界晚于本点 → 之后放置的机器 BE 构造时谓词/槽位快照
 *       已含新流体（只处理新机器的语义自然成立）；</li>
 *   <li>客户端 JEI 在更晚的客户端 RecipesUpdatedEvent 注册配方 → 读到的就是
 *       改后的状态（同 JVM 静态共享 / IF datapack 族经 injectRuntimeRecipes 双写）。</li>
 * </ol>
 *
 * <p>单人档里客户端也会触发一次 TagsUpdatedEvent（同 JVM）→ 本监听器再跑一遍 →
 * 事件再 post 一次（同 JVM 的 SERVER 脚本处理器再执行一遍）→ applyAll 幂等、
 * 配方应用器带查重，双跑无副作用。</p>
 */
@Mod.EventBusSubscriber(modid = "fluidunifyapi")
public final class FluidUnifyListener {

    private FluidUnifyListener() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        FluidUnifyEventJS eventJS = new FluidUnifyEventJS();
        try {
            FluidUnifyEvents.UNIFY.post(ScriptType.SERVER, eventJS);
        } catch (Throwable t) {
            // 脚本错误不吞：直接抛出（fail-fast，配置错误当场崩）
            FluidUnifyApi.LOGGER.error("[fluidunifyapi] unify event script failed", t);
            throw t;
        }
        List<FluidPatch> patches = eventJS.getPatches();
        if (patches.isEmpty()) {
            return;
        }
        FluidUnifyApi.LOGGER.info("[fluidunifyapi] applying {} patches", patches.size());
        UnifiedFluidRegistry.applyAll(patches);
        // 机器族应用：IU 配方族（运行时配方表）+ IF datapack 族（服务端 RecipeManager 直写）
        for (FluidPatch patch : patches) {
            IuRecipeFamilyApplier.applyIfRecipeMachine(patch);
        }
        IfDatapackFamilyApplier.applyServerSide(patches);
    }
}
