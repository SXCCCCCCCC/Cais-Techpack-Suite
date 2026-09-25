# 终局物品 mod — 实现指南

2026-09-14 整理。所有条目均来自本包实测（配置 / kubejs 导出 / 包内 jar）或源码核实，来源已逐条标注。
**标「未核实」的不要直接落进代码。**

---

## 一、形态定案

奖杯是**方块**，同时有 **BlockItem** 形态。两条路各走各的：

| 形态 | 谁来充 | 机制 |
|---|---|---|
| **物品**（拿在手里） | AE2 / GT / IC2 / IU / Mek / IE / Thermal / IF / PnC | 各家的物品级接口 |
| **方块**（放下来） | Create | 顶面接旋转动力，消耗应力充气 |

**必然的坑：物品↔方块状态同步。** BlockItem 放下时会调 `updateBlockEntityTag` 把物品 NBT 传给 BlockEntity——BlockEntity 必须正确实现 `getUpdateTag` / `load` / `saveAdditional`，否则充了一半放下来就清零。

**模型可以直接抄 `proof_of_honor`**：`models/custom/{championship_trophy,runner_up_trophy,third_place_trophy,trophy}.json` 四个文件都是 17140 字节、完全同构，换一张材质就是新奖杯。材质路径 `assets/<命名空间>/textures/block/<名字>.png`（那个 mod 的 `textures/item` 是空的，物品形态直接复用方块材质）。

---

## 二、十家的充电接口

### 归类

| mod | 类 | 接口 / 机制 | **判定方式** | 只挂通用 FE 能否被它充 |
|---|---|---|---|---|
| **GTCEu** | A + FE | `com.gregtechceu.gtceu.api.capability.IElectricItem` | **`instanceof`** | 能（`nativeEUToFE=true`），但会被 EU 路径短路 |
| **AE2** | A | `appeng.api.implementations.items.IAEItemPowerStorage` | **`instanceof`** | **不能** |
| **IC2 Refab** | A | `ic2_120.content.item.energy.IBatteryItem` / `IElectricTool`（父 `ITiered`） | **`instanceof`** | **不能** |
| **IU** | A | `com.denfop.api.item.energy.EnergyItem` + `ElectricItemManager` | **`instanceof`** | **不能** |
| **PnC** | A | `me.desht.pneumaticcraft.api.tileentity.IAirHandlerItem`（capability `PNCCapabilities.AIR_HANDLER_ITEM_CAPABILITY`）**+ 必须同时实现 `IPressurizableItem`** | **capability** | **不能** |
| **Mekanism** | A + FE | `mekanism.api.energy.IEnergyContainer` | capability（也认 FE） | 能 |
| **Thermal** | A + FE | `cofh.lib.api.item.IEnergyContainerItem`（但 `charge_bench` **只查 FE**） | capability（FE） | 能 |
| **IE** | B | 无私有接口 | capability（FE） | 能 |
| **IF** | B | 无私有接口 | capability（FE） | 能 |
| **Create** | 方块级 | 见第六节 | 方块 | 不适用 |

**要点**：AE2 / IC2 / IU / GT 四家是 **`instanceof` 判定**，所以物品类**必须真的 `implements` 那个接口**，光注册 capability 没用（两个都做最保险）。

### 三家的名字和常见写法不一样（已核实不存在）

- `ic2.api.item.IElectricItem` —— **不存在**。IC2 Refab 去掉了整个 `ic2/api` 包，现在是 `ic2_120.content.item.energy.IBatteryItem`。
- `me.desht.pneumaticcraft.api.item.IPneumaticItem` —— **不存在**。真名 `IAirHandlerItem`。
- `com.simibubi.create...IStressImpact` / `StressConsumer` —— **不存在**。Create 的应力注册入口是 `com.simibubi.create.api.stress.BlockStressValues` 的 `IMPACTS` 注册表。

---

## 三、GT 那家：必须自己实现 `IElectricItem`

### 为什么不能直接用 GT 的现成实现

`BatteryBufferMachine` 的充能分支（源码 8.0.0，`com/gregtechceu/gtceu/common/machine/electric/BatteryBufferMachine.java:375-384`）：

```java
for (Object item : batteries) {
    long charged = 0;
    if (item instanceof IElectricItem electricItem) {
        charged = electricItem.charge(
                Math.min(distributed, GTValues.V[electricItem.getTier()] * inputAmpsPerItem),
                tier, true, false);
    } else if (item instanceof IEnergyStorage energyStorage) {     // ← else if，互斥
        charged = FeCompat.insertEu(energyStorage,
                Math.min(distributed, GTValues.V[tier] * inputAmpsPerItem), false);
    }
    ...
}
```

