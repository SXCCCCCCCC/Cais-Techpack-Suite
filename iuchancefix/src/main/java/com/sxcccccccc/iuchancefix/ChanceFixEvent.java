package com.sxcccccccc.iuchancefix;

import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.RecipeOutput;
import com.sxcccccccc.iuchancefix.util.ChanceFixUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * 把"煤粉做合金"这份注册配方（Recipes.recipes.map_recipes["handlerho"] 里的
 * BaseMachineRecipe.output.metadata）的每槽 chance（"input"+i）改成 100。
 * <p>
 * 为什么需要这一半：运行时机器侧的 col 由 mixin 强置（那才是真正 100%），但
 * 一切"读配方数据"的消费者——JEI 的 HandlerHOCategory 直接从 metadata
 * CompoundTag.getInt("input"+i) 渲染百分比（javap 实证
 * {@code invokevirtual CompoundTag.m_128451_(...)} + makeConcatWithConstants），
 * 之后若有任何 AE2 型配方导入器也会读同一份元数据——都看到 100。
 * <p>
 * 时机：IU 在 {@code IUCore.getore(TagsUpdatedEvent)}（默认 NORMAL 优先级）里重跑
 * {@code Recipes.recipes.initializationRecipes()} 重建配方表；本监听器以 LOWEST
 * 优先级跟在其后重写元数据，同一次事件内即可完成。
 */
@Mod.EventBusSubscriber(modid = IuChanceFix.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ChanceFixEvent {

    private static final Logger LOGGER = LogManager.getLogger("iuchancefix");

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            return;
        }
        int fixed = 0;
        try {
            List<BaseMachineRecipe> recipes = Recipes.recipes.getRecipeList("handlerho");
            for (BaseMachineRecipe recipe : recipes) {
                if (!ChanceFixUtil.isCoalAlloyRecipe(recipe)) {
                    continue;
                }
                RecipeOutput out = recipe.output;
                CompoundTag nbt = out.metadata;
                for (int i = 0; i < out.items.size(); i++) {
                    nbt.putInt("input" + i, 100);
                }
                fixed++;
            }
        } catch (Throwable t) {
            LOGGER.error("iuchancefix metadata rewrite failed", t);
            return;
        }
        if (fixed > 0) {
            LOGGER.info("[iuchancefix] handlerho coal-dust-for-alloys recipe metadata chance forced to 100 ({} recipe(s))", fixed);
        }
    }

}
