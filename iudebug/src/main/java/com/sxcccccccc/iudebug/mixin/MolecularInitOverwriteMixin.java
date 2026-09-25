package com.sxcccccccc.iudebug.mixin;

import com.denfop.blockentity.base.BlockEntityMolecularTransformer;
import com.sxcccccccc.iudebug.config.IudebugConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

/**
 * 用户裁决（2026-09-04）：分子重构机 {@code init()} 全部配方改为 iudebug 配置驱动
 * ——以后魔改配方只改 config/iudebug-common.toml 的 [molecular] recipes 一行，
 * 不再每改一处写一个新 mixin。
 *
 * <p><b>背景</b>：核心升级链 4 换 1（4^13=67M 铱锭挂 30+ 小时/核心，与包宗旨冲突）
 * → 用户裁决 3 换 1（3^13=1.59M）；钚/铱锭等此前已各写过一次性换源 mixin
 * （iuunify MolecularPlutoniumSwapMixin 等），用户要求统一成配置表。
 *
 * <p><b>实现</b>：@Overwrite init()（IU 自有方法，官方名，remap=false）。
 * init() 原生体只注册配方（构造器做其他初始化），因此整方法替换安全。
 * 逐行解析配置：{@code 输入*数量|输出*数量|能量}；输入带 {@code #} 前缀走
 * tag 版 {@code addrecipe(String, ItemStack, double)}，否则物品版。物品经
 * {@link BuiltInRegistries} 现取（init 阶段注册表已完整）；id 不存在/数量非法/
 * 段数不对一律抛 RuntimeException——"解析有错直接崩"，不留静默错误。
 *
 * <p><b>与 iuunify 0.5.3 的交互</b>：MolecularPlutoniumSwapMixin 的 @ModifyArg
 * 挂在 init() 的 addrecipe(ItemStack,…) 调用上。本 @Overwrite 替换整个方法体后，
 * 无论两个 mixin 应用先后，该 swap 的过滤（输入==IU 钚才换）对配置版输入
 * （ic2_120:plutonium）恒不命中——惰性无害；钚换源语义由配置表直接承担。
 */
@Mixin(value = BlockEntityMolecularTransformer.class, remap = false)
public abstract class MolecularInitOverwriteMixin {

    /**
     * 替换原生 init()：逐行解析 [molecular] recipes 配置并注册。
     * 静态 addrecipe 以目标类限定名调用（mixin 类编译期不继承目标类，运行时合并后
     * 即同一方法）。init() 原生体只做配方注册，整方法替换安全。
     */
    @Overwrite(remap = false)
    public void init() {
        for (String line : IudebugConfig.MOLECULAR_RECIPES.get()) {
            String[] parts = line.split("\\|");
            if (parts.length != 3) {
                throw new RuntimeException("[iudebug] molecular 配方格式错误（应为 输入*数量|输出*数量|能量）: " + line);
            }
            double energy = Double.parseDouble(parts[2]);
            ItemStack output = parseStack(parts[1], line);
            if (parts[0].startsWith("#")) {
                // tag 输入：只取 id 部分（忽略 *数量——tag 配方输入无数量语义，
                // 原版就是裸 tag 字符串；IU InputHandler 会自行小写化后建
                // ResourceLocation，残留 "*1" 会撞非法字符直接抛
                // ResourceLocationException——1.2.0 启动日志实锤）
                String tag = parts[0].substring(1).split("\\*")[0];
                BlockEntityMolecularTransformer.addrecipe(tag, output, energy);
            } else {
                BlockEntityMolecularTransformer.addrecipe(parseStack(parts[0], line), output, energy);
            }
        }
    }

    private static ItemStack parseStack(String spec, String line) {
        String[] p = spec.split("\\*");
        ResourceLocation id = new ResourceLocation(p[0]);
        int count = p.length > 1 ? Integer.parseInt(p[1]) : 1;
        if (count < 1) {
            throw new RuntimeException("[iudebug] molecular 数量非法: " + line);
        }
        Item item = BuiltInRegistries.ITEM.getOptional(id).orElseThrow(
                () -> new RuntimeException("[iudebug] molecular 物品不存在: " + p[0] + " （整行: " + line + "）")
        );
        return new ItemStack(item, count);
    }
}