**结论**：实现了 `IElectricItem` → GT 充电器**只走 EU 路径，绝不碰 FE**。这就是"短路 FE 充电"，成立。
而 Mek / IF / IE / Thermal 走的是 Forge capability 查询，**不受影响，照常充 FE**。

### 但 GT 自带的 `ElectricItem` 有一个硬门槛

`com/gregtechceu/gtceu/api/item/capability/ElectricItem.java:75-90`：

```java
public long charge(long amount, int chargerTier, boolean ignoreTransferLimit, boolean simulate) {
    if (itemStack.getCount() != 1) {
        return 0L;                                           // ← 堆叠数必须 = 1
    }
    if ((chargeable || amount == Long.MAX_VALUE) && (chargerTier >= tier) && amount > 0L) {
        ...                                                  // ← chargerTier >= tier 是硬要求
    }
    return 0;                                                // ← 不满足直接 0
}
```

**如果奖杯 `tier = UHV(10)`，那么 UV / ZPM / LuV / … 所有低档充电器全部返回 0，一点都充不进去。** 玩家看到的是"充不进"，不是"充得慢"。

所以要**自己实现 `IElectricItem`，只改这一处**——删掉 `chargerTier >= tier` 那个条件。

### 自己实现的骨架

```java
public class TrophyElectricItem implements IElectricItem {
    private final ItemStack stack;
    private final long maxCharge;
    private final int tier;              // 设成 GTValues.UHV

    @Override public long charge(long amount, int chargerTier, boolean ignoreTransferLimit, boolean simulate) {
        if (stack.getCount() != 1) return 0L;
        // 【改这里】不再检查 chargerTier >= tier
        long canReceive = getMaxCharge() - getCharge();
        long charged = Math.min(amount, canReceive);
        if (!simulate) setCharge(getCharge() + charged);
        return charged;
    }

    @Override public long getTransferLimit() { return GTValues.V[getTier()]; }
    @Override public long getMaxCharge() { /* 读 NBT "MaxCharge"，缺省用字段 */ }
    @Override public long getCharge() { /* 读 NBT "Charge" */ }
    @Override public boolean canProvideChargeExternally() { return false; }  // 不是电池，不对外放电
    @Override public boolean chargeable() { return true; }
    @Override public int getTier() { return tier; }
    @Override public long discharge(long amount, int dischargerTier, boolean ignoreTransferLimit,
                                    boolean externally, boolean simulate) { return 0L; }
    // canUse(long) 有默认实现，不用写
}
```

**NBT 约定沿用 GT 的**（别家 GT 工具/电池都认这套）：`"Charge"`(long)、`"MaxCharge"`(long，可覆盖)、`"Infinite"`(boolean，`getCharge()` 直接返回满值)。

**两个必须知道的约束**：

1. **`getTier()` 不能写低**。速率上限是 `min(distributed, V[物品tier] × 4)`——**`V[物品tier] × 4` 是天花板**。tier 写 LV 的话，UHV 充电器也被卡在 128 EU/t。**tier 要写 UHV**，"低档慢"是机器电压 `distributed` 自然造成的。
2. **`ignoreTransferLimit = true`**。GT 调用时忽略物品的传输限制，**想用「物品侧接收上限」卡 GT 的速率是无效的**。GT 那条路只能靠 `getTier()` 和 `charge()` 内部逻辑调。

### 注册 capability

参照 GT 自家写法（`com/gregtechceu/gtceu/api/item/component/ElectricStats.java:67-70`）：

```java
return GTCapability.CAPABILITY_ELECTRIC_ITEM.orEmpty(capability,
        LazyOptional.of(() -> new ElectricItem(itemStack, maxCharge, tier, chargeable, dischargeable)));
```

---

## 四、数值表

### 单位换算（本包实测，全部与源码默认一致）

```
1 EU = 10 J = 4 FE        1 FE = 2.5 J        1 AE = 2 FE = 5 J
1 EF(IU) = 4 FE           1 Flux(IE) = 1 FE
```

（`config/Mekanism/general.toml` 的 `euConversionRate=10` / `feConversionRate=2.5`；`config/gtceu.yaml` 的 `euToFeRatio=4`；`config/ae2/common.json` 的 `ForgeEnergy: 0.5`）

### 各家速率

