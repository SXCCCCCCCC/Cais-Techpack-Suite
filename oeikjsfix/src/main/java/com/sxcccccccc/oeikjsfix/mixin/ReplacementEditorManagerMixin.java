package com.sxcccccccc.oeikjsfix.mixin;

import com.mafuyu404.oneenoughitem.client.gui.manager.ReplacementEditorManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;

/**
 * 抑制 {@code createPackMcmetaIfNeeded(Path)} 的写文件副作用。
 *
 * <p>PathUtilsMixin 把 {@code getDatapackPath} 重定向到 gamedir/kubejs 后，
 * manager 新建替换文件时的 dpRoot = gamedir/kubejs，这个本来用于世界数据包根
 * 的方法会在 {@code kubejs/pack.mcmeta} 写一个流浪的 pack.mcmeta
 * （{"pack": {"description": "The automatically generated OEI data pack",
 * "pack_format": 15}}）。KubeJS 的生成数据包自带 pack.mcmeta，kubejs/ 根上的
 * 这个文件无任何作用，只会污染 kubejs 目录。HEAD 注入直接取消写入。
 *
 * <p>无副作用依据（javap -c，ReplacementsEditorManager.createReplacementFile）：
 * {@code Files.createDirectories(dir)} 在调用本方法<b>之前</b>已完成，
 * 后面也没有任何代码依赖 pack.mcmeta 是否存在——只读文件不受影响。
 *
 * <p>目标为<b>实例私有方法</b>，handler 不带 static（与本包 mixin 铁律一致：
 * handler 的 static 修饰符必须与被注入方法一致；args 精确 = 目标参数 + CallbackInfo）。
 */
@Mixin(ReplacementEditorManager.class)
public abstract class ReplacementEditorManagerMixin {

    @Inject(
            method = "createPackMcmetaIfNeeded(Ljava/nio/file/Path;)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private void oeikjsfix$createPackMcmetaIfNeeded(Path datapackRoot, CallbackInfo ci) {
        ci.cancel();
    }
}
