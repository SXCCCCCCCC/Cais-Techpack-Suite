package com.sxcccccccc.iuunify.mixin;

import com.denfop.blocks.FluidName;
import com.sxcccccccc.iuunify.RegistrationPhaseGuard;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流体统一核心：把 12 种 IU 流体的 {@code FluidName.getInstance()} 重定向到 IC2
 * Refabricated（命名空间 {@code ic2_120}）的对应流体。
 *
 * <p>目标签名（jar 实证，IndustrialUpgrade-1.20.1-3.4.0.10 javap）：
 * {@code public RegistryObject<IUFluid> getInstance()}（擦除返回类型
 * {@code RegistryObject}，无重载）。枚举常量（如 {@code fluidcoolant}）是注册 id 的
 * 推导源头（FluidHandler：{@code iufluidX}/{@code iufluidX_flowing}/…），本 mixin 按
 * 枚举常量映射到 IC2R 注册名（ModFluids.kt + jar 字节码实证：coolant / hot_coolant /
 * uu_matter / weed_ex / pahoehoe_lava / biomass / distilled_water /
 * construction_foam / creosote / compressed_air / steam / superheated_steam）。
 *
 * <p>重定向语义：
 * <ul>
 *   <li>注册阶段（RegistrationPhaseGuard 为 true）——透传原逻辑（原始
 *       {@code instance} 字段），注册链零干扰；</li>
 *   <li>其余时机——12 个枚举值返回包装 RegistryObject，其 get() 惰性解析
 *       {@code ForgeRegistries.FLUIDS} 中 {@code ic2_120:xxx} 的流体。</li>
 * </ul>
 *
 * <p>包装 RegistryObject 的填充机制（forge-1.20.1-47.4.10 字节码实证）：
 * {@code RegistryObject.create(ResourceLocation, IForgeRegistry)} 构造器注册
 * {@code ObjectHolderRegistry.addHandler} 回调并立即 updateReference——与
 * DeferredRegister 自身 RegistryObject 的 {@code (name, owner, modid, optional)}
 * 构造器同一机制，在 FLUIDS 注册表冻结时自动填充 value。因此：
 * <ul>
 *   <li>12 流体的首次 getInstance().get() 必发生在注册完成后（不变量：原版 IU 在
 *       注册前对 RO.get() 同样 NPE "Registry Object not present"，见 iudebug 实测
 *       文档），届时 ic2_120 流体已注册 → 包装 get() 恒成功；</li>
 *   <li>iudebug 删除 = 注册表里没有 iufluidX；包装 RO 只读 ic2_120 流体、从不写入
 *       ——"IC2 流体二次注册到 iufluidX id"不可能发生（全树仅 FluidHandler 的
 *       注册调用会被跳过，且不读 getInstance()）。</li>
 * </ul>
 *
 * <p>0.3.3 修复（2026-08-24 实崩复盘，latest.log 第 1116 行原文）：
 * {@code @Shadow method name in iuunify.mixins.json:FluidNameRedirectMixin ...
 * was not located in the target class com.denfop.blocks.FluidName}。0.3.2 用的
 * {@code @Shadow(remap=false) public String name()}（Mixin 官方对 final 方法
 * java.lang.Enum.name() 的非 abstract shadow 写法）解析失败——非 abstract shadow
 * 的目标必须在目标类（含父链）中解析到，而 {@code java.lang.Enum} 是 JDK 类，
 * Mixin ClassInfo 的父链在 JDK 边界断裂，解析不到 {@code name()}。hasInstance()
 * 的 abstract shadow 目标在 FluidName 自身，不受影响。
 *
 * <p>0.3.3 修复方式：本类不再使用任何 {@code @Shadow}。IC2_FLUID_NAME 的 key 从
 * 枚举名字符串改为枚举常量本身（{@code Map<FluidName, String>}），handler 内
 * {@code IC2_FLUID_NAME.get(this)} 直接以目标实例（合并后 this 即枚举实例）为 key，
 * 枚举哈希按常量身份命中；name() 与 hasInstance() 两个 shadow 一并删除。
 * hasInstance() 检查随之移除的语义变化：注册阶段外 + 实例未初始化（mod 构造期
 * setInstance 之前）原本走原方法逻辑抛 ISE（崩溃路径，IU 自身在注册链内零调用
 * getInstance() 已实证），现改为返回包装 RO——该路径无实际调用方，语义放宽为
 * 不崩溃、延迟到 get()（同样无人调用）。
 *
 * <p>未覆盖引用（世界生成 9 处 {@code (IUFluid)} 强转等）会得到 IC2R 流体实例而非
 * {@code IUFluid} 子类 → 运行时 ClassCastException = 设计反馈（阶段二逐个改写）。
 */
