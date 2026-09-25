package com.sxcccccccc.oeikjsfix.mixin;

import com.mafuyu404.oneenoughitem.client.gui.util.PathUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;

/**
 * OEI 1.0.8 {@link PathUtils} 的 GUI 读写路径重定向（目标类在 client 包，
 * 服务端不加载；本包运行时原版成员是 SRG 名，此处只引用 Forge API
 * {@code FMLPaths}（名字恒不变），无任何原版成员引用，规避映射问题。
 * 注入目标串按安装 jar 的 javap 结果精确写（remap=false、完整描述符）。
 *
 * <p><b>字节码证据（javap -p -c，OneEnoughItem-1.0.8.jar）</b>：
 * <ul>
 *   <li>{@code getReplacementsPath()} = {@code getDatapackPath(null)} +
 *       {@code getReplacementsSubPath()}（"data/&lt;id&gt;/replacements"），
 *       id 来自 DomainRegistry（物品编辑器 = "oei"）；</li>
 *   <li>manager {@code createReplacementFile(datapackName, fileName)} =
 *       {@code getDatapackPath(datapackName)} → resolve "data/&lt;id&gt;/replacements"
 *       → createDirectories → {@code createPackMcmetaIfNeeded(dpRoot)}
 *       → resolve "&lt;file&gt;.json"；</li>
 *   <li>manager {@code saveReplacement()} 写的是 {@code currentFilePath}
 *       （来自上述两个链路）；{@code deleteFile(path)} 直接用列表里的 path；</li>
 *   <li>{@code scanAllReplacementFiles(id)} = 遍历 {@code getDatapacksPath()}
 *       顶层各子目录（仅目录条目，lambda 里 Files.isDirectory 过滤；缺失目录
 *       Files.walk 抛 IOException 走 catch 记日志，不会崩），逐个扫描其
 *       "data/&lt;id&gt;/replacements"。</li>
 * </ul>
 * 消费端全部经由这两个"咽喉点"，改两处即可整片覆盖：
 * <ol>
 *   <li><b>{@code getDatapackPath(String)} → gamedir/kubejs</b>：
 *       createReplacementFile 和 getReplacementsPath() 各自再 resolve
 *       "data/&lt;id&gt;/replacements"，得到恰好的 kubejs/data/oei/replacements，
 *       不会拼出 data/data 双段路径。datapack 名字段从此成为装饰（被忽略）。</li>
 *   <li><b>{@code getDatapacksPath()} → gamedir 根</b>：scanAllReplacementFiles
 *       遍历 gamedir 顶层目录时，"kubejs" 这个子目录正好命中
 *       kubejs/data/&lt;id&gt;/replacements——文件浏览界面（FileSelectionScreen）
 *       与 GUI 客户端提示缓存（AbstractGlobalReplacementCache 重建）都能看到
 *       kubejs 规则。若把这里也指到 gamedir/kubejs，遍历逻辑会把 kubejs 的
 *       "data" 子目录当数据包根，拼出 kubejs/data/data/... 双段，扫不到文件。</li>
 * </ol>
 * 两个 handler 的 static 修饰符与原方法一致（静态目标方法），全部取消原方法体
 * （原体里的 Minecraft.getInstance()/findCurrentWorldPath 不再执行）。
 */
@Mixin(PathUtils.class)
public abstract class PathUtilsMixin {

    @Inject(
            method = "getDatapackPath(Ljava/lang/String;)Ljava/nio/file/Path;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void oeikjsfix$getDatapackPath(String name, CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(FMLPaths.GAMEDIR.get().resolve("kubejs"));
    }

    @Inject(
            method = "getDatapacksPath()Ljava/nio/file/Path;",
            at = @At("HEAD"),
            cancellable = true,
            require = 1,
            remap = false
    )
    private static void oeikjsfix$getDatapacksPath(CallbackInfoReturnable<Path> cir) {
        cir.setReturnValue(FMLPaths.GAMEDIR.get());
    }
}
