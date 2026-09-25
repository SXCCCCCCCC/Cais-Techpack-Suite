package com.sxcccccccc.creativetabfix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.0.0：creativetabfix.json 配置（config 目录，启动时读取；文件不存在则生成默认文件）。
 *
 * <p>唯一键 {@code mappings}：字符串数组，每项格式 {@code <物品命名空间>=<目标创造标签页ID>}。
 * 默认 {@code ["gtca=gtca:main"]}。目标标签页必须真实存在（由某个模组注册）；
 * 不存在时 {@link CreativeTabFixEvents#onLoadComplete} 会报 ERROR，映射本身静默跳过。
 *
 * <p>读取时机 = mod 构造器，早于注册表事件，因此创造标签页开始构建时配置必定已就绪 ——
 * 这也是不用 {@code ModConfigSpec} 的原因之一（Common 配置的加载时机与标签页构建时机的
 * 先后没有硬保证）。
 */
public final class CreativeTabFixConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger("creativetabfix");

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final List<String> DEFAULT_MAPPINGS = List.of("gtca=gtca:main");

    /** 运行时配置值（原始字符串），构造器里 load() 后即固定。 */
    public static List<String> mappings = DEFAULT_MAPPINGS;

    /** 已解析的映射，load() 后即固定。两个事件处理器共用，避免每条映射重复解析 ~200 次。 */
    public static List<Mapping> parsed = List.of();

    private CreativeTabFixConfig() {
    }

    /** 一条 {@code <命名空间>=<标签页ID>} 映射。 */
    public record Mapping(String namespace, ResourceLocation tab) {
    }

    public static void load() {
        try {
            Path file = FMLPaths.CONFIGDIR.get().resolve("creativetabfix.json");
            if (Files.exists(file)) {
                try {
                    Cfg cfg = GSON.fromJson(Files.newBufferedReader(file, StandardCharsets.UTF_8), Cfg.class);
                    // null（缺失键/未知格式）一律回退默认值，无需升级既有文件
                    if (cfg != null && cfg.mappings != null && !cfg.mappings.isEmpty()) {
                        mappings = List.copyOf(cfg.mappings);
                    }
                } catch (Exception e) {
                    LOGGER.error("[creativetabfix] creativetabfix.json 解析失败，回退默认 mappings={}。文件: {}",
                            DEFAULT_MAPPINGS, file, e);
                    mappings = DEFAULT_MAPPINGS;
                }
            } else {
                // 首次运行：生成默认文件（不改行为）
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(new Cfg()), StandardCharsets.UTF_8);
            }
            LOGGER.info("[creativetabfix] creativetabfix.json applied: mappings={} (文件: {})", mappings, file);
        } catch (Exception e) {
            LOGGER.error("[creativetabfix] creativetabfix.json 初始化失败，按默认 mappings={} 运行", DEFAULT_MAPPINGS, e);
            mappings = DEFAULT_MAPPINGS;
        }
        parse();
    }

    private static void parse() {
        List<Mapping> out = new ArrayList<>();
        for (String mapping : mappings) {
            int sep = mapping.indexOf('=');
            if (sep <= 0 || sep == mapping.length() - 1) {
                LOGGER.warn("[creativetabfix] 忽略非法映射 '{}'，格式应为 <命名空间>=<标签页ID>", mapping);
                continue;
            }
            String namespace = mapping.substring(0, sep).trim();
            ResourceLocation tab = ResourceLocation.tryParse(mapping.substring(sep + 1).trim());
            if (namespace.isEmpty() || tab == null) {
                LOGGER.warn("[creativetabfix] 忽略非法映射 '{}'（命名空间为空或标签页 ID 不合法）", mapping);
                continue;
            }
            out.add(new Mapping(namespace, tab));
        }
        parsed = List.copyOf(out);
    }

    private static class Cfg {
        // Gson 不走构造器，用包装集合 + null 判定区分「缺失键」与显式空数组
        List<String> mappings = DEFAULT_MAPPINGS;
    }
}
