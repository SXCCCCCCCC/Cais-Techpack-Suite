package com.sxcccccccc.iuunify.mixin;

import com.denfop.api.recipe.RecipeInputStack;
import com.denfop.recipe.IInputItemStack;
import com.sxcccccccc.iuunify.compat.ItemInputRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阶段 0.4.0：物品输入 tag 化的槽位接受侧（咽喉点）。
 *
 * <p>目标方法（3.4.0.10 正式 jar javap 实证，dev 树同签名）：
 * {@code com.denfop.api.recipe.RecipeInputStack.matched(net.minecraft.world.item.ItemStack)}
 * ，描述符 {@code (Lnet/minecraft/world/item/ItemStack;)Z}。该方法是 IU 机器输入
 * 槽的所有「槽位接受判定」汇聚点：
 * {@code InventoryRecipes.canPlaceItem}（api/recipe/InventoryRecipes.java:168-188，
 * require 分支 :186 {@code list.contains(itemStack)}）→
 * {@code RecipeArrayList.contains(ItemStack)}（RecipeArrayList.java:16-21）→ 本方法；
 * GUI 放置（ContainerMenuBase.java:47 {@code slot.mayPlace(stack)}）与 shift 转移
 * 全部经此。原始实现逐项 {@code input.getItem() == stack.getItem()}（RecipeInputStack.java:33-44）
 * ——精确 id 裸比对，统一后的跨 mod 同族物品（GT 硅粉 × IU 硅粉）互不相认。
 *
 * <p>本 mixin HEAD 拦截：规则命中（配方基准输入 = 登记的旧物品，subject 属于统一
 * tag）即放行；否则回落原实现——未登记规则的输入逐位不变（零加宽方向：只加不减，
 * 与原接受域是「tag 或原判定」并集，语义与
 * {@code InternalFluidTankAcceptMixin} 的流体罐接受放宽同构）。
 *
 * <p>mixin 铁律：handler 参数 = [目标方法参数] + [CallbackInfo]（this 不占参数位，
 * 经 getInput() 公开访问器传给 helper——目标枚举字段 input 是 private，避开
 * @Shadow/accessor）；require=1 防静默失效；remap=false 按运行时真名（IU 自身方法
 * 名 dev/prod 一致，字节码 javap 实证）；handler 体内原版调用由 reobfJar 自动
 * 重映射为运行时 SRG 名。
 */
@Mixin(value = RecipeInputStack.class, remap = false)
public abstract class RecipeInputStackMatchedMixin {

    @Inject(
            method = "matched(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void iuunify$tagWiden(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        IInputItemStack input = ((RecipeInputStack) (Object) this).getInput();
        if (ItemInputRegistry.widen(input, stack)) {
            cir.setReturnValue(true);
        }
    }
}