| 设备 | 速率 | 折 FE/刻 |
|---|---|---|
| **IF** `infinity_charger` | **无固定上限**（每刻把整块储量 offer 给物品，截断到 int 上限 2.1e9） | ≤ 2.1e9 |
| **IU** `qua_mfsu_chargepad`（tier 11） | 33,554,432 EF/t（构造值）<br>61,988,864 EF/t（模块槽被碰过之后） | 1.34e8 ~ 2.48e8 |
| **GT** `uhv_charger_4x` | 4 × 2,097,152 = 8,388,608 EU/t | 3.36e7 |
| **GT** `uv_charger_4x` | 2,097,152 EU/t | 8,388,608 |
| **Mek** `chargepad` | 1,024,000 J/刻 | 409,600 |
| **Mek** 能量立方（ultimate） | 256,000 J/t | 102,400 |
| **mekmm** 无线充电站 | 100,000 J/刻（全体共享） | 40,000 |
| **IE** `charging_station` | `min(剩余空间, max(平均输入, 256))`，上限 32,000 | 256 ~ 32,000 |
| **IC2** `mfsu_chargepad`（tier4，顶档） | 2048 EU/t | 8,192 |
| **IU** `mfsu_iu_chargepad`（tier4） | 2048 EF/t | 8,192 |
| **createaddition** `tesla_coil` | 5,000 FE/刻（**吃 FE 不吃 SU**） | 5,000 |
| **Thermal** `charge_bench` | 4,000 FE/刻 | 4,000 |
| **AE2** `charger` | 物品自己的 `getChargeRate()` × 1.0 | 由物品决定 |
| **PnC** `charging_station` | 10 mL/刻 × 速度升级倍率 | 另一套单位 |

### 按目标时间反推总量

**GT「UHV 充电器 5 分钟」** = 8,388,608 × 6,000 刻 = **5.03e10 EU = 2.01e11 FE**

同一总量下各档 GT 充电器的实际用时：

| 充电器 | 速率(EU/t) | 时间 |
|---|---|---|
| **UHV** | 8,388,608 | **5 分钟** |
| **UV** | 2,097,152 | **20 分钟** |
| ZPM | 524,288 | 1.3 小时 |
| LuV | 131,072 | 5.3 小时 |
| IV | 32,768 | 21 小时 |
| EV | 8,192 | 3.5 天 |
| HV | 2,048 | 14 天 |
| MV | 512 | 57 天 |
| **LV** | 128 | **227 天** |

**注意 GT 是 4 倍一跳，不是 2 倍**——"低一级双倍时间"在 GT 这边做不到，档位间隔是固定的 4 倍。

**FE 类「30 分钟」**（36,000 刻，上限卡在 Mek chargepad）：409,600 × 36,000 = **1.47e10 FE ≈ 14.7 GFE**

**IC2「30 分钟」**（顶档 tier4）：8,192 × 36,000 = **2.95e8 FE ≈ 0.295 GFE**

**PnC「30 分钟」**：10 mL/刻 × 36,000 = **360,000 mL**（升压到 25 bar 会快 3 倍，容量要相应放大）

---

## 五、物品侧的两个杠杆

| 目标 | 手段 | 影响范围 |
|---|---|---|
| **卡住所有 FE 机器到同一速率** | 在 `receiveEnergy(amount, simulate)` 里加每刻上限 | Mek / IF / IE / Thermal / GT 的 FE 分支**全部**被卡 |
| **调 GT 的速率** | 只能靠 `getTier()` 和 `charge()` 内部逻辑 | GT 的 EU 路径（`ignoreTransferLimit=true` 让物品侧上限失效） |

**IF 的 `infinity_charger` 没有速率上限**（每刻 offer 整块储量），**它比 Mek chargepad 快 5000 倍**。想让它和 Mek 同速，唯一办法就是在物品的 `receiveEnergy` 里设上限——**标签限制不了它**（它的槽位过滤器只有一句 `stack.getCapability(ForgeCapabilities.ENERGY).isPresent()`，不看任何 tag）。

**AE2 的速率也由物品决定**（`getChargeRate()`），在物品侧就能控制，不用额外处理。

---

## 六、Create：方块级应力充气

**Create 本体没有任何物品级能量接口**（全 jar 扫 `IEnergyStorage` 零命中，阳性对照证明扫描有效）。所有储能/充气机制**都落在 BlockEntity 字段上**，物品只是搬运容器。

### 现成的样板：`create:copper_backtank`

