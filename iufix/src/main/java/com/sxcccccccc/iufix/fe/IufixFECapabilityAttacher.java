package com.sxcccccccc.iufix.fe;

import com.denfop.blockentity.base.BlockEntityBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 1.3.3 方案 A 注册点：{@link AttachCapabilitiesEvent}（Forge EVENT_BUS）为 IU 机器附加
 * {@code iufix:energy_fe} FE capability。
 *
 * <p><b>时序铁证（Forge 1.20.1 补丁源码）</b>：{@code BlockEntity.<init>} 尾部
 * {@code this.gatherCapabilities()}——attach 事件在 <b>BlockEntity 构造期间</b>触发，
 * 早于子类构造器体的 {@code addComponent(...)}（Energy 组件注册）。因此本处理器
 * <b>不能</b>在 attach 时查找 Energy 组件（componentList 必为空，1.3.0 因此全部漏挂、
 * Mek 完全看不见 capability）；改为无条件附加，组件查找延迟到 capability 首次 resolve
 * 时（BE 构造完成后才可能被查询，组件必然已注册）。
 *
 * <p>{@code BlockEntityBase.getCapability} 对未注册 capability 走 {@code super.getCapability}
 * （Forge 附加链），附加后 Mek 电缆 / IC2-FE 桥等 FE 设备即可直接对机器查 ENERGY 能力。
 * 无 Energy 组件的 BlockEntityBase 子类（如纯物品机器）resolve 时返回 empty，不受影响；
 * 服务端同样生效（能量逻辑在服务端）。
 */
@Mod.EventBusSubscriber(modid = "iufix")
public class IufixFECapabilityAttacher {

    private static final ResourceLocation FE_CAP_ID = new ResourceLocation("iufix", "energy_fe");

    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        if (event.getObject() instanceof BlockEntityBase) {
            event.addCapability(FE_CAP_ID, new FEStorageProvider((BlockEntityBase) event.getObject()));
        }
    }
}
