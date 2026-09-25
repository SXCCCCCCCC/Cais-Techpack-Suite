package com.sxcccccccc.kjsexportcmd;

import com.mojang.logging.LogUtils;
import dev.latvian.mods.kubejs.server.DataExport;
import dev.latvian.mods.kubejs.server.KubeJSReloadListener;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * /kubejs export 静默失败诊断 mod v3（1.20.1-Tec 包自用，无 mixin）。
 *
 * <p>前两版诊断（mixin @Overwrite exportData）在 Connector 环境下确认未被应用，
 * 且原版 {@code exportData()} 的 {@code catch (Exception)} 吞掉了 Exception、
 * 放跑了 Error，无 join 的 CompletableFuture 让异常彻底静默。
 *
 * <p>本版绕开整条 /kubejs export → reload → kjs$afterResourcesLoaded 链，
 * 注册 /kjsexport 命令直接以反射调用 private {@code exportData0()}，
 * 在命令线程捕获 {@link Throwable} 全类，用 slf4j 打完整栈进 latest.log。
 */
@Mod("kjsexportcmd")
public class KjsExportCmd {

    public static final Logger LOGGER = LogUtils.getLogger();

    public KjsExportCmd() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("kjsexport")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> runExport(ctx.getSource()))
        );
    }

    /**
     * GT 全局 5 倍速（替代 gt_speed_multiplier.js 的 ServerEvents.afterRecipes）。
     *
     * <p>根因（2026-08-21 全链路定位）：KubeJS 2001.6.5-build.24/26 的
     * AfterRecipesLoadedEventJS.afterPosted 在 RecipeManager.apply 完成、
     * recipes map 冻结为 ImmutableMap 之后执行 removeIf(Map::isEmpty)，
     * ImmutableCollection.removeIf 无条件抛 UnsupportedOperationException，
     * 调用链无异常保护 → kjs$afterResourcesLoaded 链静默断掉
     * （/kubejs export 失败 + "Server resource reload complete!" 消失）。
     * 任何 afterRecipes 监听器（哪怕空转）都会触发。
     *
     * <p>本修复在 TagsUpdatedEvent 的服务端时机加速：日志实证此刻
     * RecipeManager.recipes 仍是可变的 HashMap（Render thread 的客户端
     * 同步事件到来时已冻结为 ImmutableMap——"map 已冻结则跳过"同时
     * 天然过滤了客户端事件）。
     */
    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        try {
            ReloadableServerResources rsrc = KubeJSReloadListener.resources;
            if (rsrc == null) {
                return;
            }
            RecipeManager rm = rsrc.getRecipeManager();
            Field f = RecipeManager.class.getDeclaredField("f_44007_");
            f.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) f.get(rm);
            String mapType = map.getClass().getName();
            if (!mapType.contains("HashMap")) {
                LOGGER.info("[kjsexportcmd][gt_speed] recipes map 已冻结（{}），跳过加速（客户端同步事件）", mapType);
                return;
            }
            Field durationField = null;
            int changed = 0;
            int clamped = 0;
            for (Object v : map.values()) {
                if (!(v instanceof Map<?, ?> inner)) {
                    continue;
                }
                for (Object r : inner.values()) {
                    String cls = r.getClass().getName();
                    if (!cls.startsWith("com.gregtechceu.gtceu.api.recipe.GTRecipe")) {
                        continue;
                    }
                    try {
                        if (durationField == null) {
                            durationField = r.getClass().getField("duration"); // public int，GTRecipe 基类声明
                        }
                        int old = durationField.getInt(r);
                        if (old <= 0) {
                            continue; // 持续型/per-tick 类不动
                        }
                        int next = Math.max(1, old / 5);
                        if (next == old) {
                            continue;
                        }
                        durationField.setInt(r, next);
                        changed++;
                        if (old < 5) {
                            clamped++;
                        }
                    } catch (Exception ex) {
                        LOGGER.warn("[kjsexportcmd][gt_speed] 配方 {} duration 处理失败: {}", r.getClass().getName(), ex.toString());
                    }
                }
            }
            if (changed > 0) {
                LOGGER.info("[kjsexportcmd][gt_speed] {} 条 GT 配方加速至 1/5（其中 {} 条 clamp 到 1t）", changed, clamped);
            }
        } catch (Throwable t) {
            LOGGER.error("[kjsexportcmd][gt_speed] 加速过程异常", t);
        }
    }

    private static int runExport(CommandSourceStack source) {
        DataExport export = new DataExport();
        export.source = source;
        DataExport.export = export;
        LOGGER.info("[kjsexportcmd] 直接调用 DataExport.exportData0()（绕过 /kubejs export 的 reload 链）...");
        try {
            Method m = DataExport.class.getDeclaredMethod("exportData0");
            m.setAccessible(true);
            m.invoke(export);
            LOGGER.info("[kjsexportcmd] exportData0() 正常返回，未抛异常。");
            source.sendSuccess(() -> Component.literal("[kjsexportcmd] 导出完成，未抛异常（见 local/kubejs/export/）"), false);
            return 1;
        } catch (InvocationTargetException e) {
            Throwable t = e.getCause();
            LOGGER.error("[kjsexportcmd] exportData0() 抛出 {}: {}", t.getClass().getName(), t.getMessage(), t);
            source.sendFailure(Component.literal("[kjsexportcmd] 导出失败: " + t.getClass().getSimpleName() + ": " + t.getMessage() + "（完整栈见 latest.log）"));
            return 0;
        } catch (Throwable t) {
            LOGGER.error("[kjsexportcmd] 意外异常", t);
            source.sendFailure(Component.literal("[kjsexportcmd] 意外异常: " + t));
            return 0;
        } finally {
            DataExport.export = null;
        }
    }
}
