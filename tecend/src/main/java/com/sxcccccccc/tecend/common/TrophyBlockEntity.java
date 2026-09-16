package com.sxcccccccc.tecend.common;

import com.sxcccccccc.tecend.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 奖杯方块实体：阶段 + 充能进度 + 阶段名。
 *
 * <p><b>阶段是倒着数的</b>：blockstate 属性的默认值永远是它的第一个取值（0），而"没有 NBT 的奖杯"
 * （直接 /give、旧存档里已放的方块）必须显示<b>原版金奖杯</b>——所以 0 必须是完成态，
 * 远古态放 4。进度 = age 递减。
 * </p>
 * <pre>
 *   age=0  金奖杯（完成；＜默认值，无 NBT 就是它）   原版外观与原版名
 *   age=1  未充能的奖杯                              原版外观 + 物品形态附魔光效
 *   age=2  粗胚奖杯（未喷砂）                        我方材质 trophy_rough_trimmed
 *   age=3  粗胚奖杯（未处理）                        我方材质 trophy_rough_cast
 *   age=4  远古奖杯                                  我方材质 trophy_ancient
 * </pre>
 *
 * <p>age 0 与 1 外观相同（原版外观）—— 区别在物品形态的附魔光效；方块形态没法发光效。</p>
 *
 * <p><b>名字</b>：物品 id 只有一个（{@code proof_of_honor:championplatform}），lang 键也只有一个，
 * 五个阶段靠 lang 分不开 —— 名字走 BE 的自定义名（{@code CustomName}），掉落时由战利品表的
 * {@code minecraft:copy_name}（source = block_entity）抄进物品（实测其实现：判定
 * {@code Nameable.hasCustomName()} → 取 {@code getCustomName()} → 调 {@code ItemStack.setHoverName()}）。
 * 名字是可翻译组件，跟随客户端语言。</p>
 *
 * <p>{@code BlockEntity} 本身不带名字 API（原版只有部分 BE 实现 {@link Nameable}），所以这里自己实现。</p>
 */
public class TrophyBlockEntity extends BlockEntity implements Nameable {
    /** 阶段属性（原版共享 AGE_5：0..5 六档，默认值 0 = 完成态） */
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;

    /** 物品 NBT 里携带阶段/充能进度的键（战利品表 copy_nbt 抄的就是它俩） */
    public static final String TAG_TYPE = "type";
    public static final String TAG_CHARGE = "charge";
    /** 自定义名键（原版约定，容器类 BE 同名） */
    public static final String TAG_CUSTOM_NAME = "CustomName";

    /** 阶段编号：0 = 完成（默认）… 5 = 未雕琢；进度按 age 递减 */
    public static final int STAGE_FINISHED = 0;
    public static final int STAGE_UNCHARGED = 1;
    public static final int STAGE_ROUGH_TRIMMED = 2;
    public static final int STAGE_ROUGH_CAST = 3;
    public static final int STAGE_ANCIENT = 4;
    /** 雕琢之前的远古奖杯（第 ⑥ 步的输入态） */
    public static final int STAGE_UNCARVED = 5;
    /** 阶段上界：夹取与创造栏遍历都用它，加档只改这里 */
    public static final int STAGE_MAX = STAGE_UNCARVED;

    /** 名称键按阶段索引；阶段 0（完成）为 null = 不给自定义名，保留原版物品名 */
    private static final String[] STAGE_NAME_KEYS = {
            null,
            "tecend.trophy.uncharged",
            "tecend.trophy.rough_trimmed",
            "tecend.trophy.rough_cast",
            "tecend.trophy.ancient",
            "tecend.trophy.uncarved",
    };

    private int type = STAGE_FINISHED;
    private long charge;
    private Component customName;

    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY.get(), pos, state);
        // 放下时 age 已由 getStateForPlacement 按物品 NBT 设好，这里直接读状态
        this.type = state.hasProperty(AGE) ? state.getValue(AGE) : STAGE_FINISHED;
        setCustomName(stageName(this.type));
    }

    /**
     * 阶段对应的名字（可翻译组件）；阶段 0（完成）返回 null = 保留原版物品名。
     * 战利品表 copy_name 抄的就是它。
     */
    public static Component stageName(int stage) {
        return stage >= STAGE_FINISHED && stage < STAGE_NAME_KEYS.length && STAGE_NAME_KEYS[stage] != null
                ? Component.translatable(STAGE_NAME_KEYS[stage])
                : null;
    }

    /**
     * 当前阶段（0 = 完成 … 4 = 远古）。
     *
     * <p>别叫 {@code getType()} —— {@code BlockEntity} 已有 {@code getType()}（返回 {@code BlockEntityType<?>}），
     * 同名同参不同返回类型会直接编译失败。</p>
     */
    public int getStage() {
        return type;
    }

    public long getCharge() {
        return charge;
    }

    public void addCharge(long amount) {
        charge += amount;
        setChanged();
    }

    /** 方块形式推进一步（age 递减，往完成走）。到顶（已完成）返回 false。 */
    public boolean advance() {
        if (type <= STAGE_FINISHED) {
            return false;
        }
        setStage(type - 1);
        charge = 0;
        return true;
    }

    public void setStage(int stage) {
        int clamped = Math.max(STAGE_FINISHED, Math.min(stage, STAGE_MAX));
        if (clamped == type) {
            return;
        }
        type = clamped;
        setCustomName(stageName(type));
        setChanged();
        // 把阶段写回方块状态：blockstate 才会渲染成对应的模型
        if (level != null && !level.isClientSide() && getBlockState().hasProperty(AGE)
                && getBlockState().getValue(AGE) != type) {
            level.setBlock(worldPosition, getBlockState().setValue(AGE, type), Block.UPDATE_ALL);
        }
    }

    // ---------------- 名字（Nameable） ----------------

    public void setCustomName(Component name) {
        this.customName = name;
        setChanged();
    }

    @Override
    public Component getName() {
        return customName != null ? customName : getBlockState().getBlock().getName();
    }

    @Override
    public boolean hasCustomName() {
        return customName != null;
    }

    @Override
    public Component getCustomName() {
        return customName;
    }

    // ---------------- 存档 ----------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt(TAG_TYPE, type);
        tag.putLong(TAG_CHARGE, charge);
        if (customName != null) {
            tag.putString(TAG_CUSTOM_NAME, Component.Serializer.toJson(customName));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains(TAG_TYPE)) {
            type = Math.max(STAGE_FINISHED, Math.min(tag.getInt(TAG_TYPE), STAGE_MAX));
        }
        charge = tag.getLong(TAG_CHARGE);
        if (tag.contains(TAG_CUSTOM_NAME, 8)) {
            customName = Component.Serializer.fromJson(tag.getString(TAG_CUSTOM_NAME));
        }
        // 名字只补空的：玩家自己改过名（铁砧）就保留玩家的
        if (!hasCustomName()) {
            setCustomName(stageName(type));
        }
    }
}
