package com.sxcccccccc.iufix.screen;

import com.denfop.Constants;
import com.denfop.api.widget.EnumTypeComponent;
import com.denfop.api.widget.ScreenWidget;
import com.denfop.api.widget.TankWidget;
import com.denfop.api.widget.WidgetDefault;
import com.denfop.componets.ComponentRenderInventory;
import com.denfop.componets.EnumTypeComponentSlot;
import com.denfop.inventory.Inventory;
import com.denfop.network.packet.PacketUpdateServerTile;
import com.denfop.screen.ScreenMain;
import com.sxcccccccc.iufix.containermenu.ContainerMenuCanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 装罐机 GUI（贴图 textures/gui/guicanner.png，面板 176x180）。
 *
 * <p>布局按贴图逐像素测量：背包三行 y=98/116/134、快捷栏 y=155（渲染层高 180）；机器槽位
 * 由容器（ContainerMenuCanner）摆放，渲染层把 inputSlotA/outputSlot/dischargeSlot 拉入
 * 黑名单（贴图已烘焙管/格图标，不再叠画通用槽位 art）；升级槽走通用 18x18 art 与贴图
 * (151,22) 起烘焙格重叠。两个 20x55 流体罐 (42,41)/(118,41)，能量条 (11,58)（贴图深色电池
 * 图标处），进度条 (77,22)（贴图顶部泵图标上）。点击顶部泵形图标换罐
 * （updateTileServer 事件 → switchTanks，原 1.7.10 装罐机的换罐行为）。
 */
@OnlyIn(Dist.CLIENT)
public class ScreenCanner<T extends ContainerMenuCanner> extends ScreenMain<ContainerMenuCanner> {

    public ScreenCanner(final ContainerMenuCanner container) {
        super(container);
        this.imageHeight = 180;
        componentList.clear();
        this.invSlotList.add(container.base.inputSlotA);
        inventory = new ScreenWidget(this, 7, 97, getComponent(),
                new WidgetDefault<>(new ComponentRenderInventory(EnumTypeComponentSlot.ALL))
        );
        // 三个参数：槽位类型、inventory 集（无需 26x26 适配 art，置空列表）、黑名单（不叠画机器槽）
        final List<Inventory> blackList = List.of(
                container.base.inputSlotA,
                container.base.outputSlot,
                container.base.dischargeSlot
        );
        this.slots = new ScreenWidget(this, 0, 0, getComponent(),
                new WidgetDefault<>(new ComponentRenderInventory(
                        EnumTypeComponentSlot.SLOTS_UPGRADE, this.invSlotList, blackList))
        );

        componentList.add(inventory);
        componentList.add(slots);
        this.addComponent(new ScreenWidget(this, 11, 58, EnumTypeComponent.ENERGY,
                new WidgetDefault<>(this.getContainer().base.energy)
        ));
        this.addComponent(new ScreenWidget(this, 77, 22, EnumTypeComponent.PROCESS,
                new WidgetDefault<>(this.getContainer().base.componentProgress)
        ));
    }

    @Override
    protected void mouseClicked(int i, int j, final int k) {
        super.mouseClicked(i, j, k);
        i -= this.guiLeft;
        j -= this.guiTop;
        // 顶部泵形图标 (73..96,21..41)：点击换罐（fluidTank <-> outputTank）
        if (i >= 73 && i <= 96 && j >= 21 && j <= 41) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            new PacketUpdateServerTile(this.container.base, 0);
        }
    }

    @Override
    protected void drawForegroundLayer(GuiGraphics poseStack, int mouseX, int mouseY) {
        super.drawForegroundLayer(poseStack, mouseX, mouseY);
        TankWidget.createNormal(this, 42, 41, this.container.base.fluidTank)
                .drawForeground(poseStack, mouseX, mouseY);
        TankWidget.createNormal(this, 118, 41, this.container.base.outputTank)
                .drawForeground(poseStack, mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(GuiGraphics poseStack, float partialTicks, int mouseX, int mouseY) {
        super.drawGuiContainerBackgroundLayer(poseStack, partialTicks, mouseX, mouseY);
        this.bindTexture();
        this.drawBackgroundAndTitle(poseStack, partialTicks, mouseX, mouseY);
        TankWidget.createNormal(this, 42, 41, this.container.base.fluidTank)
                .drawBackground(poseStack, this.guiLeft, this.guiTop);
        TankWidget.createNormal(this, 118, 41, this.container.base.outputTank)
                .drawBackground(poseStack, this.guiLeft, this.guiTop);
        for (ScreenWidget element : this.componentList) {
            element.drawBackground(poseStack, this.guiLeft, this.guiTop);
        }
    }

    @Override
    protected ResourceLocation getTexture() {
        return new ResourceLocation(Constants.MOD_ID, "textures/gui/guicanner.png");
    }

}
