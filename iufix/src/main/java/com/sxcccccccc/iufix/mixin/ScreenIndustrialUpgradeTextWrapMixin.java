package com.sxcccccccc.iufix.mixin;

import com.denfop.screen.ScreenIndustrialUpgrade;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.4.3-hotfix6：指南书（任务详情窗口）中文描述不换行修复。
 *
 * <p>现象：书内任务描述（lang key {@code iu.guide_quest_description.*}，zh_cn 49 条
 * 全为无空格长段中文、0 条含 \n）一行画到底，被 193px 宽的 scissor 裁掉，
 * 无法阅读；页数 maxPage = lines.size()-6 恒为 1，翻页也失效。
 *
 * <p>根因（dev 源码 + 部署 jar 字节码 + 可读树三方一致）：描述渲染走
 * {@code ScreenIndustrialUpgrade.splitTextToLines}——先按 "\n" 分段、再按 " "（空格）
 * 分词做贪心换行。中文没有空格 → 整段中文是单个"单词" → 循环里 testLine 只有一次
 * 判定就超宽 → 产出恒为单行。英文/俄文（有空格）不受影响，故只有中文用户报此问题。
 * 换行宽度（canvasWidth/scale）是方法局部变量，无法在 split 调用点拿到；
 * 本方法自包含、3.4.0.10 与 dev 源码逐行一致（部署 jar 该类 md5 与 libs 标本相同）。
 *
 * <p>修复：@Overwrite 该方法。保留原语义（\n 分段、空格分词贪心、scale 参与宽度
 * 判定、超宽单词单独成行），新增 CJK 回退——空格分词后仍超宽的长词（中文连续串、
 * 超长英文词）按 font.width 逐字符切块，每块 ≤ 画布宽度，块与块之间在宽度允许时
 * 续接。宽度度量与原实现完全一致（font.width × scale），有空格语言的换行行为不变。
 * 参考：WrapFix 同类方案（CJK 串不作为不可断单词）。
 *
 * <p>为什么不用 @Redirect：split 调用点栈上只有字符串本身，看不到 canvasWidth 与
 * scale；@ModifyReturnValue 需 MixinExtras（本包全库无 com.llamalad7，实证）。
 * 本 @Overwrite 失败会直接启动崩溃（目标方法缺失），不会静默失效。
 *
 * <p>目标为 denfop 类 → remap=false，方法名按运行时真名 "splitTextToLines"；
 * 覆写体内 vanilla 引用（Minecraft.getInstance().font / Font.width）由 reobfJar
 * 打包时自动官方名→SRG 重映射（m_91268_ / m_92895_），无需手写 SRG。
 */
@Mixin(value = ScreenIndustrialUpgrade.class, remap = false)
public abstract class ScreenIndustrialUpgradeTextWrapMixin {

    /**
     * @author SXCCCCCCCC
     * @reason IU 原实现只按空格分词，中文（无空格）整段作为单个单词永不换行；
     *        本覆写在保留原分词语义的基础上增加字符级宽度回退。
     */
    @Overwrite
    public List<String> splitTextToLines(String text, int canvasWidth, float scale) {
        List<String> lines = new ArrayList<>();
        Font font = Minecraft.getInstance().font;
        String[] manualLines = text.split("\n");
        for (String manualLine : manualLines) {
            StringBuilder current = new StringBuilder();
            int len = manualLine.length();
            int i = 0;
            while (i < len) {
                while (i < len && manualLine.charAt(i) == ' ') {
                    i++;
                }
                if (i >= len) {
                    break;
                }
                int wordStart = i;
                while (i < len && manualLine.charAt(i) != ' ') {
                    i++;
                }
                String word = manualLine.substring(wordStart, i);
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (font.width(candidate) * scale <= canvasWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (current.length() > 0) {
                        lines.add(current.toString());
                        current.setLength(0);
                    }
                    iufix$appendWordCharWrapped(font, lines, current, word, canvasWidth, scale);
                }
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines;
    }

    /**
     * 长词（无空格串）按宽度逐字符切块。保证进度：即使单个字符也超宽（极小画布），
     * 每轮至少推进 1 个字符。
     */
    @Unique
    private void iufix$appendWordCharWrapped(Font font, List<String> lines, StringBuilder current, String word, int canvasWidth, float scale) {
        int len = word.length();
        int j = 0;
        while (j < len) {
            int k = j + 1;
            while (k < len && font.width(word.substring(j, k + 1)) * scale <= canvasWidth) {
                k++;
            }
            String chunk = word.substring(j, k);
            String candidate = current.length() == 0 ? chunk : current + chunk;
            if (current.length() > 0 && font.width(candidate) * scale > canvasWidth) {
                lines.add(current.toString());
                current.setLength(0);
            }
            current.append(chunk);
            j = k;
        }
    }
}
