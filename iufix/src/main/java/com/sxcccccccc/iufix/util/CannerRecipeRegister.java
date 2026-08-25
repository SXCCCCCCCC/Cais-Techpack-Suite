package com.sxcccccccc.iufix.util;

import com.denfop.IUItem;
import com.denfop.api.Recipes;
import com.denfop.api.recipe.BaseMachineRecipe;
import com.denfop.api.recipe.Input;
import com.denfop.api.recipe.RecipeOutput;
import com.denfop.recipe.IInputHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 装罐机（"cannerenrich"，BlockEntityCanner.inputSlotA）配方补全。
 *
 * <p>现状（源码实名）：RecipesCore.addRecipeManager("cannerenrich", 2, true, true) 只建了个
 * 空管理器，IUCore.getore 的初始化钩子里没有任何注册行；BlockEntityCanner 也没像
 * BlockEntityRodFactory 那样实现 init()（init() = Recipes.recipes.initializationRecipes()
 * 的每机器回调），所以装罐机永远没有配方，放了东西不会加工。1.7.10 的
 * (CannerRecipe.java) 的 10 条"燃料棒封装"配方在 1.20.1 整批丢失。
 *
 * <p>本类在 TagsUpdatedEvent（SERVER_DATA_LOAD，与 IUCore.getore 同一事件、LOWEST 优先级
 * 在其后执行）补注册全部 10 条：容器 = IC2 Refabricated 的空燃料棒（ic2_120:fuel_rod，
 * "Fuel Rod (Empty)"），材料 = IU 辐射资源 gem / 质子，产物 = IU 单棒
 * （reactorProton/Toriy/Americium/Neptunium/Curium/California/Mendelevium/Berkelium/
 * Einsteinium/Uran233 Simple）。IC2R 缺位（未装 IC2）时整体跳过、不崩；两条物料槽
 * （槽0=空棒、槽1=材料，一一对应，consume=true 后两者各耗 1，产物 1 根棒）。
 *
 * <p>注册时点与 IU 自家配方同轨（getore 之后、机器读配方前），见配方在机器 GUI/加工
 * 中即刻可用；JEI 展示取决于 IU 是否给 cannerenrich 注册了 JEI 类别（入库前确认没有）。
 */
public class CannerRecipeRegister {

    private boolean ran = false;

    @SubscribeEvent
    public void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() != TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD || ran) {
            return;
        }
        ran = true;
        final IInputHandler input = Recipes.inputFactory;
        final Item rodItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation("ic2_120", "fuel_rod"));
        if (rodItem == null || rodItem == Items.AIR) {
            return;
        }
        final ItemStack rod = new ItemStack(rodItem, 1);
        addCanning(input, rod, new ItemStack(IUItem.proton.getItem(), 1), IUItem.reactorprotonSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(4), 1), IUItem.reactortoriySimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(0), 1), IUItem.reactoramericiumSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(1), 1), IUItem.reactorneptuniumSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(2), 1), IUItem.reactorcuriumSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(3), 1), IUItem.reactorcaliforniaSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(5), 1), IUItem.reactormendeleviumSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(6), 1), IUItem.reactorberkeliumSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(7), 1), IUItem.reactoreinsteiniumSimple.getItemStack());
        addCanning(input, rod, new ItemStack(IUItem.radiationresources.getStack(8), 1), IUItem.reactoruran233Simple.getItemStack());
    }

    private void addCanning(IInputHandler input, ItemStack rod, ItemStack material, ItemStack output) {
        Recipes.recipes.addRecipe(
                "cannerenrich",
                new BaseMachineRecipe(
                        new Input(
                                input.getInput(rod), input.getInput(material)
                        ),
                        new RecipeOutput(null, output)
                )
        );
    }

}
