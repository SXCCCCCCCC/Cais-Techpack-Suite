package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 1.4.3-hotfix3：canPlace 占用判定改用原版 canBeReplaced 语义（用户裁决）。
 *
 * <p>现象：铁砧（BlockEntityAnvil，占地 AABB (-1,0,0,2,1,1)/(0,0,-1,1,1,2) 共 3 格）
 * 能压在花上放置；其他 mod 的同体积结构会拒绝。用户实测 + 诊断确认。
 *
 * <p>根因（源码证据）：{@code BlockEntityBase.canPlace}（行 582-613，铁砧未覆写、多胞
 * 结构共用）遍历 AABB 每格，拒绝条件为 {@code !state.isAir() && !state.getCollisionShape().isEmpty()}。
 * 花（虞美人等）isAir=false 但碰撞形状为空 → 条件不成立 → 被当作"可穿透"放行。
 *
 * <p>修复：@Redirect 该 getCollisionShape（m_60812_）调用点，返回
 * {@code state.isAir() || state.is(BlockTags.REPLACEABLE) ? Shapes.empty() : Shapes.box(0,0,0,1,1,1)}。
 * 原条件 {@code !isAir && !shape.isEmpty()} 随之恒等于「不可替换 → 拒绝」：
 * 空气短路放行；#minecraft:replaceable 标签方块（草/水/雪层/藤蔓/火把）形状空放行；
 * 不可替换方块（花/石头/告示牌）形状非空拒绝。
 * 副作用变化：雪层等"可替换但碰撞非空"的方块由拒绝改为放行——正是原版语义。
 * canPlace 其余逻辑（朝向计算、facing 复原、循环结构）一行未动。
 *
 * <p>语义实证（javap joined srg jar）：两参 canBeReplaced(BlockGetter, BlockPos) = m_60796_
 * （协调者消息中的 m_60806_ 不存在），字节码委托 BlockStateBase.f_60602_（isReplaceable
 * 谓词，源自 BlockBehaviour.Properties.isReplaceable 默认 lambda，语义 = isAir 或
 * #minecraft:replaceable 标签）。mojmap 编译环境（mapped_official jar javap 实证）中
 * BlockStateBase 只有无参 canBeReplaced()（= Properties.replaceable 静态配置字段，非标签
 * 逻辑）与一参 canBeReplaced(BlockPlaceContext)/canBeReplaced(Fluid)，两参版本不存在——
 * 故用 isAir() || is(BlockTags.REPLACEABLE) 显式表达同一标签语义（原版 initCache 同款
 * 组合）。target 按运行时 SRG 写（m_60812_ = getCollisionShape），remap=false；
 * handler 内 vanilla 成员写 mojmap 名，reobfJar 机械转 SRG。
 */
@Mixin(value = BlockEntityBase.class, remap = false)
public abstract class BlockEntityBaseCanPlaceMixin {

    @Redirect(method = "canPlace",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;m_60812_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"))
    private VoxelShape iufix$placementCollision(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isAir() || state.is(BlockTags.REPLACEABLE)
                ? Shapes.empty()
                : Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    }
}
