package com.sxcccccccc.tecend.compat;

import com.sxcccccccc.tecend.TecEnd;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

/**
 * 「残片」这一族物品的 tooltip：名字换成我们自己的，GT 的说明行删掉，调试信息保留。
 *
 * <p>三件事：</p>
 * <ol>
 *   <li><b>名字</b>：把第 0 行替换成我们自己的可翻译组件（{@code tecend.fragment.*}）。
 *       GT 那边名字是按「材质名 + 前缀名」拼的（源码 {@code TagPrefix.getUnlocalizedName(material)}
 *       会优先查 {@code item.<材质所属mod>.<idPattern>}，查不到再回退 {@code tagprefix.<前缀>}），
 *       所以单靠 lang 键受它那套回退规则影响；这里在客户端直接接管，跟随语言且立刻生效。</li>
 *   <li><b>说明</b>：GT 给脏粉/净粉挂的"右击盛有水的炼药锅以清洗"之类的提示行删掉。</li>
 *   <li><b>调试信息保留</b>：F3+H 的命名空间 id 行与标签行都含 {@code ':'}，据此识别并保留 ——
 *       早先"只留第 0 行、其余全清"的写法会把它们一起清掉（踩过的坑）。</li>
 * </ol>
 *
 * <p>用 Forge 的 {@link ItemTooltipEvent}（forge 总线事件）而不是 mixin：不改任何原版/上游类。</p>
 */
@Mod.EventBusSubscriber(modid = TecEnd.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class FragmentTooltipFilter {
    private FragmentTooltipFilter() {}

    /** id 路径 → 名字键 */
    private static final Map<String, String> NAMES = Map.of(
            "trophy_fragment_clump", "tecend.fragment.clump",                   // 远古奖杯残片
            "trophy_fragment_shard", "tecend.fragment.shard",                   // 粉碎的残片
            "trophy_fragment_crystal", "tecend.fragment.crystal",               // 小残片晶体
            "dirty_trophy_fragment_dust", "tecend.fragment.dirty_dust",         // 不那么干净的残片粉末
            "impure_trophy_fragment_dust", "tecend.fragment.impure_dust",       // 黏糊糊的残片粉末
            "trophy_fragment_dust", "tecend.fragment.dust",                     // 残片粉末
            "pure_trophy_fragment_dust", "tecend.fragment.pure_dust",           // 干净残片粉末
            "trophy_fragment_gem", "tecend.fragment.gem");                      // 残片

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null || !id.getPath().contains("trophy_fragment")) {
            return;
        }

        List<Component> lines = event.getToolTip();
        if (lines.isEmpty()) {
            return;
        }

        // ① 名字：客户端直接给，跟随语言
        String key = NAMES.get(id.getPath());
        lines.set(0, key != null ? Component.translatable(key) : Component.literal(id.getPath()));

        // ② 其余行：保留含 ':' 的（F3+H 的 id / 标签行），删掉 GT 的提示行
        if (lines.size() > 1) {
            lines.subList(1, lines.size()).removeIf(line -> !line.getString().contains(":"));
        }
    }
}
