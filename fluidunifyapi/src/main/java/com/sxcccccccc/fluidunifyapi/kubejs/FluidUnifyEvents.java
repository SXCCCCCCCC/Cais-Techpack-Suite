package com.sxcccccccc.fluidunifyapi.kubejs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;

/**
 * 本 mod 的 KubeJS 事件组。脚本侧自动获得 {@code FluidUnifyEvents.<事件名>} 绑定。
 *
 * <p>事件由 {@code listener.FluidUnifyListener} 在 Forge TagsUpdatedEvent
 * （priority = LOWEST，保证晚于 IU 的原生配方注册 IUCore.getore、且 tag 已装载）
 * 时 post（ScriptType.SERVER）。单人档同 JVM 静态共享：服务端应用的补丁对
 * 客户端 JEI 直接可见。</p>
 */
public interface FluidUnifyEvents {

    EventGroup GROUP = EventGroup.of("FluidUnifyEvents");

    /** 统一流体补丁事件。用法见 {@link FluidUnifyEventJS} 的类注释。 */
    EventHandler UNIFY = GROUP.server("unify", () -> FluidUnifyEventJS.class);
}
