package com.sxcccccccc.iudebug;

import com.sxcccccccc.iudebug.config.BannedFileReader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 功能1 实施（v1.1.1）：纯事件 + 反射移除 IU 的 DeferredRegister 条目。
 * mixin 路线整体废弃（2026-08-24 用户裁决：侵入性强但测试环境无所谓）。
 *
 * <p><b>事件时序</b>（fmlcore/javafmllanguage/universal jar 字节码实证链）：
 * <ol>
 *   <li>mods.toml 声明 {@code [[dependencies.iudebug]] modId="industrialupgrade" ordering="BEFORE"}
 *       → FML 依赖拓扑排序把 iudebug 排在 industrialupgrade 之前加载；</li>
 *   <li>REGISTRY 阶段 {@code GameData.postRegisterEvents()} 对每个注册表依次构造
 *       RegisterEvent，{@code ModLoader.postEventWrapContainerInModOrder} 按 mod 顺序
 *       逐个 container 调用 {@code ModContainer.acceptEvent}（虚派发到
 *       {@code FMLModContainer.acceptEvent} → 该 mod 自己的 EventBus 派发）；</li>
 *   <li>本监听器挂在 iudebug 的 mod 总线（@Mod.EventBusSubscriber bus=MOD），
 *       对同一个 RegisterEvent 一定先于 industrialupgrade 总线上 IU 的
 *       DeferredRegister.addEntries 执行（IU 的 DR 也注册在它自己 mod 的 EventBus 上）。</li>
 * </ol>
 *
 * <p><b>配置来源（v1.1.1 根因修复，与 Forge 配置生命周期解耦）</b>：本包实际
 * 启动序列里 RegisterEvent 派发（实测 22:49:30）<b>先于</b> CONFIG_LOAD 配置进
 * 内存（实测 22:49:37）——实时读 ForgeConfigSpec 在事件时刻拿到的是默认空列表
 * （v1.1.0 0 匹配根因）。因此被禁列表改由 {@link BannedFileReader} 在首个目标
 * RegisterEvent 时<b>直接读磁盘 TOML</b>（nightconfig FileConfig/TomlFormat）
 * 并缓存；读失败 = ERROR + loadFailed 标记 → 中止删除（不静默）。其余配置项
 * （recipecheck 等）继续走 Forge 配置生命周期，不受影响。
 *
 * <p><b>反射目标</b>（IU 生产 jar 字节码实证，类名/字段名未混淆保留源名）：
 * <ul>
 *   <li>{@code com.denfop.register.Register}：21 个 public static DeferredRegister 字段
 *       （FLUIDS / FLUID_TYPES / ITEMS / BLOCKS / BLOCK_ENTITIES 等）；</li>
 *   <li>{@code DeferredRegister.entries} 私有字段，实证类型
 *       {@code LinkedHashMap<RegistryObject<T>, Supplier<? extends T>>}；public
 *       getEntries() 只返回不可变 key 集合（entriesView），删条目必须反射 entries；</li>
 *   <li>条目 key = RegistryObject，{@code key.getId()} = 完整注册 id（addEntries 里
 *       同样用 getId() 注册），与文件直读的被禁集合（namespace:path）精确匹配后
 *       iterator.remove() → IU 的 addEntries 遍历时条目已不在 → 真删除。</li>
 * </ul>
 *
 * <p>只处理 FLUIDS / ITEMS / BLOCKS / FLUID_TYPES 四个注册表的 RegisterEvent
 * （当前 60 条配置恰好覆盖这四类：*_flowing / bucket/* / fluid/* / *_types）。
 * 幂等：条目被移除后，后续事件再跑无操作。破坏性：被移除条目若在 IU 代码里
 * .get() 会抛 "Registry Object not present"（测试环境预期行为，见 usage.md）。
 */
