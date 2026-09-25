package com.sxcccccccc.iufix.mixin;

import com.denfop.blockentity.base.BlockEntityInventory;
import com.denfop.blockentity.mechanism.BlockEntityProbeAssembler;
import com.denfop.blockentity.mechanism.BlockEntityRocketAssembler;
import com.denfop.blockentity.mechanism.BlockEntityRoverAssembler;
import com.denfop.blockentity.mechanism.BlockEntitySatelliteAssembler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 用户裁决（2026-09-03）：探测器/火箭/卫星/漫游车四台组装机的<b>自动化插入路径</b>
 * 每格限 1——AE2/漏斗推入时物品按格散开（一格一个），而不是整堆怼进第一个可放格。
 *
 * <p><b>根因（研究 agent a594e5ade42d70ede 全链核实）：</b>四台配方
 * {@code require=true} 索引严格：槽 i 必须放第 i 个输入且全部槽非空才出产
 * （InventoryRecipes.process 223-229 + RecipesCore require 分支 1325-1352）。
 * 机器本就有"一格一个"设计：{@code InventoryRecipes.getStackSizeLimit()=1}，
 * 但它只对 GUI 生效（SlotInvSlot.getMaxStackSize 71-73 读它）。自动化路径走
 * Forge 能力：getCapability 的 ITEM_HANDLER 分支包 SidedInvWrapper，其
 * getSlotLimit lambda（反汇编 {@code lambda$new$2}）调用的是
 * {@code Container.getMaxStackSize()}——IU 侧即本类 getMaxStackSize（479）
 * → getInventoryStackLimit（483）硬编码 64，InventoryRecipes 的 1 永远不会被问。
 * 于是 AE2 一次 push 64 个全进第一个可放格（其余槽全空）→ 配方不匹配 →
 * 不出产，即"格子不会自动分配"。
 *
 * <p><b>做法（最小面：单方法单注入点，一个 mixin 覆盖四台）：</b>HEAD 拦截
 * {@code getMaxStackSize()}，四台组装机之一时返回 1。SidedInvWrapper 每槽 limit
 * 即 1：insertItem(slot, 64) 进 1 退 63，AE2 的 adaptor 逐槽探测继续放——
 * 物品按 canPlaceItem 允许集（SidedInvWrapper.isItemValid → InventoryRecipes
 * 配方允许集）散入正确索引的格子，一格一个，与配方语义吻合。
 *
 * <p>为什么注入本类而不是四台各一个：四台均未覆写 getMaxStackSize/
 * getInventoryStackLimit（grep 实证），公共入口只有这一个；instanceof 过滤
 * 与 BlockStateBaseFacingRotateMixin 同款已实证模式，且只动四台、其他机器
 * 原样 64（macerator 等动态 limit 机器不受影响）。
 *
 * <p>副作用核查（全部安全）：
 * <ul>
 *   <li>输出格同步限 1：机器产出走 Inventory.add（忽略 limit）照常堆满，
 *       抽取端每次 1 个、循环取完——慢一点但不会卡；</li>
 *   <li>机器内部拉取（InventoryUpgrade.tickPullIn → Inventory.add）不看
 *       getMaxStackSize，行为不变；</li>
 *   <li>GUI 本来就限 1（SlotInvSlot），玩家侧无感；</li>
 *   <li>配方全部单槽数量=1（四台 addRecipe 全 {@code new ItemStack(item, 1)}
 *       已逐条核实），限 1 不会饿死任何合成。</li>
 * </ul>
 *
 * <p>取证：@Inject require=1（0 命中直接应用失败，不被 mixins.json 的
 * injectors.defaultRequire=0 静默吞掉——1.9.0 正是这样炸在启动期的：
 * InvalidInjectionException，crash-2026-09-03_19.00.40-fml.txt）。
 * 注入串用运行时 SRG 名 {@code m_6893_}（= getMaxStackSize）：本方法是
 * vanilla {@code WorldlyContainer} 接口的覆写成员，IU 编译产物里为 SRG 名
 * （运行时 jar javap 实证：{@code public int m_6893_()}，字节码
 * {@code aload_0; invokevirtual getInventoryStackLimit; ireturn}），
 * 与源码 3.4.0.1 的 mojmap 名不同——denfop 自有方法才用官方名。
 */
@Mixin(value = BlockEntityInventory.class, remap = false)
public abstract class AssemblerSlotLimitOneMixin {

    @Inject(method = "m_6893_", at = @At("HEAD"), cancellable = true, require = 1)
    private void iufix$assemblerSlotLimitOne(CallbackInfoReturnable<Integer> cir) {
        BlockEntityInventory inv = (BlockEntityInventory) (Object) this;
        if (inv instanceof BlockEntityProbeAssembler
                || inv instanceof BlockEntityRocketAssembler
                || inv instanceof BlockEntitySatelliteAssembler
                || inv instanceof BlockEntityRoverAssembler) {
            cir.setReturnValue(1);
        }
    }
}
