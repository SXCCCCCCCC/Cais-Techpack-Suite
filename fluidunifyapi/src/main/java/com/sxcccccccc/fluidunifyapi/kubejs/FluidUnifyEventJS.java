package com.sxcccccccc.fluidunifyapi.kubejs;

import com.sxcccccccc.fluidunifyapi.core.FluidPatch;
import com.sxcccccccc.fluidunifyapi.core.FluidPatch.Mode;
import dev.latvian.mods.kubejs.event.EventJS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * FluidUnifyEvents.unify 事件对象——收集用户六键配置，产出 {@link FluidPatch} 列表。
 *
 * <p>主形态（与用户六键一一对应）：</p>
 * <pre>{@code
 * FluidUnifyEvents.unify(event => {
 *     event.add({
 *         machine: 'industrialupgrade:single_fluid_adapter',  // 键1 机器 id
 *         fluid:   'ic2_120:distilled_water',                 // 键2 模板流体
 *         mode:    'add',        // 键3 'add' | 'replace'（省略时随 add()/replace() 方法名）
 *         match:   'list',       // 键4 'list' | 'tag'（省略时按 fluids/tag 键推断）
 *         fluids:  ['ic2_120:distilled_water', 'gtceu:distilled_water']  // 键5
 *         // tag:  'forge:distilled_water'                    // 键6（tag 模式）
 *     });
 * });}</pre>
 *
 * <p>链式糖（等价）：</p>
 * <pre>{@code
 * event.machine('industrialupgrade:single_fluid_adapter')
 *      .forFluid('ic2_120:distilled_water')
 *      .add(['ic2_120:distilled_water', 'gtceu:distilled_water'])
 *      // .replace([...])  .addTag('forge:...')  .replaceTag('forge:...')
 * }</pre>
 *
 * <p>校验策略（fail-fast）：机器 id 必须落在适配器登记表、模板流体必须可解析、
 * fluids 列表非空且全部可解析——配置错误当场抛异常，绝不静默吞。</p>
 */
public class FluidUnifyEventJS extends EventJS {

    private final List<FluidPatch> patches = new ArrayList<>();

    public List<FluidPatch> getPatches() {
        return patches;
    }

    // ==================== 主形态：六键 Map ====================

    /** add 模式六键补丁。 */
    public void add(Map<?, ?> patch) {
        collect(patch, Mode.ADD);
    }

    /** replace 模式六键补丁。 */
    public void replace(Map<?, ?> patch) {
        collect(patch, Mode.REPLACE);
    }

    private void collect(Map<?, ?> patch, Mode defaultMode) {
        String machine = str(patch.get("machine"), "machine");
        String fluid = str(patch.get("fluid"), "fluid");
        Object modeObj = patch.get("mode");
        Mode mode = modeObj == null ? defaultMode : Mode.valueOf(String.valueOf(modeObj).toUpperCase());
        Object matchObj = patch.get("match");
        Object fluids = patch.get("fluids");
        Object tag = patch.get("tag");

        String match = matchObj == null
                ? (fluids != null ? "list" : tag != null ? "tag" : "list")
                : String.valueOf(matchObj).toLowerCase();
        if ("list".equals(match)) {
            if (!(fluids instanceof List<?> fluidList)) {
                throw new IllegalArgumentException(
                        "[fluidunifyapi] patch for machine " + machine + ": match=list requires a 'fluids' list");
            }
            List<String> ids = new ArrayList<>();
            for (Object o : fluidList) {
                ids.add(str(o, "fluids[]"));
            }
            patches.add(FluidPatch.list(machine, fluid, mode, ids));
        } else if ("tag".equals(match)) {
            if (tag == null) {
                throw new IllegalArgumentException(
                        "[fluidunifyapi] patch for machine " + machine + ": match=tag requires a 'tag' string");
            }
            patches.add(FluidPatch.tag(machine, fluid, mode, String.valueOf(tag)));
        } else {
            throw new IllegalArgumentException("[fluidunifyapi] unknown match type: " + match + " (expected 'list' or 'tag')");
        }
    }

    private static String str(Object o, String key) {
        if (o == null) {
            throw new IllegalArgumentException("[fluidunifyapi] missing required patch key: " + key);
        }
        String s = String.valueOf(o);
        if (s.isBlank()) {
            throw new IllegalArgumentException("[fluidunifyapi] blank patch key: " + key);
        }
        return s;
    }

    // ==================== 链式糖 ====================

    /** 链式入口：锁定机器 id。 */
    public MachineEntry machine(String machineId) {
        return new MachineEntry(machineId);
    }

    /** 链式：机器 → 模板流体 → 动作。 */
    public final class MachineEntry {
        private final String machineId;

        private MachineEntry(String machineId) {
            this.machineId = machineId;
        }

        public PortEntry forFluid(String templateFluid) {
            return new PortEntry(machineId, templateFluid);
        }
    }

    /** 链式：机器 + 模板流体 → 动作（add/replace × list/tag 四种）。 */
    public final class PortEntry {
        private final String machineId;
        private final String templateFluid;

        private PortEntry(String machineId, String templateFluid) {
            this.machineId = machineId;
            this.templateFluid = templateFluid;
        }

        public void add(List<?> fluids) {
            patches.add(FluidPatch.list(machineId, templateFluid, Mode.ADD, toStrings(fluids)));
        }

        public void add(String... fluids) {
            patches.add(FluidPatch.list(machineId, templateFluid, Mode.ADD, List.of(fluids)));
        }

        public void replace(List<?> fluids) {
            patches.add(FluidPatch.list(machineId, templateFluid, Mode.REPLACE, toStrings(fluids)));
        }

        public void replace(String... fluids) {
            patches.add(FluidPatch.list(machineId, templateFluid, Mode.REPLACE, List.of(fluids)));
        }

        public void addTag(String tag) {
            patches.add(FluidPatch.tag(machineId, templateFluid, Mode.ADD, tag));
        }

        public void replaceTag(String tag) {
            patches.add(FluidPatch.tag(machineId, templateFluid, Mode.REPLACE, tag));
        }

        private List<String> toStrings(List<?> list) {
            if (list == null) {
                throw new IllegalArgumentException("[fluidunifyapi] fluid list is null for machine " + machineId);
            }
            List<String> out = new ArrayList<>(list.size());
            for (Object o : list) {
                out.add(str(o, "fluids[]"));
            }
            return out;
        }
    }
}
