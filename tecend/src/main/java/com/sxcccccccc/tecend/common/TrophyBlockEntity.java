package com.sxcccccccc.tecend.common;

import com.sxcccccccc.tecend.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * 奖杯方块实体：阶段 + 充能 + 阶段名。实际状态在 {@link TrophyState} 里 —— 方块形态有两个 BE
 * （这个普通的，和装了 Create 时用的动力变体 {@code TrophyKineticBlockEntity}），
 * Java 单继承没法共用父类，只能共用那个状态对象。
 *
 * <p><b>阶段是倒着数的</b>：blockstate 属性的默认值永远是它的第一个取值（0），而"没有 NBT 的奖杯"
 * （直接 /give、旧存档里已放的方块）必须显示<b>原版金奖杯</b>——所以 0 必须是完成态。
 * 进度 = age 递减。</p>
 * <pre>
 *   age=0  金奖杯（完成；＜默认值，无 NBT 就是它）   原版外观，名字是「金奖杯」
 *   age=1  金奖杯（未充能）                          原版外观 + 物品形态附魔光效
 *   age=2  粗胚奖杯（未喷砂）                        我方材质 trophy_rough_trimmed
 *   age=3  粗胚奖杯（未处理）                        我方材质 trophy_rough_cast
 *   age=4  远古奖杯                                  我方材质 trophy_ancient
 *   age=5  远古奖杯（未雕琢）                        我方材质 trophy_uncarved
 *   age=6  粗胚奖杯（未剪水口）                      未喷砂底 + 铸造毛刺
 * </pre>
 *
 * <p><b>名字</b>：物品 id 只有一个（{@code proof_of_honor:championplatform}），lang 键也只有一个，
 * 七个阶段靠 lang 分不开 —— 名字走 BE 的自定义名（{@code CustomName}），掉落时由战利品表的
 * {@code minecraft:copy_name}（source = block_entity）抄进物品。名字是可翻译组件，跟随客户端语言。</p>
 */
public class TrophyBlockEntity extends BlockEntity implements Nameable, TrophyHolder {
    /**
     * 阶段属性：0..6 七档，默认值 0 = 完成态。
     *
     * <p>不用原版共享的 {@code AGE_*}：原版只有 AGE_1/2/3/4/5/7/15/25，没有 AGE_6，
     * 拿 AGE_7 会多一个用不到的取值。自定义属性的名字仍叫 {@code age}，blockstate JSON 的键不变。</p>
     */
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 6);

    /** 物品 NBT 里携带阶段/充能进度的键（战利品表 copy_nbt 抄的就是它们） */
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
    /** 还没有剪水口的奖杯（第 ⑬ 步之前） */
    public static final int STAGE_WITH_SPRUE = 6;
    /** 阶段上界：夹取与创造栏遍历都用它，加档只改这里 */
    public static final int STAGE_MAX = STAGE_WITH_SPRUE;

    /** 第 ⑭ 步「自然风化」：未喷砂的奖杯放地上满这么久就进到未充能（1 小时） */
    public static final int AGING_TICKS = 72_000;

    /** 名称键按阶段索引（阶段 0 = 完成态，也就是设计稿里的「金奖杯」） */
    private static final String[] STAGE_NAME_KEYS = {
            "tecend.trophy.finished",
            "tecend.trophy.uncharged",
            "tecend.trophy.rough_trimmed",
            "tecend.trophy.rough_cast",
            "tecend.trophy.ancient",
            "tecend.trophy.uncarved",
            "tecend.trophy.sprued",
    };

    private final TrophyState state;

    public TrophyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TROPHY.get(), pos, state);
        this.state = new TrophyState(this, state);
    }

    /**
     * 阶段对应的名字（可翻译组件）；战利品表 copy_name 抄的就是它。
     *
     * <p>每个阶段都有自定义名 —— 包括完成态：物品 id 只有一个、原版 lang 键也只有一个，
     * 完成态不挂名字的话就会显示 proof_of_honor 的原版名（「冠军奖杯」），跟设计稿的「金奖杯」对不上。</p>
     */
    public static Component stageName(int stage) {
        return stage >= STAGE_FINISHED && stage < STAGE_NAME_KEYS.length
                ? Component.translatable(STAGE_NAME_KEYS[stage])
                : null;
    }

    /** 共享状态，动力变体与外部（战利品表逻辑、充能）都通过它读写 */
    @Override
    public TrophyState trophyState() {
        return state;
    }

    /** 当前阶段（0 = 完成 … 6 = 未剪水口）。别叫 getType()，BlockEntity 已有同名的类型访问器。 */
    public int getStage() {
        return state.type();
    }

    public void setStage(int stage) {
        state.setType(stage);
    }

    /** 这套条是否已满 */
    public boolean isChargeComplete() {
        return state.isChargeComplete();
    }

    /** 方块形式推进一步（age 递减，往完成走）。到顶（已完成）返回 false。 */
    public boolean advance() {
        return state.advance();
    }

    // ---------------- 名字（Nameable） ----------------

    public void setCustomName(Component name) {
        state.setCustomName(name);
    }

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

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        state.save(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        state.load(tag);
    }
}
