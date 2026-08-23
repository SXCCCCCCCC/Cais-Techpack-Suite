package com.sxcccccccc.iufix;

import com.mojang.brigadier.context.CommandContext;
import com.sxcccccccc.iufix.util.MultiblockCollisionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 1.4.0：一次性反污染命令 {@code /iufix cleanmultiblock}。
 *
 * <p>用途：清扫当前所有已加载维度里的「死指针代理」——NBT 指向的 master
 * 位已不是 BlockEntityBase 的 BlockEntityCollisionProxy 方块（1.4.0 之前的
 * 绝对坐标旧数据造成的持久污染）。只拆代理方块，不动任何 master/正常结构；
 * 与 refresh 的惰性清扫互补：refresh 只能覆盖「有某 master 恰好刷新」的时机，
 * 命令是主动全维度扫一遍，适合进档后先执行一次。
 *
 * <p>用法：
 * <pre>
 *   /iufix cleanmultiblock            — 全维度清理死指针代理（需 OP 权限）
 *   /iufix cleanmultiblock dryrun     — 只统计不拆除（先看会动哪些）
 * </pre>
 * 结果按维度报告「死指针数 / 健康代理数 / 合计移除数」。
 *
 * <p>注：只能扫到已加载区块里的代理；未加载区块的存量污染在区块加载时
 * 被健康校验标记、被下一次 refresh 清扫回收（见 BlockEntityCollisionProxyIEModeMixin）。
 * 多胞结构破坏时客户端上代理仍会短暂显示，服务端拆方块后同步移除。
 */
public final class CleanMultiblockCommand {

    private CleanMultiblockCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("iufix")
                .then(Commands.literal("cleanmultiblock")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> run(context, false))
                        .then(Commands.literal("dryrun")
                                .executes(context -> run(context, true)))));
    }

    private static int run(CommandContext<CommandSourceStack> context, boolean dryRun) {
        CommandSourceStack source = context.getSource();
        int totalRemoved = 0;
        StringBuilder report = new StringBuilder("[iufix] ")
                .append(dryRun ? "dryrun 预检（未拆除）：" : "清理结果：");
        for (ServerLevel level : source.getServer().getAllLevels()) {
            int[] counts = MultiblockCollisionUtil.cleanDimension(level, dryRun);
            totalRemoved += counts[0];
            report.append(level.dimension().location()).append(": 死指针=").append(counts[0])
                    .append(" 健康代理=").append(counts[1]).append("; ");
        }
        report.append("合计移除=").append(totalRemoved);
        source.sendSuccess(() -> Component.literal(report.toString()), true);
        return totalRemoved;
    }
}
