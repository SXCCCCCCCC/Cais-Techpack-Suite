package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blockentity.collision.BlockEntityCollisionProxy;
import com.sxcccccccc.iufix.util.MultiblockCollisionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.0：BlockEntityCollisionProxy 按 IE 模式重构（相对偏移 + 双轨读取 + 健康校验）。
 *
 * <p><b>NBT 双轨：</b>新写入格式为相对偏移键 {@code relX/relY/relZ}
 * （IE 的 posInMB 模式：master 坐标 = 自身坐标 − 偏移，纯函数推导，结构上免疫
 * 死指针/互相指环；且对 WE/结构方块原样搬运 NBT 天然友好——偏移与位置无关）。
 * 读取时新键优先；没有新键则读旧绝对键 {@code masterX/masterY/masterZ}
 * （兼容 1.4.0 之前的存档），旧键 + 新键并存时新键胜出。保存一律只写新键，
 * 旧世界数据在下一次落盘时自动迁移成相对格式。
 *
 * <p><b>健康校验（读取时）：</b>推导出的 master 位若 chunk 已加载且该位 TE
 * 不是 BlockEntityBase（旧数据里可能指向空气/代理/其他方块），视为死指针，
 * masterPos 置 null——getDelegatedShape 的空形状分支自然接管，不再崩溃。
 * chunk 未加载时保留推导值（乐观等待；refresh 全维度清扫会在 master 所在
 * chunk 加载后兜底回收）。chunk 加载期间不做强制加载，避免重入。
 *
 * <p><b>setMasterPos 重构：</b>写入前校验目标位 TE 是 BlockEntityBase，
 * 校验失败拒绝写入（不再把死指针写进 NBT）；并去掉原实现里的 setChanged——
 * setChanged → getBlockState → getCollisionShape → getDelegatedShape 的
 * 重入链是 16.09 崩溃的第三层根源（refresh 摆放代理时 collision 查询重入
 * 同一 tick）。refresh 摆放循环里 setBlock 本身已标记 chunk 脏，不再需要
 * setChanged 补标记。
 *
 * <p>denfop 自有成员运行时名即官方名（masterPos/setMasterPos），原版覆写
 * （m_142466_=loadAdditional、m_183515_=saveAdditional）在注入串里用 SRG 名，
 * handler 体内对原版成员的引用由 reobf 映射。
 */
@Mixin(value = BlockEntityCollisionProxy.class, remap = false)
public abstract class BlockEntityCollisionProxyIEModeMixin extends BlockEntityCollisionProxy {

    @Shadow
    private BlockPos masterPos;

    protected BlockEntityCollisionProxyIEModeMixin(BlockPos pos, BlockState state) {
        super(pos, state);
    }

    /** 构造时自注册，供全维度清扫/反污染命令枚举（1.20.1 无已加载 BE 全量枚举 API）。 */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void iufix$register(BlockPos pos, BlockState state, CallbackInfo ci) {
        MultiblockCollisionUtil.register(this);
    }

    /** loadAdditional 重写：双轨读取 + 健康校验（新键优先，旧绝对键兼容）。 */
    @Inject(method = "m_142466_", at = @At("HEAD"), cancellable = true)
    private void iufix$loadAdditional(CompoundTag tag, CallbackInfo ci) {
        BlockPos derived = null;
        if (tag.contains("relX") && tag.contains("relY") && tag.contains("relZ")) {
            BlockPos offset = new BlockPos(tag.getInt("relX"), tag.getInt("relY"), tag.getInt("relZ"));
            derived = this.worldPosition.subtract(offset);
        } else if (tag.contains("masterX") && tag.contains("masterY") && tag.contains("masterZ")) {
            derived = new BlockPos(tag.getInt("masterX"), tag.getInt("masterY"), tag.getInt("masterZ"));
        }
        if (derived != null) {
            if (this.level != null && this.level.isLoaded(derived) && !(this.level.getBlockEntity(derived) instanceof BlockEntityBase)) {
                this.masterPos = null;
                MultiblockCollisionUtil.warnOnce(derived.asLong() ^ Long.rotateLeft(this.worldPosition.asLong(), 32),
                        "读取代理 NBT：位置 " + this.worldPosition.toShortString()
                                + " 推导出的 master @" + derived.toShortString()
                                + " 处不是 BlockEntityBase（旧数据死指针），已按空 master 加载，后续清扫会回收");
            } else {
                this.masterPos = derived;
            }
        } else {
            this.masterPos = null;
        }
        ci.cancel();
    }

    /** saveAdditional 重写：只写相对偏移新键（旧键不写，读时兼容，存量数据自动迁移）。 */
    @Inject(method = "m_183515_", at = @At("HEAD"), cancellable = true)
    private void iufix$saveAdditional(CompoundTag tag, CallbackInfo ci) {
        if (this.masterPos != null) {
            BlockPos offset = this.worldPosition.subtract(this.masterPos);
            tag.putInt("relX", offset.getX());
            tag.putInt("relY", offset.getY());
            tag.putInt("relZ", offset.getZ());
        }
        ci.cancel();
    }

    /** setMasterPos 重构：目标位类型校验 + 去掉重入路径上的 setChanged。 */
    @Inject(method = "setMasterPos", at = @At("HEAD"), cancellable = true)
    private void iufix$setMasterPos(BlockPos masterPos, CallbackInfo ci) {
        if (masterPos == null) {
            ci.cancel();
            return;
        }
        if (this.level == null || !(this.level.getBlockEntity(masterPos) instanceof BlockEntityBase)) {
            MultiblockCollisionUtil.warnOnce(masterPos.asLong() ^ Long.rotateLeft(this.worldPosition.asLong(), 30),
                    "setMasterPos 拒绝写入：位置 " + this.worldPosition.toShortString()
                            + " 的目标 master @" + masterPos.toShortString()
                            + " 处不是 BlockEntityBase，指针保持原值");
            ci.cancel();
            return;
        }
        this.masterPos = masterPos;
        ci.cancel();
    }
}
