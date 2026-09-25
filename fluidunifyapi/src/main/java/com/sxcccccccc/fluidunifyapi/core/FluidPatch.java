package com.sxcccccccc.fluidunifyapi.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一条"机器输入流体补丁"——用户六键配置的解析后形态（键4=LIST 时键5、键4=TAG 时键6，
 * 两者互斥，由 KubeJS 事件层保证恰好填一个）。
 *
 * <p>语义：machineId 机器的 templateFluid 输入口，接受新流体集合（ADD 并列 / REPLACE
 * 取代）。输出侧永不触碰；流体身份永不改写；防混装由 Forge FluidTank 原生语义保证。</p>
 */
public final class FluidPatch {

    public enum Mode { ADD, REPLACE }

    public enum MatchType { LIST, TAG }

    /** 键1：机器 id（方块注册名；IU 用 "industrialupgrade:<配方管理器名>" 形式）。 */
    public final String machineId;

    /** 键2：该机器原生支持的模板流体 id（用于定位输入口）。 */
    public final String templateFluid;

    /** 键3：增加或替换。 */
    public final Mode mode;

    /** 键4：列表匹配或 tag 整族匹配。 */
    public final MatchType matchType;

    /** 键5：LIST 模式下的流体 id 列表（不可变视图）。 */
    public final List<String> fluids;

    /** 键6：TAG 模式下的流体 tag 字符串（如 "forge:biofuel"）。 */
    public final String tag;

    private FluidPatch(String machineId, String templateFluid, Mode mode, MatchType matchType,
                       List<String> fluids, String tag) {
        this.machineId = machineId;
        this.templateFluid = templateFluid;
        this.mode = mode;
        this.matchType = matchType;
        this.fluids = fluids == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(fluids));
        this.tag = tag;
    }

    /** LIST 模式构造。 */
    public static FluidPatch list(String machineId, String templateFluid, Mode mode, List<String> fluids) {
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("fluidunifyapi: patch machine id is blank");
        }
        if (templateFluid == null || templateFluid.isBlank()) {
            throw new IllegalArgumentException("fluidunifyapi: patch template fluid is blank for machine " + machineId);
        }
        if (fluids == null || fluids.isEmpty()) {
            throw new IllegalArgumentException("fluidunifyapi: patch fluid list is empty for machine " + machineId);
        }
        return new FluidPatch(machineId, templateFluid, mode, MatchType.LIST, fluids, null);
    }

    /** TAG 模式构造。 */
    public static FluidPatch tag(String machineId, String templateFluid, Mode mode, String tag) {
        if (machineId == null || machineId.isBlank()) {
            throw new IllegalArgumentException("fluidunifyapi: patch machine id is blank");
        }
        if (templateFluid == null || templateFluid.isBlank()) {
            throw new IllegalArgumentException("fluidunifyapi: patch template fluid is blank for machine " + machineId);
        }
        if (tag == null || tag.isBlank()) {
            throw new IllegalArgumentException("fluidunifyapi: patch tag is blank for machine " + machineId);
        }
        return new FluidPatch(machineId, templateFluid, mode, MatchType.TAG, null, tag);
    }

    @Override
    public String toString() {
        return "FluidPatch{machine=" + machineId + ", template=" + templateFluid + ", mode=" + mode
                + ", match=" + matchType + (matchType == MatchType.LIST ? ", fluids=" + fluids : ", tag=" + tag) + '}';
    }
}
