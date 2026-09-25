package com.sxcccccccc.tecend.common;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.sxcccccccc.tecend.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 奖杯方块实体的 Create 动力变体：接上转速后消耗应力「充气」（第 15 步的 SU 通道）。
 *
 * <p>只在装了 Create 时被实例化（工厂见 {@code registry/ModBlockEntities} 与 {@code compat/CreateTrophy}）。
 * 必须继承 Create 的 {@code KineticBlockEntity} 才能进旋转网络 —— 网络传播只认这个类
 * （javap 实证：{@code RotationPropagator} 的邻居查找签名全是它）。</p>
 *
 * <p>时长口径：<b>满速（256 RPM，Create 的上限）下 30 分钟</b>，慢转速最多翻倍。
 * 即每刻灌 {@code charge.suTarget / 36000}（默认 360000 / 36000 = 10），转速低于一半时按一半算。
 * 时间 = suTarget ÷ 每刻的量，想调时长改 config 那一个值就行。</p>
 */
public class TrophyKineticBlockEntity extends KineticBlockEntity implements Nameable, TrophyHolder {

    /** 满速基准：Create 的转速上限 */
    private static final double FULL_SPEED_RPM = 256.0D;
    /** 满速时每刻灌的量：suTarget(360000) ÷ 36000 刻 = 10 */
    private static final int UNITS_PER_TICK = 10;
    /** 慢转速的折算下限：低到 128 RPM 以下就按一半速率，时间是满速的两倍 */
    private static final double MIN_FACTOR = 0.5D;

    private final TrophyState state;

    public TrophyKineticBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.TROPHY.get(), pos, blockState);
        this.state = new TrophyState(this, blockState);
        this.state.setCustomName(TrophyBlockEntity.stageName(this.state.type()));
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide() || !state.isChargeable()) {
            return;
        }
        double speed = Math.abs(getSpeed());
        if (speed < 1.0D) {
            return;
        }
        double factor = Mth.clamp(speed / FULL_SPEED_RPM, MIN_FACTOR, 1.0D);
        int perTick = Math.max(1, (int) Math.round(UNITS_PER_TICK * factor));
        state.addCharge(TrophyCharge.SU, perTick);
        if (state.isChargeComplete()) {
            state.advance();
        }
    }

    @Override
    public TrophyState trophyState() {
        return state;
    }

    // ---------------- 名字（Nameable） ----------------

    @Override
    public Component getName() {
        return state.getName();
    }

    @Override
    public boolean hasCustomName() {
        return state.hasCustomName();
    }

    @Override
    public Component getCustomName() {
        return state.getCustomName();
    }

    // ---------------- 存档 ----------------
    // Create 把 load/saveAdditional 标成 final 了（SmartBlockEntity.m_142466_/m_183515_），
    // 覆写会直接 IncompatibleClassChangeError。它的正规钩子是 write/read。

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        state.save(tag);
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        state.load(tag);
    }
}
