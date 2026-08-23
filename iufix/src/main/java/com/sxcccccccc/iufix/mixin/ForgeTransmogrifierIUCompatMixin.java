package com.sxcccccccc.iufix.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 1.3.5/1.3.6：WorldEdit 7.2.15 × IU 自定义方块属性（com.denfop.blocks.state.State）兼容。
 *
 * <p>背景（javap 核实，WE jar 为 Forge 版官方名字节码）：
 * <ul>
 *   <li>WE 反向转译 {@code ForgeTransmogrifier.transmogToMinecraftProperties}（private static，
 *       WE BlockState → MC BlockState）对每个属性 entry 只特判 DirectionProperty/EnumProperty，
 *       其余一律 {@code stateDefinition.getProperty(weProp.getName())} 按名查表后
 *       {@code blockState.setValue(mcProp, value)}；IU 的 "type" 属性（TypeProperty，值对象
 *       State 携带运行时 teBlock 引用）不是原版 Direction/Enum 属性 → 按名查表失败或值域
 *       不匹配 → 启动期注册表构建（PlatformReadyEvent 枚举状态）与 //regen 状态适配崩。</li>
 *   <li>WE 正向把这类属性包成 {@code IPropertyAdapter}（PROPERTY_CACHE.loader 的兜底分支），
 *       构造器注入 {@code private final Property property}（原版属性引用）；adapter 的
 *       getName()/getValueFor()/getValues() 全部委托给该包裹属性。IPropertyAdapter 是
 *       package-private 类，编译期不可见。</li>
 * </ul>
 *
 * <p>修复（1.3.6 次修，治启动期残留）：@Inject HEAD cancellable 复刻整个循环——entry key
 * 为 IPropertyAdapter 时直通其包裹的原版属性（不经按名查表、不做 Direction/Enum 特判：
 * adapter 只包非 Direction/Enum 属性）；其余 entry 完整复刻原逻辑（DirectionProperty →
 * ForgeAdapter.adapt、EnumProperty → getValue().orElseThrow）。直通对 IU "type" 属性
 * （值=State 运行时实例、按方块实例化）必撞 allowedValues 校验——setValue 抛
 * IllegalArgumentException 时**捕获并跳过该条目**（不设值、不中断循环），让 WE 注册表
 * 构建走完；被跳过的属性由主修（BlockTypeIUCompatMixin 的 getState 查空回退默认状态）兜底。
 * 包裹属性经反射取 private final property 字段（static 懒缓存）。remap=false：target 串按
 * 运行时官方名原样进 jar；mixin 类字节码里的原版成员引用由 reobf 阶段统一重映射回 SRG。
 */
@Mixin(targets = "com.sk89q.worldedit.forge.internal.ForgeTransmogrifier", remap = false)
public abstract class ForgeTransmogrifierIUCompatMixin {

    private static final String ADAPTER_CLASS = "com.sk89q.worldedit.forge.internal.IPropertyAdapter";
    private static Field adapterPropertyField;

    @Inject(method = "transmogToMinecraftProperties", at = @At("HEAD"), cancellable = true)
    private static void iufix$transmogToMinecraftProperties(
            StateDefinition<?, ?> stateDefinition,
            BlockState state,
            Map<com.sk89q.worldedit.registry.state.Property<?>, Object> properties,
            CallbackInfoReturnable<BlockState> cir) {

        BlockState result = state;
        for (Map.Entry<com.sk89q.worldedit.registry.state.Property<?>, Object> entry : properties.entrySet()) {
            com.sk89q.worldedit.registry.state.Property<?> weProp = entry.getKey();
            Property<?> mcProp;
            Comparable<?> value = (Comparable<?>) entry.getValue();

            Property<?> wrapped = iufix$wrappedProperty(weProp);
            if (wrapped != null) {
                // IPropertyAdapter 直通：包裹的原版属性 + 正向适配时原样传入的原版值
                mcProp = wrapped;
            } else {
                mcProp = stateDefinition.getProperty(weProp.getName());
                if (mcProp instanceof DirectionProperty) {
                    value = com.sk89q.worldedit.forge.ForgeAdapter
                            .adapt((com.sk89q.worldedit.util.Direction) value);
                } else if (mcProp instanceof EnumProperty) {
                    final String rawValue = (String) value;
                    final String propName = weProp.getName();
                    java.util.Optional<?> resolved = ((EnumProperty) mcProp).getValue(rawValue);
                    if (!resolved.isPresent()) {
                        throw new IllegalStateException(
                                "Unknown value '" + rawValue + "' for property '" + propName + "'");
                    }
                    value = (Comparable<?>) resolved.get();
                }
            }
            try {
                result = result.setValue((Property) mcProp, (Comparable) value);
            } catch (IllegalArgumentException e) {
                // IU "type" 属性按方块实例化（allowedValues 是各 TypeProperty 自己的值域），
                // 跨方块的值实例必然 "not an allowed value"——跳过该条目而非中断整个转译。
            }
        }
        cir.setReturnValue(result);
    }

    /** 若 weProp 是 IPropertyAdapter，返回其包裹的原版 Property；否则返回 null。 */
    private static Property<?> iufix$wrappedProperty(Object weProp) {
        Class<?> cls = weProp.getClass();
        if (!ADAPTER_CLASS.equals(cls.getName())) {
            return null;
        }
        if (adapterPropertyField == null) {
            try {
                adapterPropertyField = cls.getDeclaredField("property");
                adapterPropertyField.setAccessible(true);
            } catch (NoSuchFieldException e) {
                return null;
            }
        }
        try {
            return (Property<?>) adapterPropertyField.get(weProp);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
