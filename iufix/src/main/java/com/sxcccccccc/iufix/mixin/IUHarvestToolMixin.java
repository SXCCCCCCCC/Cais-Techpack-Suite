package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 用户裁决（1.6.0，覆盖 1.3.2 铁镐门槛；1.6.1 补充）：任何 IU 机器方块用任何镐子即可挖掘——
 * {@code minecraft:pickaxes} 或 {@code forge:tools/paxels} 任一 tag 命中即合格（1.6.1 补
 * paxel——成员含 mysticalagradditions 六阶 paxel + forge 九个子 tag，kubejs 导出核
 * 实）；并取消 TieredItem/tier>=2 检查（木镐/石镐/GT 镐/AE2 石英镐/IC2 钻机等全部放行）。
 * 挖掉必须完整掉落机器——掉落链已核查（见下），只要服务器 canHarvestBlock==true 即
 * 100% 完整机器掉落，本 mixin 无需触碰掉落路径。
 *
 * <p>背景：BlockTileEntity.canHarvestBlock 内 switch 按 HarvestTool 分支检查工具 tag，
 * 且各分支 AND super.canHarvestBlock（Forge 链）。Wrench 分支查不存在的
 * minecraft:wrench tag → 恒 false → 镐子挖 Wrench 机器无掉落（1.0.3 实测"有些东西
 * 挖不了"）；iucore 式 create 重定向 getHarvestTool 无效，因为 switch 在挖掘时读
 * te.teBlock.getHarvestTool() 而非 create 时的值。
 *
 * <p>做法：canHarvestBlock HEAD cancellable 整体重写判定——保留原守卫（TE 存在 +
 * canEntityDestroy，私有组件/负债太阳能板/蜂群等 IU 设计性限制原样保留），然后
 * "手持任意 minecraft:pickaxes / forge:tools/paxels tag 工具 → true，否则 false"。
 * super 链被整体绕过，不受 Forge harvest-level 现状影响。扳手拆除路径
 * （wrenchCanRemove / Wrenchable 右键）不在本方法内，未触碰。
 *
 * <p>掉落链核查结论（桌面源码 3.4.0.1，与部署 3.4.0.10 一致）：
 * {@code BlockTileEntity.playerDestroy}(486-509) 的 chance =
 * te.getLevel().random.nextInt(100)，但 {@code BlockEntityBase.adjustDrop(drop,wrench,fortune)}
 * (1146-1180) 的非扳手分支（!wrench）**完全不读取 fortune**——所有 DefaultDrop 变体
 * （Self=完整机器 getPickBlock、Machine/AdvMachine=blockResource 机器/高级机器、
 * Generator=基础机器）都是无条件返回；仅分支 (case None) 无掉落，而其唯一来源
 * BlockCollision（碰撞代理，非可放置机器）；NBT 由组件 needWriteNBTToDrops 写入掉落品。
 * 即 hasHarvest==true → playerDestroy → 100% 完整机器（含 NBT）掉落，无需额外注入。
 *
 * <p>取证：@Inject require=1（0 命中直接应用失败，不被 mixins.json 的
 * injectors.defaultRequire=0 静默吞掉）+ 一次性 warn 探针（每个 JVM 首个
 * canHarvestBlock 调用打一行，常驻留作"mixin 是否应用"回归判断）。
 */
@Mixin(targets = "com.denfop.blocks.BlockTileEntity", remap = false)
public class IUHarvestToolMixin {

    private static final Logger LOGGER = LoggerFactory.getLogger("iufix");

    private static final TagKey<Item> PICKAXES = ItemTags.create(new ResourceLocation("minecraft", "pickaxes"));

    private static final TagKey<Item> PAXELS = ItemTags.create(new ResourceLocation("forge", "tools/paxels"));

    private static final AtomicBoolean PROBED = new AtomicBoolean(false);

    @Inject(method = "canHarvestBlock", at = @At("HEAD"), cancellable = true, require = 1)
    private void iufix$canHarvestBlock(BlockState state, BlockGetter world, BlockPos pos, Player player,
                                       CallbackInfoReturnable<Boolean> cir) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BlockEntityBase te)) {
            cir.setReturnValue(false);
            return;
        }
        if (!te.canEntityDestroy(player)) {
            // 私有组件/负债太阳能板/蜂群等 IU 设计性限制：保持不可挖
            cir.setReturnValue(false);
            return;
        }
        ItemStack stack = player.getMainHandItem();
        boolean ok = !stack.isEmpty() && (stack.is(PICKAXES) || stack.is(PAXELS));
        cir.setReturnValue(ok);
        if (PROBED.compareAndSet(false, true)) {
            LOGGER.warn("[iufix] IUHarvestToolMixin active: canHarvestBlock overlaid; first call " +
                    "player={} pos={} tool={} inPickaxesOrPaxelsTag={} allows={}",
                    player.getGameProfile().getName(), pos, stack,
                    !stack.isEmpty() && (stack.is(PICKAXES) || stack.is(PAXELS)), ok);
        }
    }
}
