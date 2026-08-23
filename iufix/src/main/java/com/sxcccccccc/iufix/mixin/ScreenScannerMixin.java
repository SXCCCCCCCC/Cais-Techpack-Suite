package com.sxcccccccc.iufix.mixin;

import com.denfop.api.recipe.IPatternStorage;
import com.denfop.blockentity.base.BlockEntityScanner;
import com.denfop.containermenu.ContainerMenuScanner;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(targets = "com.denfop.screen.ScreenScanner", remap = false)
public abstract class ScreenScannerMixin {

    private static final int GRID_START_X = 25;
    private static final int GRID_START_Y = 25;
    private static final int CELL_SIZE = 18;
    private static final int GRID_COLS = 8;

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (button != 1) {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (!(screen.getMenu() instanceof ContainerMenuScanner)) {
            return;
        }
        ContainerMenuScanner menu = (ContainerMenuScanner) screen.getMenu();
        if (menu.base == null) {
            return;
        }
        if (!(menu.base instanceof BlockEntityScanner)) {
            return;
        }
        BlockEntityScanner scanner = (BlockEntityScanner) menu.base;
        BlockEntityScannerAccessor accessor = (BlockEntityScannerAccessor) scanner;
        Map<BlockPos, IPatternStorage> patternMap = accessor.getPatternStorageMap();
        if (patternMap == null || patternMap.isEmpty()) {
            return;
        }
        int relX = mouseX - screen.getGuiLeft();
        int relY = mouseY - screen.getGuiTop();
        int col = (relX - GRID_START_X) / CELL_SIZE;
        int row = (relY - GRID_START_Y) / CELL_SIZE;
        if (col < 0 || row < 0 || col >= GRID_COLS) {
            return;
        }
        int index = row * GRID_COLS + col;
        if (index >= patternMap.size()) {
            return;
        }
        ci.cancel();
    }
}
