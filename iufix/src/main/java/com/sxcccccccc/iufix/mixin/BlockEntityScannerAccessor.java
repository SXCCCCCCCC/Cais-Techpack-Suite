package com.sxcccccccc.iufix.mixin;

import com.denfop.api.recipe.IPatternStorage;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Map;

@Mixin(targets = "com.denfop.blockentity.base.BlockEntityScanner", remap = false)
public interface BlockEntityScannerAccessor {

    @Accessor(value = "iPatternStorageMap", remap = false)
    Map<BlockPos, IPatternStorage> getPatternStorageMap();

    @Accessor(value = "iPatternStorageList", remap = false)
    List<IPatternStorage> getPatternStorageList();
}
