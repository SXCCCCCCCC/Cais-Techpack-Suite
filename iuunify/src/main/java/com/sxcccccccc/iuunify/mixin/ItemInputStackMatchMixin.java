package com.sxcccccccc.iuunify.mixin;

import com.denfop.recipe.IInputItemStack;
import com.denfop.recipe.InputItemStack;
import com.sxcccccccc.iuunify.compat.ItemInputRegistry;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 阶段 0.4.0：物品输入 tag 化的配方匹配侧（咽喉点）。
 *
 * <p>目标方法（3.4.0.10 正式 jar javap 实证，dev 树同签名）：
 * {@code com.denfop.recipe.InputItemStack.matches(net.minecraft.world.item.ItemStack)}
 * ，描述符 {@code (Lnet/minecraft/world/item/ItemStack;)Z}。该方法是 IU 全部「精确
 * ItemStack 输入」配方谓词：{@code RecipesCore.getRecipeMachineRecipeOutput}
 * （api/recipe/RecipesCore.java:1281，require 分支 :1327
 * {@code recipeInputList.get(i).matches(stacks.get(i))}，非 require 分支 :1300 同款）
 * 以及 getRecipeOutput/getRecipeMultiOutput 等全部经
 * {@code IInputItemStack.matches} 接口分派。原始实现
 * {@code subject.getItem() == this.input.getItem() && ModUtils.checkItemEquality(...)}
 * （InputItemStack.java:59-62）——精确 id + NBT 裸比对。
 *
 * <p>本 mixin HEAD 拦截：规则命中（本谓词的基准输入 item = 登记的旧物品，subject
 * 属于统一 tag）即放行；否则回落原实现。覆盖效果（配合
 * {@link RecipeInputStackMatchedMixin} 与
 * {@link PrimalSiliconCrystalHandlerActivateMixin}）：两台晶体机（primal 右键 +
 * basemachine3 GUI，共用 "silicon_recipe"）的「放得进」与「炼得出」（process →
 * getOutputFor → matches 全链）一并修通，不会出现"放得进炼不出"。
 *
 * <p>mixin 铁律：handler 参数 = [目标方法参数] + [CallbackInfo]（this 经 Object
 * 双强转上转 IInputItemStack 传给 helper——目标字段 input 虽是 public，
 * com.denfop.recipe 包名无关，按 0.3.3 排雷结论一律经公开 getInputs() 读取）；
 * require=1；remap=false；handler 体内原版调用由 reobfJar 自动重映射。
 */
@Mixin(value = InputItemStack.class, remap = false)
public abstract class ItemInputStackMatchMixin {

    @Inject(
            method = "matches(Lnet/minecraft/world/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void iuunify$tagWiden(ItemStack subject, CallbackInfoReturnable<Boolean> cir) {
        IInputItemStack input = (IInputItemStack) (Object) this;
        if (ItemInputRegistry.widen(input, subject)) {
            cir.setReturnValue(true);
        }
    }
}
