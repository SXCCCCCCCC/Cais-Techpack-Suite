package com.sxcccccccc.iuunify.mixin;

import com.denfop.api.recipe.InventoryRecipes;
import com.denfop.blockentity.mechanism.BlockEntityPrimalSiliconCrystalHandler;
import com.sxcccccccc.iuunify.compat.ItemInputRegistry;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阶段 0.4.0：原始晶体生长室（primal_silicon_crystal_handler）右键入口的
 * 统一物品守卫。
 *
 * <p>目标方法（3.4.0.10 正式 jar javap 字节码实证，与 dev 树逐段一致）：
 * {@code com.denfop.blockentity.mechanism.BlockEntityPrimalSiliconCrystalHandler.onActivated}
 * ，描述符
 * {@code (Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/Vec3;)Z}。
 * 原始实现第 120 行：
 * {@code stack.getItem() instanceof ItemDust<?> && IUItem.iudust.getMeta((ItemDust) stack.getItem()) == 60}
 * ——只收「IU 自家 ItemDust、variant 60」；跨 mod 同族物品（gtceu:silicon_dust，
 * instanceof 非 ItemDust）被直接放空返回 false。
 *
 * <p>本 mixin 为「纯入口守卫」：手持物品命中统一 tag（{@code ItemInputRegistry#SILICON}
 * = forge:dusts/silicon，成员 gtceu:silicon_dust + industrialupgrade:itemdust/silicon_dust）
 * 时，按原始方法第 121-143 行的语义复刻装入 inputSlotA[1]（数量封顶 3、从手持
 * shrink、服务端 getOutput()），然后取消原方法（等价「接受了，没进 else」）。
 * <b>既不替换物品身份、也不制造新物品</b>：槽内物品保持原始 Stack（GT 粉放 GT 粉、
 * IU 粉放 IU 粉），后续配方匹配由 {@link ItemInputStackMatchMixin} 与
 * {@link RecipeInputStackMatchedMixin} 按统一 tag 放行。
 *
 * <p>边界保持（与原方法逐一对齐）：
 * <ul>
 *   <li>outputSlot 非空 → 不拦截，走原方法「取出产物」分支（原 163-172 行）；</li>
 *   <li>手持为空/非统一物品（flint、工具等）→ 不拦截，原方法分支照旧；</li>
 *   <li>inputSlotA[1] 被非同一物品占用（如槽内 IU 粉 vs 手持 GT 粉）→ 不拦截，
 *       原方法该分支同样落空返回 false（本守卫不改写他人身份，不私换槽内物品）；</li>
 *   <li>原方法未处理客户端镜像问题（shrink 服务端同步），本守卫与其一致。</li>
 * </ul>
 *
 * <p>字段可见性（3.4.0.10 javap 实证，无需 @Shadow/accessor，直接经 public 成员
 * 访问）：{@code inputSlotA} 为目标类 {@code public final InventoryRecipes}；
 * {@code outputSlot} 声明在父类
 * {@code com.denfop.blockentity.base.BlockEntityElectricMachine}，{@code public
 * InventoryOutput}；{@code getOutput()} 为目标类 public 方法。
 *
 * <p>mixin 铁律：handler 参数 = [目标方法参数] + [CallbackInfo]（this 经 Object
 * 双强转）；require=1；remap=false；handler 体内 vanilla 调用（getLevel/isClientSide）
 * 写 mojmap 名，由 reobfJar 重映射为运行时 SRG。
 *
 * <p>目标类/方法均为 Forge mod 成员，mod 自身方法名 dev/prod 一致（发布 jar
 * javap：onActivated 为 mojmap 名）——method/@Mixin 字符串按运行时真名写。
 */
@Mixin(value = BlockEntityPrimalSiliconCrystalHandler.class, remap = false)
public abstract class PrimalSiliconCrystalHandlerActivateMixin {

    @Inject(
            method = "onActivated(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/core/Direction;Lnet/minecraft/world/phys/Vec3;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void iuunify$unifiedDust(
            Player player, InteractionHand hand, Direction side, Vec3 hit,
            CallbackInfoReturnable<Boolean> cir
    ) {
        BlockEntityPrimalSiliconCrystalHandler machine = (BlockEntityPrimalSiliconCrystalHandler) (Object) this;
        ItemStack stack = player.getItemInHand(hand);
        // 原方法：空手或输出槽非空走「取出产物」分支（163-172 行），不抢。
        if (stack.isEmpty() || !ItemInputRegistry.isUnified(stack) || !machine.outputSlot.isEmpty()) {
            return;
        }
        InventoryRecipes inputSlot = machine.inputSlotA;
        ItemStack slot = inputSlot.get(1);
        if (slot.isEmpty()) {
            ItemStack put = stack.copy();
            if (put.getCount() > 3) {
                put.setCount(3);
                stack.shrink(3);
            } else {
                stack.shrink(put.getCount());
            }
            inputSlot.set(1, put);
        } else if (slot.is(stack.getItem())) {
            int minCount = Math.min(stack.getCount(), 3 - slot.getCount());
            slot.grow(minCount);
            stack.grow(-minCount);
        } else {
            // 槽 1 被异物品占用：守卫不改写槽内物品，放行原方法（原方法同样落空为 false）。
            return;
        }
        if (!((BlockEntity) (Object) this).getLevel().isClientSide) {
            machine.getOutput();
        }
        cir.setReturnValue(true);
    }
}