@Mixin(value = FluidName.class)
public abstract class FluidNameRedirectMixin {

    /** 12 种统一流体：枚举常量 → IC2R 注册名（ic2_120 命名空间）。fluidair 对应 compressed_air。 */
    private static final Map<FluidName, String> IC2_FLUID_NAME = Map.ofEntries(
            Map.entry(FluidName.fluidcoolant, "coolant"),
            Map.entry(FluidName.fluidhot_coolant, "hot_coolant"),
            Map.entry(FluidName.fluiduu_matter, "uu_matter"),
            Map.entry(FluidName.fluidweed_ex, "weed_ex"),
            Map.entry(FluidName.fluidpahoehoe_lava, "pahoehoe_lava"),
            Map.entry(FluidName.fluidbiomass, "biomass"),
            Map.entry(FluidName.fluiddistilled_water, "distilled_water"),
            Map.entry(FluidName.fluidconstruction_foam, "construction_foam"),
            Map.entry(FluidName.fluidcreosote, "creosote"),
            Map.entry(FluidName.fluidair, "compressed_air"),
            Map.entry(FluidName.fluidsteam, "steam"),
            Map.entry(FluidName.fluidsuperheated_steam, "superheated_steam")
    );

    /** 包装 RegistryObject 缓存（每流体一个；get() 在注册表冻结后经 ObjectHolderRegistry 自动填充）。 */
    private static final Map<String, RegistryObject<Fluid>> WRAPPERS = new ConcurrentHashMap<>();

    /**
     * 0.3.2 修复（2026-08-24 实崩复盘，CallbackInjector$Callback.getDescriptor
     * 字节码实证）：@Inject handler 参数必须精确 = [目标方法参数] + [CallbackInfo]，
     * 非静态 handler 的目标实例（this）不占参数位——0.3.1 写的
     * {@code (FluidName self, CIR)} 多了一个参数位 → "Invalid descriptor ... Expected
     * (CallbackInfoReturnable) but found (FluidName, CallbackInfoReturnable)" →
     * FluidName 类加载失败连锁 ModList 崩溃（同 0.3.1 的 IUCoreRegisterItemTabMixin
     * 排雷序列）。目标 {@code getInstance()} 是实例方法（可读源码实证：用 this.instance），
     * handler 合并后 this 即枚举实例。
     *
     * <p>0.3.3：不再通过 @Shadow 取枚举名/实例状态（见类 javadoc 崩溃复盘）；
     * 枚举名映射改用常量 key（Map.get(Object) 编译期接受 this，运行时按枚举身份命中）。
     */
    @Inject(
            method = "getInstance()Lnet/minecraftforge/registries/RegistryObject;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void iuunify$redirectToIc2(CallbackInfoReturnable<RegistryObject<Fluid>> cir) {
        if (RegistrationPhaseGuard.isRegistrationActive()) {
            return;
        }
        String ic2Name = IC2_FLUID_NAME.get(this);
        if (ic2Name == null) {
            return;
        }
        cir.setReturnValue(WRAPPERS.computeIfAbsent(ic2Name, name ->
                RegistryObject.create(new ResourceLocation("ic2_120", name), ForgeRegistries.FLUIDS)));
    }
}