@Mod.EventBusSubscriber(modid = "iudebug", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class IuRegistrationBlocker {

    private static final Logger LOGGER = LoggerFactory.getLogger("iudebug");

    /** 本次配置覆盖的四个注册表（RegisterEvent 的 registryKey.location()）。 */
    private static final Set<ResourceLocation> TARGET_REGISTRIES = Set.of(
            Registries.FLUID.location(),
            Registries.ITEM.location(),
            Registries.BLOCK.location(),
            ForgeRegistries.Keys.FLUID_TYPES.location()
    );

    /** 持有 IU 全部 DeferredRegister 静态字段的类（类名未混淆，逐一核对后按源名保留）。 */
    private static final List<String> IU_REGISTER_CLASSES = List.of(
            "com.denfop.register.Register",
            "com.denfop.register.FluidHandler"
    );

    /** DeferredRegister.entries 私有字段（实证类型 Map&lt;RegistryObject, Supplier&gt;）。 */
    private static final Field ENTRIES_FIELD;

    /** 首次相关事件时解析并缓存（REGISTRY 阶段 IU 已完全构造，静态字段必然已初始化）。 */
    private static List<DeferredRegister<?>> iuRegisters;

    static {
        try {
            ENTRIES_FIELD = DeferredRegister.class.getDeclaredField("entries");
            ENTRIES_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("iudebug: DeferredRegister.entries field missing", e);
        }
    }

    private IuRegistrationBlocker() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        ResourceLocation registry = event.getRegistryKey().location();
        if (!TARGET_REGISTRIES.contains(registry)) {
            return;
        }
        // 配置来源：磁盘文件直读（BannedFileReader），与 Forge 配置生命周期无关。
        if (BannedFileReader.loadFailed()) {
            LOGGER.error("[iudebug] banned list unavailable (file read failed) - aborting deletion for event {}", registry);
            return;
        }
        Set<String> banned = BannedFileReader.get();
        if (iuRegisters == null) {
            iuRegisters = findIuDeferredRegisters();
        }
        if (iuRegisters.isEmpty()) {
            LOGGER.warn("[iudebug] RegisterEvent {}: no IU DeferredRegister found (reflection target missing?)", registry);
            return;
        }
        for (DeferredRegister<?> dr : iuRegisters) {
            int removed;
            try {
                removed = stripBanned(dr, banned);
            } catch (ReflectiveOperationException | RuntimeException ex) {
                LOGGER.error("[iudebug] failed to strip banned entries from {} (registry {})",
                        dr.getRegistryName(), dr.getRegistryKey().location(), ex);
                continue;
            }
            if (removed > 0) {
                LOGGER.info("[iudebug] RegisterEvent {}: removed {} banned entries from {} (registry {})",
                        registry, removed, dr.getRegistryName(), dr.getRegistryKey().location());
            }
        }
    }

    private static List<DeferredRegister<?>> findIuDeferredRegisters() {
        List<DeferredRegister<?>> result = new ArrayList<>();
        for (String className : IU_REGISTER_CLASSES) {
            Class<?> clazz;
            try {
                // initialize=false：此时（REGISTRY 阶段）IU 必然已初始化完毕，
                // 不主动触发任何第三方类初始化。
                clazz = Class.forName(className, false, IuRegistrationBlocker.class.getClassLoader());
            } catch (ClassNotFoundException e) {
                LOGGER.error("[iudebug] IU register class not found: {}", className);
                continue;
            }
            for (Field field : clazz.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                if (!DeferredRegister.class.isAssignableFrom(field.getType())) {
                    continue;
                }
                try {
                    Object value = field.get(null);
                    if (value instanceof DeferredRegister<?> dr) {
                        result.add(dr);
                        LOGGER.info("[iudebug] found IU DeferredRegister {}.{} (registry {})",
                                className, field.getName(), dr.getRegistryKey().location());
                    }
                } catch (IllegalAccessException e) {
                    LOGGER.error("[iudebug] cannot read field {}.{}", className, field.getName(), e);
                }
            }
        }
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static int stripBanned(DeferredRegister<?> dr, Set<String> banned) throws ReflectiveOperationException {
        Map<RegistryObject<?>, Supplier<?>> entries =
                (Map<RegistryObject<?>, Supplier<?>>) ENTRIES_FIELD.get(dr);
        int removed = 0;
        Iterator<Map.Entry<RegistryObject<?>, Supplier<?>>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<RegistryObject<?>, Supplier<?>> entry = it.next();
            ResourceLocation id = entry.getKey().getId();
            if (!"industrialupgrade".equals(id.getNamespace())) {
                continue;
            }
            if (!banned.contains(id.toString())) {
                continue;
            }
            it.remove();
            removed++;
            // source=file-direct 标识：被禁列表来自磁盘 TOML 直读（BannedFileReader），
            // 不是 Forge 配置内存值——排查时靠这个区分来源。
            LOGGER.info("[iudebug] banned registration skipped: {} (config source: file-direct)", id);
        }
        return removed;
    }
}
