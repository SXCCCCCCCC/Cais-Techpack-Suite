package com.sxcccccccc.iufix.mixin;

import com.denfop.api.container.CustomWorldContainer;
import com.denfop.blockentity.mechanism.BlockEntityCanner;
import com.denfop.containermenu.ContainerMenuBase;
import com.denfop.screen.ScreenIndustrialUpgrade;
import com.sxcccccccc.iufix.containermenu.ContainerMenuCanner;
import com.sxcccccccc.iufix.screen.ScreenCanner;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;

/**
 * 给装罐机（BlockEntityCanner）挂上 GUI：覆写父类 BlockEntityInventory 的两个钩子。
 *
 * <p>源码（BlockEntityInventory.java 290-313）中 getGuiContainer/getGui 都在父类返回 null
 * （BlockEntityCanner 继承了却没覆写），导致 generic 菜单链开路走到 null → 打开机器无 GUI。
 * 本 mixin 按父类 ERASED 签名（Player）ContainerMenuBase、与
 * (Player, ContainerMenuBase)ScreenIndustrialUpgrade 各覆写一次，descriptor 与父类一致，
 * 是 JVM 层面的真覆写：服务端 createMenu → getGuiContainer 建 ContainerMenuCanner；
 * 客户端 MenuScreens 回调 → getGui 建 ScreenCanner。
 *
 * <p>覆写方法由 mixin 0.8.x 的 overlay 机制合入目标类（签名匹配目标类父类方法即自然覆写，
 * 非 @Overwrite、也无须 @Inject，方法体整体替换）。
 */
@Mixin(value = BlockEntityCanner.class, remap = false)
public abstract class BlockEntityCannerGuiMixin {

    public ContainerMenuBase<?> getGuiContainer(Player var1) {
        return new ContainerMenuCanner(var1, (BlockEntityCanner) (Object) this);
    }

    @OnlyIn(Dist.CLIENT)
    public ScreenIndustrialUpgrade<ContainerMenuBase<? extends CustomWorldContainer>> getGui(
            Player var1, ContainerMenuBase<? extends CustomWorldContainer> menu) {
        return new ScreenCanner((ContainerMenuCanner) menu);
    }

}
