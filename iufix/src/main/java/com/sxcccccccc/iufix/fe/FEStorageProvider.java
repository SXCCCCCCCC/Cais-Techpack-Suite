package com.sxcccccccc.iufix.fe;

import com.denfop.blockentity.base.BlockEntityBase;
import com.denfop.componets.AbstractComponent;
import com.denfop.componets.Energy;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Set;

/**
 * 1.3.0 方案 A：IU 机器直接暴露 Forge FE capability（吃+吐），移植 gregfluxology 机制。
 *
 * <p>链路结论（javap 证据）：{@code BlockEntityBase.getCapability} 在
 * {@code capabilityComponents.get(cap) == null} 时 {@code return super.getCapability(...)}
 * ——走标准 Forge 附加链，因此 {@code AttachCapabilitiesEvent} 附加的 ENERGY capability
 * 直接可见，无需 mixin。Energy 组件不在 capabilityComponents 里注册任何 capability
 * （AbstractComponent.getProvidedCapabilities 默认空集，Energy 未覆写）。
 *
 * <p>包装语义（全部经 javap/13 源码确认）：
 * <ul>
 *   <li>组件获取：遍历 {@code BlockEntityBase.componentList}（public 字段）找
 *       {@code instanceof Energy}，每台机器唯一。</li>
 *   <li>存量/容量：{@code Energy.getEnergy()/getCapacity()} = buffer.storage/capacity（EU）。</li>
 *   <li>吃电：{@code addEnergy(eu)} 实插并 clamp 到剩余容量，返回实插量；simulate 探针用
 *       {@code min(eu, getFreeEnergy())}。</li>
 *   <li>吐电：{@code useEnergy(eu, simulate)} 返回 {@code min(eu, storage)}（实际消耗量），
 *       simulate=true 不扣减——与 IEnergyStorage.extractEnergy 语义直接对应。</li>
 *   <li>方向：{@code sinkDirections/sourceDirections}（getSinkDirs/getSourceDirs）。IU 语义
 *       （asBasicSink=allFacings+emptySet）：空集=该能力禁用；集合非空时按 side 成员判定；
 *       side==null 全局查询=能力集合非空即可。</li>
 *   <li>换算：1 EU = 4 FE，固定写死（社区惯例，与 IU 原 wrapper 一致，不参数化）。</li>
 * </ul>
 *
 * <p>不做 tier 吞吐钳制（容量 clamp 天然限制，FE 设备自持节流）；不走 IU 能量网循环
 * （直接操作 buffer，与机器内部充电槽/电网共用同一 buffer，数据一致）。
 */
public class FEStorageProvider implements ICapabilityProvider {

    /** 换算率：1 EU = 4 FE（与 IU 原 EnergyForge 包装器一致）。 */
    private static final double FE_PER_EU = 4.0;
    private static final int MAX_INT = Integer.MAX_VALUE;

    private final LazyOptional<IEnergyStorage>[] bySide; // [0..5] = Direction ordinal, [6] = null

    @SuppressWarnings("unchecked")
    FEStorageProvider(BlockEntityBase be) {
        this.bySide = new LazyOptional[7];
        for (int i = 0; i < 7; i++) {
            Direction side = i < 6 ? Direction.values()[i] : null;
            bySide[i] = LazyOptional.of(() -> new MachineFEStorage(be, side));
        }
    }

    /** 从机器组件列表中取 Energy 组件（每台机器唯一）。 */
    static Energy findEnergy(BlockEntityBase be) {
        if (be.componentList == null) {
            return null;
        }
        for (AbstractComponent c : be.componentList) {
            if (c instanceof Energy e) {
                return e;
            }
        }
        return null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (cap != ForgeCapabilities.ENERGY) {
            return LazyOptional.empty();
        }
        return bySide[side == null ? 6 : side.get3DDataValue()].cast();
    }

    /** 单方向视图的 IEnergyStorage，方向过滤在 canReceive/canExtract 内完成。 */
    static final class MachineFEStorage implements IEnergyStorage {
        private final BlockEntityBase be;
        private final Direction side;

        MachineFEStorage(BlockEntityBase be, Direction side) {
            this.be = be;
            this.side = side;
        }

        private Energy energy() {
            return findEnergy(be);
        }

        /** IU 方向语义：null 集合=不限；空集=禁用；side==null 全局=能力存在即可。 */
        private boolean accepts(Set<Direction> dirs) {
            if (dirs == null) {
                return true;
            }
            if (side == null) {
                return !dirs.isEmpty();
            }
            return dirs.contains(side);
        }

        @Override
        public boolean canReceive() {
            Energy e = energy();
            return e != null && accepts(e.getSinkDirs());
        }

        @Override
        public boolean canExtract() {
            Energy e = energy();
            return e != null && accepts(e.getSourceDirs());
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            Energy e = energy();
            if (e == null || !canReceive()) {
                return 0;
            }
            double eu = maxReceive / FE_PER_EU;
            if (simulate) {
                return (int) Math.min(MAX_INT, Math.min(eu, e.getFreeEnergy()) * FE_PER_EU);
            }
            return (int) Math.min(MAX_INT, e.addEnergy(eu) * FE_PER_EU);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            Energy e = energy();
            if (e == null || !canExtract()) {
                return 0;
            }
            double eu = maxExtract / FE_PER_EU;
            return (int) Math.min(MAX_INT, e.useEnergy(eu, simulate) * FE_PER_EU);
        }

        @Override
        public int getEnergyStored() {
            Energy e = energy();
            return e == null ? 0 : (int) Math.min(MAX_INT, e.getEnergy() * FE_PER_EU);
        }

        @Override
        public int getMaxEnergyStored() {
            Energy e = energy();
            return e == null ? 0 : (int) Math.min(MAX_INT, e.getCapacity() * FE_PER_EU);
        }
    }
}
