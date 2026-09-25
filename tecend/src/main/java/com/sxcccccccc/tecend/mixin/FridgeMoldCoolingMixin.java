package com.sxcccccccc.tecend.mixin;

import com.sxcccccccc.tecend.registry.ModBlocks;
import net.blay09.mods.cookingforblockheads.tile.FridgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 冰箱冷却（第 ⑪ 步的「懒人厨房」路线）：含奖杯的浇筑模具放进冰箱 10 秒 → 冷却的模具。
 *
 * <p><b>为什么这里只能在冰箱自己的 tick 里顺手看，用不了计划刻</b>：计划刻是给<b>方块</b>的 ——
 * {@code Level#scheduleTick} 到点回调的是那个方块自己的 {@code tick}。而这里要计时的是
 * 一件<b>躺在容器里的物品</b>：原版 {@code Container} 不 tick 物品，{@code Item#inventoryTick}
 * 只服务实体背包（{@code Player#tick} 遍历自己的 inventory 时才调），MC 里没有"物品进入容器"
 * 的钩子。所以在<b>容器自己的 tick</b> 里顺带看一眼是唯一不引入额外调度的做法 ——
 * 冰箱 BE 本来就每刻在跑，我们搭它的车，没有多出任何一次调度。</p>
 *
 * <p><b>为什么注入实例重载</b>：javap 实证 {@code FridgeBlockEntity} 有两个 {@code serverTick}
 * —— 静态的 {@code serverTick(Level, BlockPos, BlockState, FridgeBlockEntity)}（Forge 的 ticker
 * 形式，方法体只有一句：把四个参数拆开转调实例重载）和实例的
 * {@code serverTick(Level, BlockPos, BlockState)}。注入<b>实例</b>那个：静态 ticker 对每个冰箱
 * 方块各调一次、各自转到对应实例，所以既不会漏也不会重。取容器用 {@code getContainer()}
 * （该方块自己那一格），不是 {@code getCombinedContainer()}（整台冰箱合并的）—— 后者会让
 * 同一件模具被相连的每个方块各处理一遍。</p>
 *
 * <p><b>计时写在物品 NBT 上</b>（{@code tecend:cooling}，累计 tick）：不占旁挂表，区块卸载也不丢。
 * 这么做是安全的，因为模具没有按 NBT 匹配的配方 —— ⑪ 的 GT 真空冷冻机走
 * {@code itemInputs('tecend:filled_casting_mold')}（裸 id，只看物品类型），AE2 熵变机械臂走
 * {@code {block: {id: ...}}}（方块状态）。奖杯就不能这么干，它的配方全走 {@code forge:nbt}。</p>
 *
 * <p>取出冰箱 = 计时停住（没 tick 喂它），放回去接着走 —— 与 BD 不稳定时空碎片
 * 「不在身上就不扣时间」同一个语义。</p>
 *
 * <p><b>注入用 {@code require = 0}</b>：本包不硬依赖 CFB（mods.toml 里是 optional）。
 * 万一 CFB 换了方法签名，宁可这条功能静默失效，也不要在启动阶段把整个游戏带崩。</p>
 */
@Mixin(value = FridgeBlockEntity.class, remap = false)
public abstract class FridgeMoldCoolingMixin {

    /** 冰箱冷却耗时：10 秒（设计定的「放进去即可」） */
    @Unique
    private static final int TECEND_COOL_TICKS = 10 * 20;

    /** 累计 tick 的 NBT 键 */
    @Unique
    private static final String TECEND_COOLING = "tecend:cooling";

    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
            at = @At("TAIL"), require = 0)
    private void tecend$coolMolds(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        Container container = ((FridgeBlockEntity) (Object) this).getContainer();
        if (container == null) {
            return;
        }
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty() || !stack.is(ModBlocks.FILLED_CASTING_MOLD_ITEM.get())) {
                continue;   // 不是模具（或者空槽）—— 无事发生，连 tag 都不碰
            }
            CompoundTag tag = stack.getOrCreateTag();
            int elapsed = tag.getInt(TECEND_COOLING) + 1;
            if (elapsed < TECEND_COOL_TICKS) {
                tag.putInt(TECEND_COOLING, elapsed);
                continue;
            }
            // 冷却完成：数量保留，换一个干净的冷却模具栈（计时键自然不再跟着）
            container.setItem(slot, new ItemStack(ModBlocks.COOLED_CASTING_MOLD_ITEM.get(), stack.getCount()));
        }
    }
}
