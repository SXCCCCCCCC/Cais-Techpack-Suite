package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.3.1：修复 worldgen 期放置 BE 后 setRemoved 的 NPE。
 *
 * <p>根因（字节码证据）：{@code BlockEntityBase.m_6596_（setRemoved）} 中
 * {@code getLevel()（m_58904_）→ Level.isClientSide（m_5776_）} 裸调无 null 守卫；
 * {@code StructureTemplate.placeInWorld} 在 worldgen 期（level==null）放 BE 再 setRemoved
 * → NPE 崩区块生成（含 IU 村民房屋的平原村庄必现）。
 *
 * <p>修复：@Redirect 该 isClientSide 调用点，receiver（Level）为 null 时返回 false。
 * 其余方法体（super.setRemoved()、setLevelIfNull、服务端 componentList markDirty 循环）
 * 全部原样保留——不用 HEAD cancel，避免跳过 super 清理。denfop 覆写方法运行时名即
 * m_6596_，@Mixin remap=false 时注入串不重映射，target 按运行时 SRG 写。
 */
@Mixin(value = BlockEntityBase.class, remap = false)
public abstract class BlockEntityBaseSetRemovedGuard {

    @Redirect(method = "m_6596_",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_5776_()Z"))
    private boolean iufix$guardIsClientSide(Level level) {
        return level != null && level.isClientSide();
    }
}
