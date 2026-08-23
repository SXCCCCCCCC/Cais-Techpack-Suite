package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.blockentity.collision.BlockEntityCollisionProxy;
import com.sxcccccccc.iufix.util.MultiblockCollisionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1.4.0/1.4.1：BlockEntityCollisionProxy 按 IE 模式重构（相对偏移 + 双轨读取 + 健康校验）。
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
 * <p><b>1.4.1 修正 mixin 应用失败：</b>1.4.0 版本写成了 {@code extends
 * BlockEntityCollisionProxy}，mixin 0.8.5 在 Forge 1.20.1 启动时直接拒绝——
 * 日志 ERROR {@code iufix.mixins.json:BlockEntityCollisionProxyIEModeMixin}：
 * "Super class ... was not found in the hierarchy of target class"（抛于
 * {@code MixinInfo$SubType$Standard.validate}，校验逻辑为
 * {@code target.hasSuperClass(mixinSuperName, Traversal.SUPER)}，extends 目标
 * 类自身的写法在该遍历下不命中）→ 整个 mixin 未应用，1.4.0 的代理层重构
 * 实际从未生效。1.4.1 改为无继承写法：@Shadow 只保留 denfop 自有字段
 * masterPos（denfop 类不做 reobf，运行时名即官方名），原版成员一律经
 * {@code ((BlockEntity)(Object)this)} 转型调用（源码 mojmap 名，构建期
 * jar 级 reobf 机械转 SRG 名，与 1.3.10 getDelegatedShape 守卫同款手法）。
 *
 * <p>denfop 自有覆写（m_142466_=loadAdditional、m_183515_=saveAdditional）
 * 是原版方法，运行时名是 SRG 名，注入串里直接用 SRG 名；denfop 自有方法
 * setMasterPos 运行时名即官方名。
 */
@Mixin(value = BlockEntityCollisionProxy.class, remap = false)
public abstract class BlockEntityCollisionProxyIEModeMixin {

    @Shadow
    private BlockPos masterPos;

    /** 构造时自注册，供全维度清扫/反污染命令枚举（1.20.1 无已加载 BE 全量枚举 API）。 */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void iufix$register(BlockPos pos, BlockState state, CallbackInfo ci) {
        MultiblockCollisionUtil.register((BlockEntityCollisionProxy) (Object) this);
    }

    /** loadAdditional 重写：双轨读取 + 健康校验（新键优先，旧绝对键兼容）。 */
    @Inject(method = "m_142466_", at = @At("HEAD"), cancellable = true)
    private void iufix$loadAdditional(CompoundTag tag, CallbackInfo ci) {
        BlockPos here = ((BlockEntity) (Object) this).getBlockPos();
        Level level = ((BlockEntity) (Object) this).getLevel();
        BlockPos derived = null;
        if (tag.contains("relX") && tag.contains("relY") && tag.contains("relZ")) {
            BlockPos offset = new BlockPos(tag.getInt("relX"), tag.getInt("relY"), tag.getInt("relZ"));
            derived = here.subtract(offset);
        } else if (tag.contains("masterX") && tag.contains("masterY") && tag.contains("masterZ")) {
            derived = new BlockPos(tag.getInt("masterX"), tag.getInt("masterY"), tag.getInt("masterZ"));
        }
        if (derived != null) {
            if (level != null && level.isLoaded(derived) && !(level.getBlockEntity(derived) instanceof BlockEntityBase)) {
                this.masterPos = null;
                MultiblockCollisionUtil.warnOnce(derived.asLong() ^ Long.rotateLeft(here.asLong(), 32),
                        "读取代理 NBT：位置 " + here.toShortString()
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
            BlockPos here = ((BlockEntity) (Object) this).getBlockPos();
            BlockPos offset = here.subtract(this.masterPos);
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
        BlockPos here = ((BlockEntity) (Object) this).getBlockPos();
        Level level = ((BlockEntity) (Object) this).getLevel();
        if (level == null || !(level.getBlockEntity(masterPos) instanceof BlockEntityBase)) {
            MultiblockCollisionUtil.warnOnce(masterPos.asLong() ^ Long.rotateLeft(here.asLong(), 30),
                    "setMasterPos 拒绝写入：位置 " + here.toShortString()
                            + " 的目标 master @" + masterPos.toShortString()
                            + " 处不是 BlockEntityBase，指针保持原值");
            ci.cancel();
            return;
        }
        this.masterPos = masterPos;
        ci.cancel();
    }
}
