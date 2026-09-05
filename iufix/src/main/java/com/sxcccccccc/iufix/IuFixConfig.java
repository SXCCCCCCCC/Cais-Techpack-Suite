package com.sxcccccccc.iufix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 1.7.0：iufix.json 配置（config 目录，启动时读取；文件不存在则生成默认文件）。
 *
 * <p>当前唯一键 {@code cooling}：布尔，默认 {@code true} = 原版行为（机器正常积热，
 * 温度 100 会停机/挂死）；玩家设 {@code false} = 关闭散热需求——被冷却端机器
 * （组合机家族 + 流体冰箱）温度恒 0，永不因散热停机。
 *
 * <p>接线方式：把配置值写入 IU 内置的 {@code CoolComponent.cooling} 静态总开关
 * （作者留了「未接线的开关」——addEnergy 内 {@code if (delegate instanceof ICoolSink)
 * if (!cooling) storage = 0}，但没有任何代码置它，readFromNbt 重载 100 也不经过它）。
 * 本类和 {@code IUCoolComponentMixin} 一起把开关接满：静态值覆盖 addEnergy 升温路径，
 * mixin 两处注入补 readFromNbt 重载路径（详见 mixin javadoc）。
 *
 * <p>读取时机 = mod 构造器（IuFix#构造），早于任何 BlockEntity 工作。
 */
public final class IuFixConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 运行时配置值：true = 原版（默认），false = 关闭散热需求。 */
    public static boolean cooling = true;

    private IuFixConfig() {
    }

    public static void load() {
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve("iufix.json");
            boolean value = true;
            if (Files.exists(file)) {
                try {
                    Cfg cfg = GSON.fromJson(Files.newBufferedReader(file, StandardCharsets.UTF_8), Cfg.class);
                    // null（缺失键/未知格式）一律回退默认 true = 原版行为，无需升级既有文件
                    if (cfg != null && cfg.cooling != null) {
                        value = cfg.cooling;
                    }
                } catch (Exception e) {
                    LOGGER.error("[iufix] iufix.json 解析失败，回退默认 cooling=true（原版行为）。文件: {}", file, e);
                    value = true;
                }
            } else {
                // 首次运行：生成默认文件（不改行为）
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(new Cfg()), StandardCharsets.UTF_8);
            }
            apply(value);
            LOGGER.info("[iufix] iufix.json applied: cooling={} -> CoolComponent.cooling={} (文件: {})",
                    value, com.denfop.componets.CoolComponent.cooling, file);
        } catch (Exception e) {
            LOGGER.error("[iufix] iufix.json 配置初始化失败，按默认 cooling=true（原版行为）运行", e);
            apply(true);
        }
    }

    private static void apply(boolean value) {
        cooling = value;
        // IU 内置总开关（编译期 compileOnly 引用；static init 置 true 后此处覆盖）
        com.denfop.componets.CoolComponent.cooling = value;
    }

    private static class Cfg {
        // Gson 不走构造器，用包装类型 + null 判定区分「缺失键」与显式 false
        Boolean cooling = true;
    }
}
