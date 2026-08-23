package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 用户裁决（1.3.2）：所有 IU 机器方块用镐子即可挖掘，挖掘等级要求铁镐（tier >= 2）。
 *
 * 背景：BlockTileEntity.canHarvestBlock 内 switch 按 HarvestTool 分支检查工具 tag，
 * 且各分支 AND super.canHarvestBlock（Forge 链）。Wrench 分支查不存在的
 * minecraft:wrench tag → 恒 false → 镐子挖 Wrench 机器无掉落（1.0.3 实测"有些东西
 * 挖不了"）；iucore 式 create 重定向 getHarvestTool 无效，因为 switch 在挖掘时读
 * te.teBlock.getHarvestTool() 而非 create 时的值。
 *
 * 做法：canHarvestBlock HEAD cancellable 整体重写判定——保留原守卫（TE 存在 +
 * canEntityDestroy），然后"手持镐子（minecraft:pickaxes）且 tier >= 2 → true，
 * 否则 false"。super 链被整体绕过，不受 Forge harvest-level 现状（-1，石镐也能过）
 * 影响。扳手拆除路径（getDrops / Wrenchable 右键）不在本方法内，未触碰。
 */
@Mixin(targets = "com.denfop.blocks.BlockTileEntity", remap = false)
public class IUHarvestToolMixin {

    private static final ResourceLocation PICKAXES = new ResourceLocation("minecraft", "pickaxes");

    private static final int IRON_TIER = 2;

    @Inject(method = "canHarvestBlock", at = @At("HEAD"), cancellable = true)
    private void iufix$canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player,
                                       CallbackInfoReturnable<Boolean> cir) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BlockEntityBase te)) {
            cir.setReturnValue(false);
            return;
        }
        if (!te.canEntityDestroy(player)) {
            cir.setReturnValue(false);
            return;
        }
        ItemStack stack = player.getMainHandItem();
        if (!stack.isEmpty() && stack.is(ItemTags.create(PICKAXES))
                && stack.getItem() instanceof TieredItem tiered
                && tiered.getTier().getLevel() >= IRON_TIER) {
            cir.setReturnValue(true);
            return;
        }
        cir.setReturnValue(false);
    }
}
