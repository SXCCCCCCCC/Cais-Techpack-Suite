package com.sxcccccccc.tecend.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 奖杯的共享状态与行为：阶段（age）、名字、六个充能通道。
 *
 * <p>抽出来是因为方块形态有两个 BE —— 普通 BE 与 Create 动力变体
 * （{@code KineticBlockEntity}）。Java 单继承，两个类没法共用父类，只能共用这个对象。</p>
 */
public final class TrophyState {

    private final BlockEntity blockEntity;
    /** 六个充能通道的原始值。物品放下时随 BlockEntityTag 进来，挖走时由战利品表抄回物品 */
    private final CompoundTag chargeTag = new CompoundTag();
    private int type = TrophyBlockEntity.STAGE_FINISHED;
    private Component customName;

    public TrophyState(BlockEntity blockEntity, BlockState state) {
        this.blockEntity = blockEntity;
        this.type = state.hasProperty(TrophyBlockEntity.AGE)
                ? state.getValue(TrophyBlockEntity.AGE)
                : TrophyBlockEntity.STAGE_FINISHED;
        setCustomName(TrophyBlockEntity.stageName(this.type));
    }

    public int type() {
        return type;
    }

    /** 只有「金奖杯（未充能）」那一档能充。 */
    public boolean isChargeable() {
        return type == TrophyBlockEntity.STAGE_UNCHARGED;
    }

    public void setType(int stage) {
        int clamped = Math.max(TrophyBlockEntity.STAGE_FINISHED, Math.min(stage, TrophyBlockEntity.STAGE_MAX));
        if (clamped == type) {
            return;
        }
        type = clamped;
        setCustomName(TrophyBlockEntity.stageName(type));
        blockEntity.setChanged();
        syncAgeToBlockState();
    }

    /** 把阶段写回方块状态：blockstate 才会渲染成对应的模型。 */
    private void syncAgeToBlockState() {
        Level level = blockEntity.getLevel();
        BlockState state = blockEntity.getBlockState();
        if (level != null && !level.isClientSide() && state.hasProperty(TrophyBlockEntity.AGE)
                && state.getValue(TrophyBlockEntity.AGE) != type) {
            level.setBlock(blockEntity.getBlockPos(), state.setValue(TrophyBlockEntity.AGE, type), Block.UPDATE_ALL);
        }
    }

    /** 推进一步（age 递减，往完成走）。到顶（已完成）返回 false。 */
    public boolean advance() {
        if (type <= TrophyBlockEntity.STAGE_FINISHED) {
            return false;
        }
        setType(type - 1);
        for (String route : TrophyCharge.routes()) {
            chargeTag.putLong(route, 0L);
        }
        blockEntity.setChanged();
        return true;
    }

    // ---------------- 充能 ----------------

    public long getCharge(String route) {
        return TrophyCharge.get(chargeTag, route);
    }

    /** 往某通道灌进度；返回是否真的记入了。 */
    public boolean addCharge(String route, long amount) {
        if (TrophyCharge.add(chargeTag, route, amount) <= 0L) {
            return false;
        }
        blockEntity.setChanged();
        return true;
    }

    /** 整套条是否已满 */
    public boolean isChargeComplete() {
        return TrophyCharge.fraction(chargeTag) >= 1.0D;
    }

    // ---------------- 名字 ----------------

    public Component getName() {
        return customName != null ? customName : blockEntity.getBlockState().getBlock().getName();
    }

    public boolean hasCustomName() {
        return customName != null;
    }

    public Component getCustomName() {
        return customName;
    }

    public void setCustomName(Component name) {
        this.customName = name;
        blockEntity.setChanged();
    }

    // ---------------- 存档 ----------------

    public void save(CompoundTag tag) {
        tag.putInt(TrophyBlockEntity.TAG_TYPE, type);
        TrophyCharge.copyInto(chargeTag, tag);
        if (customName != null) {
            tag.putString(TrophyBlockEntity.TAG_CUSTOM_NAME, Component.Serializer.toJson(customName));
        }
    }

    public void load(CompoundTag tag) {
        if (tag.contains(TrophyBlockEntity.TAG_TYPE)) {
            type = Math.max(TrophyBlockEntity.STAGE_FINISHED,
                    Math.min(tag.getInt(TrophyBlockEntity.TAG_TYPE), TrophyBlockEntity.STAGE_MAX));
        }
        TrophyCharge.copyInto(tag, chargeTag);
        if (tag.contains(TrophyBlockEntity.TAG_CUSTOM_NAME, 8)) {
            customName = Component.Serializer.fromJson(tag.getString(TrophyBlockEntity.TAG_CUSTOM_NAME));
        }
        // 名字只补空的：玩家自己改过名（铁砧）就保留玩家的
        if (!hasCustomName()) {
            setCustomName(TrophyBlockEntity.stageName(type));
        }
    }
}
