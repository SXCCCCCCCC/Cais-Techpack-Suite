package com.sxcccccccc.iufix.mixin;

import com.denfop.IUCore;
import com.denfop.IUItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.3-hotfix7：开局不再自动发放指南书与探矿仪。
 *
 * <p>现象：新玩家首次进入世界（UUID 不在存档 guide_book 记录里）时，背包自动出现
 * 指南书（iu:guide_book）和探矿仪（iu:sensor）。两物品现在都有合成配方
 * （guide_book：BaseRecipeFour.addShapelessRecipe 行 632；探矿仪：
 * BasicRecipeTwo 行 2977），开局白送属于多余，经用户裁决移除。
 *
 * <p>根因（源码 + javap 交叉核对 3.4.0.10 发布 jar）：发放点唯一且只在服务端——
 * {@code IUCore.loginPlayer(PlayerEvent.PlayerLoggedInEvent)}（IUCore.java 行 1002-1019）：
 * 客户端分支直接 return（字节码 13: return，无 addItem 调用），服务端分支按
 * {@code !GuideBookCore.uuidGuideMap.containsKey(uuid)} 判定"首次"（该 map 经
 * WorldSavedDataIU 的 "guide_book" NBT 跨会话持久化，行 242-272/554-576），命中后
 * 依次 {@code event.getEntity().addItem(new ItemStack(IUItem.book.getItem()))} 与
 * {@code addItem(new ItemStack(IUItem.veinsencor.getStack(0)))}。字节码实证两处调用
 * 均为 {@code Player.m_36356_(Lnet/minecraft/world/item/ItemStack;)Z}（SRG 名），
 * 返回值被 pop 忽略。客户端 EventAutoQuests 只做任务完成检测，不发放物品。
 *
 * <p>修复：@Redirect 该方法内全部 m_36356_ 调用点（当前恰为上述两处），handler 按物品
 * 守卫——被添加的是指南书/探矿仪则直接返回 true（原代码本就忽略返回值，语义不变），
 * 其余物品照旧走原 addItem 路径（防未来版本在 loginPlayer 里新增其他发放）。
 * loginPlayer 其余逻辑（GuideBookCore 首次 load / loadOrThrow、PacketUpdateVeinData、
 * PacketUpdateRelocator、辐射初始化等）一行未动。
 *
 * <p>target 按运行时 SRG 名写（javap 发布 jar 实证 m_36356_），remap=false；
 * handler 内 vanilla 成员写 mojmap 名，reobfJar 打包时自动转 SRG。
 */
@Mixin(value = IUCore.class, remap = false)
public abstract class LoginGiftDisableMixin {

    @Redirect(method = "loginPlayer",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;m_36356_(Lnet/minecraft/world/item/ItemStack;)Z"),
            require = 1)
    private boolean iufix$blockStartGifts(Player player, ItemStack stack) {
        if (stack.is(IUItem.book.getItem()) || stack.is(IUItem.veinsencor.getStack(0))) {
            return true;
        }
        return player.addItem(stack);
    }
}