| 角色 | 类 |
|---|---|
| 方块 | `com.simibubi.create.content.equipment.armor.BacktankBlock`（`extends HorizontalKineticBlock`，`hasShaftTowards` **只认 `Direction.UP`**） |
| 方块实体 | `com.simibubi.create.content.equipment.armor.BacktankBlockEntity`（`extends KineticBlockEntity`，在 `tick()` 里读 `getSpeed()` 自己消耗应力做事） |

**应力注册入口**（API 包，公开）：

```java
com.simibubi.create.api.stress.BlockStressValues.IMPACTS   // SimpleRegistry<Block, DoubleSupplier>，单位 SU/RPM
```

背罐注册的是 `CStress.setImpact(4.0)`（运行时实测 `saves/<world>/serverconfig/create-server.toml` 里 `copper_backtank = 4.0`）。

方块侧还要配套两件事：方块实现 `IRotate`（`hasShaftTowards` / `getRotationAxis`），方块实体继承 `KineticBlockEntity`（按需覆盖 `calculateStressApplied` / `calculateAddedStressCapacity`）。

### 背罐的充气公式（从字节码逐条解出）

```
airPerTick    = clamp( (|speed| - 100) / 20, 1, 5 )
airLevelTimer = clamp( (128 - |speed|/5) - 108, 0, 20 )
```

每 `(timer+1)` 刻充一次，每次充 `airPerTick`。**容量 900**：

| 转速 | 每刻充 | 充满 900 |
|---|---|---|
| 256 RPM | 5 | 9 秒 |
| 64 RPM | 1（每 8 刻） | 6 分钟 |
| 50 RPM | 1（每 11 刻） | 8.25 分钟 |
| 16 RPM | 1（每 17 刻） | 12.75 分钟 |
| 极低速 | 1（每 21 刻） | **15.75 分钟**（最慢） |

**要"充气 30 分钟"就得把奖杯的容量调大**：按最慢档算，容量 ≈ 900 × (30 / 15.75) ≈ **1700**。**容量是唯一的杠杆**。

### 物品↔方块的 NBT 搬运

背罐的做法（照抄）：
- `BacktankBlock.setPlacedBy` → 放下时把物品 NBT 写进 BE
- `BacktankBlock.getDrops` → 挖掉时把 BE 数据塞回物品

---

## 七、坑清单

1. **GT `chargerTier >= tier`** —— 不删掉，低档充电器返回 0 而不是"很慢"。
2. **GT `ignoreTransferLimit = true`** —— 物品侧接收上限对 GT 无效。
3. **GT 要求 `getCount() == 1`** —— 堆叠数大于 1 直接返回 0。
4. **GT `V[物品tier] × 4` 是速率天花板** —— tier 写低会把高档充电器也卡住。**要写 UHV**。
5. **AE2 / IC2 / IU / GT 是 `instanceof` 判定** —— 必须真的 `implements` 接口，capability 注册只是保险。
6. **PnC 必须同时实现 `IAirHandlerItem` 和 `IPressurizableItem`** —— `AirHandlerItemStack` 的构造里有 `Validate.isTrue`，不实现直接抛异常。单位是 **mL 空气 / bar 压强**。
7. **IF `infinity_charger` 无上限** —— 不加物品侧上限的话它会以 5000 倍速充爆。
8. **Create 全是方块级** —— 物品形态充不了气。
9. **物品↔方块状态同步** —— 不实现 `getUpdateTag`/`load`/`saveAdditional` 会丢充能进度。
10. **`createaddition:tesla_coil` 吃 FE 不吃 SU** —— Create 系唯一能给物品充电的东西，走的是 FE。

---

## 八、待确认

1. **GTCEu 7.5.3 的分支结构**是否与 8.0.0 源码完全一致（本指南引用的源码是 8.0.0；用户已确认"8.0.0 能用"）。
2. **IU 充电板速率的二义性**：`output` 有"构造值"与"模块槽被 set 后重算"两条路径，tier 7~11 相差 ×1.847412109375，未实测长期存档里哪种占主导。
3. **`1 EF = 4 FE`** 有旁证（IU 的 tier 电压表与 IC2 完全一致、lang key 仍叫 `..._type_eu` 只是显示值改成 "EF/t"），但无源码直接断言。
4. **GT 是否真有 OpV 级 `charger_4x`**：`ELECTRIC_TIERS` 上界是 `isHighTier() ? 13 : 8`，而 kubejs 导出快照（09-12）早于开启 highTier（09-13 19:25），导出里只有到 UV 的档。未实测。
5. **背罐最慢 15.75 分钟**是纯公式推算，未在游戏里实测过极低速情形。
