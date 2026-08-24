package com.sxcccccccc.iudebug.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * v1.1.1：banned_registrations 磁盘文件直读（与 Forge 配置生命周期彻底解耦）。
 *
 * <p><b>为什么需要</b>（2026-08-24 实测根因，latest.log 时间线实证）：本包实际
 * 启动序列里 RegisterEvent 派发在 [22:49:30]（iudebug 的 found/removed 日志），
 * 而 Forge 的 CONFIG_LOAD（ModConfigEvent.Loading，ConfigSpec 才拿到文件值，
 * iudebug 的 "banned_registrations 已加载" 日志）在 [22:49:37]——注册事件
 * <b>先于</b>配置进内存。因此删除逻辑绝不能实时读 ForgeConfigSpec（事件时刻拿到
 * 的是默认空列表 → 0 匹配），必须在首个目标 RegisterEvent 时直接读磁盘 TOML。
 *
 * <p><b>实现</b>：nightconfig（Forge 47.4.10 运行时自带依赖，pom 声明 core/toml
 * 3.6.4，libraries 里还有 3.6.7/3.8.3 等版本）的 {@link FileConfig} + {@link TomlFormat}
 * 解析 {@code <gameDir>/config/iudebug-common.toml} 顶层 {@code banned_registrations}
 * 数组（TOML 列表）。注意 3.6.4 的 {@code FileConfigBuilder} 没有 format() 方法，
 * 格式必须经 {@code FileConfig.of(path, ConfigFormat)} 传入——该 API 在
 * 3.6.4/3.6.7/3.8.3 全部存在，编译与运行时链接都稳定。首次调用（= 首个目标
 * RegisterEvent）读盘并缓存进内存；文件缺失 = 首次启动尚未生成 → WARN 并视为
 * 空列表；解析/IO 失败 = ERROR + loadFailed 标记，删除逻辑见标记中止（不静默）。
 * 其余配置项（recipecheck 等）继续走 Forge 配置生命周期，不受本类影响。
 */
public final class BannedFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger("iudebug");

    private static final String CONFIG_FILE = "iudebug-common.toml";

    /** null = 尚未读取（首个目标 RegisterEvent 才读）。 */
    private static Set<String> cached;
    private static boolean loadFailed;

    private BannedFileReader() {
    }

    /**
     * 返回 banned_registrations 集合（磁盘文件直读，首次调用读盘并缓存）。
     * 读失败或文件缺失返回空集合；是否失败用 {@link #loadFailed()} 判定。
     */
    public static synchronized Set<String> get() {
        if (cached == null) {
            cached = loadFromDisk();
        }
        return cached;
    }

    /** 磁盘读取是否已失败（失败时删除逻辑应中止，不能静默）。 */
    public static synchronized boolean loadFailed() {
        return loadFailed;
    }

    private static Set<String> loadFromDisk() {
        Path file = FMLPaths.CONFIGDIR.get().resolve(CONFIG_FILE);
        if (!Files.isRegularFile(file)) {
            LOGGER.warn("[iudebug] banned_registrations file not found: {} (first launch?) - treating as empty", file);
            return Collections.emptySet();
        }
        try (FileConfig config = FileConfig.of(file, TomlFormat.instance())) {
            config.load();
            Object raw = config.get("banned_registrations");
            if (raw == null) {
                LOGGER.warn("[iudebug] banned_registrations key missing in {} - treating as empty", file);
                return Collections.emptySet();
            }
            if (!(raw instanceof List<?> list)) {
                loadFailed = true;
                LOGGER.error("[iudebug] banned_registrations is not a TOML list in {} - aborting deletion", file);
                return Collections.emptySet();
            }
            Set<String> banned = list.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            LOGGER.info("[iudebug] banned_registrations loaded from file ({} entries, source=file-direct): {}", banned.size(), banned);
            return banned;
        } catch (Exception e) {
            loadFailed = true;
            LOGGER.error("[iudebug] failed to read config file {} - aborting deletion", file, e);
            return Collections.emptySet();
        }
    }
}
