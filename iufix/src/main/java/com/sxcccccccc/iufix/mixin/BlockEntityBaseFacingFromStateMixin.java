package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blocks.BlockTileEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.2：BlockEntityBase.facing 在 NBT 读取后以方块状态为准（state 是朝向的
 * 事实来源：渲染、碰撞、刷新全部读它；磁盘存档里 state 与 NBT 总是同源）。
 *
 * <p><b>机理：</b>{@code BlockStateBaseFacingRotateMixin} 修好 state facing
 * 随房屋旋转后，世界生成时（jigsaw 模板放置）master 铁砧的 state 已经是
 * 旋转后的朝向（字节码实证 StructureTemplate.placeInWorld 先 setBlock 再
 * loadAdditional，BE 构造时 vanilla 已把旋转后的 state 存入 f_58856_）。
 * 而 {@code BlockEntityBase.readFromNBT} 仍然无条件读 NBT 里模板烘焙的
 * 旧 facing（east）→ 本 mixin 在 readFromNBT 尾部把 this.facing 覆写为
 * state 的 facing。这样 {@code MultiCellCollisionManager.refresh}（1.4.0 代理
 * 层 + 1.4.1 ChunkLoadCollisionRefreshFix 已调度）拿到正确朝向 → 碰撞代理
 * 摆到旋转后的单元格位置（铁砧行平行于旋转后的墙、全落空气）。
 *
 * <p><b>各路径安全性：</b>
 * <ul>
 * <li>世界生成：state=旋转后朝向 → 覆写生效（这是要修的场景）。</li>
 * <li>磁盘加载：state 与 NBT 同源（存档时 setFacing 双向同步，行内实证
 * BlockEntityBase.setFacing 会写回 state）→ 覆写为同一值，无副作用。</li>
 * <li>getBlockState() 被 IU 覆写（m_58900_）：f_58856_ 缓存存在时原样返回；
 * 缓存为 null 时从 this.facing 合成状态 → 覆写读到的是同一个字段，恒为
 * 无操作，不会破坏任何路径。</li>
 * <li>state 的方块不是 BlockTileEntity 或 facingProperty 为 null → 跳过。</li>
 * </ul>
 *
 * <p>直接写 public byte facing 字段而不是调 setFacing()：setFacing 会发网络
 * 包并把 BE 朝向写回世界 state（玩家 wrench 旋转路径专用），加载期调用会
 * 污染加载流程；直接覆写字段与 readFromNBT 本体同语义（getFacing 就是
 * Direction.values()[this.facing]）。
 */
@Mixin(value = BlockEntityBase.class, remap = false)
public abstract class BlockEntityBaseFacingFromStateMixin {

    /** denfop 自有字段，不做 reobf，运行时名即官方名（与 1.4.1 masterPos 同款 @Shadow-only 写法）。 */
    @Shadow(remap = false)
    private byte facing;

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void iufix$facingFromState(CompoundTag nbt, CallbackInfo ci) {
        // 原版成员经转型调用，源码 mojmap 名由 reobfJar 构建期转 SRG（m_58900_）
        BlockState state = ((BlockEntity) (Object) this).getBlockState();
        if (state.getBlock() instanceof BlockTileEntity<?> block && block.facingProperty != null) {
            this.facing = (byte) state.getValue(block.facingProperty).ordinal();
        }
    }
}
