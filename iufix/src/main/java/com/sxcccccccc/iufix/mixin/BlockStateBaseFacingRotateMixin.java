package com.sxcccccccc.iufix.mixin;

import com.denfop.blocks.BlockTileEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 1.4.2：IU 多胞机器方块（BlockTileEntity 系）的朝向随 jigsaw 结构旋转/镜像。
 *
 * <p><b>根因（村庄铁砧"竖排放歪、左端嵌墙"）：</b>IU 村庄房屋
 * {@code metallurg_house.nbt} 由 vanilla jigsaw 放置，模板烘焙铁砧为
 * master（state facing=east）+ 2 个碰撞代理（master ±1 Z，铁砧行沿 Z 贴西墙）。
 * jigsaw 按街道方向把房屋整体旋转 0/90/180/270 度：vanilla 会旋转代理方块
 * 的位置（rotation matrix），也会调用 {@code state.m_60717_()}
 * （= {@code BlockStateBase.rotate}，字节码：→ {@code getBlock().m_6843_()}
 * 分派）旋转 master 的方块状态——但 {@code BlockTileEntity} 继承链
 * （BlockTileEntity → Block → BlockBehaviour）没有任何 rotate/mirror 覆写，
 * 默认实现原样返回状态 → state facing 恒为 east、不随房屋旋转。
 * 而 {@code BlockEntityBase.readFromNBT} 无条件从 NBT 读 facing（也恒为 east）
 * → 碰撞代理按"east → 沿 Z"摆放，而旋转后墙在 Z 轴上而铁砧行应在 X 轴上 →
 * 代理被摆进墙里、行向与墙垂直（用户观察到的"竖排放歪、左端嵌墙"；
 * 1.4.1 的 ChunkLoadCollisionRefreshFix 补了 refresh 但输入朝向就是错的，
 * 所以用户实测仍幽灵）。散射 piece 路径（ProfessionHamletPiece.placeJobBlock
 * 固定 SOUTH）不旋转房屋、几何上无 bug，不是观察到的症状来源。
 *
 * <p><b>为什么注入 BlockStateBase 而不是 BlockTileEntity：</b>mixin 0.8.5
 * 的 {@code InjectionInfo.findRootTargets} 只把 selector 匹配目标类的
 * 声明方法（{@code classNode.methods} 循环，无层级遍历，已 javap 实证）——
 * rotate/mirror 声明在 BlockBehaviour，对 BlockTileEntity 注入继承方法会
 * 抛 SelectorConstraintException 启动崩溃。因此注入到声明方法所在的
 * BlockStateBase，再用 {@code instanceof BlockTileEntity} 过滤，等价且安全；
 * 且 BlockStateBase 是 state 旋转的必经入口，即使未来 IU 给某个方块补了
 * m_6843_ 覆写也仍然拦截到。
 *
 * <p><b>为什么这样必然正确：</b>旋转语义与 vanilla 完全一致——四向旋转
 * 下 horizontal 方向恒落 horizontal，{@code Rotation.rotate(Direction)} /
 * {@code Mirror.mirror(Direction)} 即 vanilla 官方 API；facingProperty 是
 * {@code public final Property<Direction>}（BlockTileEntity 行内实证），
 * 同一 state definition 的属性取值必在域内。旋转后 state facing 即"房屋
 * 转了多少、铁砧就该朝多少"，refresh 的代理摆放、碰撞形状、渲染全部以
 * 该值为准，天然贴合旋转后的墙。
 *
 * <p>facingProperty 可能为 null（部分 BlockTileEntity 方块 state definition
 * 无 facing 属性，构造器有守卫）→ 跳过。代理方块 BlockCollisionProxy
 * extends BaseEntityBlock，不在过滤范围内。
 *
 * <p>注入串用运行时 SRG 名（m_60717_=rotate、m_60715_=mirror，srg jar
 * javap 实证）；原版成员源码写 mojmap 名、构建期 reobfJar 机械转 SRG，
 * 与既有 mixin 同款手法；denfop 类不做 reobf、运行时名即官方名。
 */
@Mixin(value = BlockBehaviour.BlockStateBase.class, remap = false)
public abstract class BlockStateBaseFacingRotateMixin {

    @Inject(method = "m_60717_", at = @At("HEAD"), cancellable = true)
    private void iufix$rotateFacing(Rotation rotation, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof BlockTileEntity<?> block && block.facingProperty != null) {
            Property<Direction> facing = block.facingProperty;
            Direction current = state.getValue(facing);
            Direction rotated = rotation.rotate(current);
            if (rotated != current) {
                cir.setReturnValue(state.setValue(facing, rotated));
            }
        }
    }

    @Inject(method = "m_60715_", at = @At("HEAD"), cancellable = true)
    private void iufix$mirrorFacing(Mirror mirror, CallbackInfoReturnable<BlockState> cir) {
        BlockState state = (BlockState) (Object) this;
        if (state.getBlock() instanceof BlockTileEntity<?> block && block.facingProperty != null) {
            Property<Direction> facing = block.facingProperty;
            Direction current = state.getValue(facing);
            Direction mirrored = mirror.mirror(current);
            if (mirrored != current) {
                cir.setReturnValue(state.setValue(facing, mirrored));
            }
        }
    }
}
