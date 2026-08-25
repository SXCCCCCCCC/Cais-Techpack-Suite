package com.sxcccccccc.iufix.containermenu;

import com.denfop.blockentity.mechanism.BlockEntityCanner;
import com.denfop.containermenu.ContainerMenuFullInv;
import com.denfop.containermenu.SlotInvSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

/**
 * 装罐机（canner_iu / BlockEntityCanner）GUI 容器。
 *
 * <p>坐标按 textures/gui/guicanner.png 贴图逐像素测量（槽位 = 贴图格 art 左上角 + 1px）：
 * 机器槽 inputSlotA 0/1 = (42,18)/(119,18)（两条流体管之间的物品格），outputSlot = (80,45)，
 * 放电槽 = (8,77)；升级槽 4 个 = (152,23/41/59/77)。面板 176x180：玩家背包三行 y=98/116/134、
 * 快捷栏 y=155。Slot.x/y 在 1.20.1 是 final，改不了标准 addPlayerInventorySlots
 * 算出的快捷栏 156，所以走 (addSlots=false) 构造器自己手放玩家槽。
 */
public class ContainerMenuCanner extends ContainerMenuFullInv<BlockEntityCanner> {

    public ContainerMenuCanner(Player player, BlockEntityCanner base) {
        super(player, base, 178, 180, false);
        final int n4 = (178 - 162) / 2;
        for (int n3 = 0; n3 < 3; n3++) {
            for (int i = 0; i < 9; i++) {
                this.addSlot(new Slot(player.getInventory(), i + n3 * 9 + 9, n4 + i * 18, 98 + n3 * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(player.getInventory(), i, n4 + i * 18, 155));
        }
        this.addSlotToContainer(new SlotInvSlot(base.dischargeSlot, 0, 8, 77));
        this.addSlotToContainer(new SlotInvSlot(base.inputSlotA, 0, 42, 18));
        this.addSlotToContainer(new SlotInvSlot(base.inputSlotA, 1, 119, 18));
        this.addSlotToContainer(new SlotInvSlot(base.outputSlot, 0, 80, 45));
        for (int i = 0; i < 4; i++) {
            this.addSlotToContainer(new SlotInvSlot(base.upgradeSlot, i, 152, 23 + i * 18));
        }
    }

}
