package com.sxcccccccc.iufix.mixin;

import com.denfop.containermenu.ContainerMenuVeinSensor;
import com.denfop.items.ItemStackVeinSensor;
import com.sxcccccccc.iufix.util.IufixPinIn;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.denfop.screen.ScreenVeinSensor", remap = false)
public abstract class ScreenVeinSensorMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 1) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!(screen.getMenu() instanceof ContainerMenuVeinSensor)) {
            return;
        }
        ContainerMenuVeinSensor menu = (ContainerMenuVeinSensor) screen.getMenu();
        if (!(menu.base instanceof ItemStackVeinSensor)) {
            return;
        }
        ItemStackVeinSensor sensor = (ItemStackVeinSensor) menu.base;
        if (sensor.getMap() == null || sensor.getMap().isEmpty()) {
            return;
        }
        if (sensor.getVector() == null) {
            return;
        }
        ci.cancel();
    }

    @Redirect(method = "drawForegroundLayer", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", remap = false))
    private boolean pinyin_fg(String itemName, String searchKey) {
        return pinyinAwareMatches(itemName, searchKey);
    }

    @Redirect(method = "drawGuiContainerBackgroundLayer", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", remap = false))
    private boolean pinyin_bg(String itemName, String searchKey) {
        return pinyinAwareMatches(itemName, searchKey);
    }

    @Redirect(method = "mouseClicked", at = @At(value = "INVOKE", target = "Ljava/lang/String;startsWith(Ljava/lang/String;)Z", ordinal = 1, remap = false))
    private boolean pinyin_click(String itemName, String searchKey) {
        return pinyinAwareMatches(itemName, searchKey);
    }

    private static boolean pinyinAwareMatches(String itemName, String searchKey) {
        if (itemName.contains(searchKey)) {
            return true;
        }
        String query = searchKey.startsWith("[") ? searchKey.substring(1) : searchKey;
        if (query.isEmpty()) {
            return false;
        }
        query = query.toLowerCase();
        if (IufixPinIn.PININ != null) {
            return IufixPinIn.PININ.contains(itemName, query);
        }
        return false;
    }
}
