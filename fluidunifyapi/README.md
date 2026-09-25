# fluidunifyapi

可配置的**机器流体输入定义** API。对 IC2 Refabricated / Industrial Foregoing /
Industrial Upgrade 每一台有流体输入的机器，提供 KubeJS 配置驱动的新流体接入能力。

## 设计边界

只改**输入侧定义**——配方表、谓词、判定点。输出侧、流体身份、Forge `FluidTank`
的防混装语义一概不碰。

取代旧的 fluidunifyfix / iuunify「罐门禁 / 匹配宽限」路线：那条路要动
`FluidTank.fill` 的语义，已被否决。

## 依赖

全部 `compileOnly`，运行时由整合包提供：

| 上游 | 版本 |
|---|---|
| Industrial Upgrade | 3.4.0.10 |
| IC2 Refabricated | 0.6 |
| Industrial Foregoing | 3.5.22 |
| Titanium | 3.8.35 |
| kjsindustrialforegoing | 1.0 |
| KubeJS | 2001.6.5（+ rhino + architectury） |
| JEI | 15.48.0.179 |

## 写一条规则

脚本放 `kubejs/server_scripts/`，入口是 `FluidUnifyEvents.unify`。

### 六键形态

```js
FluidUnifyEvents.unify(event => {
    event.add({
        machine: 'industrialupgrade:single_fluid_adapter',   // 键1 机器 id
        fluid:   'ic2_120:distilled_water',                  // 键2 模板流体
        mode:    'add',                                      // 键3 'add' | 'replace'
        match:   'list',                                     // 键4 'list' | 'tag'
        fluids:  ['ic2_120:distilled_water', 'gtceu:distilled_water']   // 键5（list 模式）
        // tag:  'forge:distilled_water'                     // 键6（tag 模式）
    })
})
```

### 链式糖

等价写法，动作由方法名决定：

```js
FluidUnifyEvents.unify(event => {
    event.machine('industrialupgrade:single_fluid_adapter')
         .forFluid('ic2_120:distilled_water')
         .add(['ic2_120:distilled_water', 'gtceu:distilled_water'])
    //  .replace([...])      替换而非并列
    //  .addTag('forge:...')     整族按 tag 接入
    //  .replaceTag('forge:...')
})
```

### 六个键

| 键 | 必填 | 说明 |
|---|---|---|
| `machine` | 是 | 机器 id，须落在机器登记表内（见下） |
| `fluid` | 是 | 该机器**原生支持**的模板流体 id，用于定位输入口 |
| `mode` | 否 | `add` 并列 / `replace` 取代；省略时随 `add()` / `replace()` 方法名 |
| `match` | 否 | `list` 按 id 列表 / `tag` 按 tag 整族；省略时按 `fluids` 还是 `tag` 推断 |
| `fluids` | list 模式必填 | 流体 id 列表，非空且全部可解析 |
| `tag` | tag 模式必填 | 流体 tag 字符串，如 `forge:biofuel` |

**模板流体的作用**：本 API 用「原生谓词是否接受这个流体」来定位机器的输入口。
写规则时必须填该机器原本就认的流体，接进去的新流体才会落在同一个口上。

## 机器 id

### 约定

| 来源 | 形式 | 例 |
|---|---|---|
| IC2 / IF | 方块注册名 | `ic2_120:condenser`、`industrialforegoing:bioreactor` |
| IU 配方族 | `industrialupgrade:<配方管理器名>` | `industrialupgrade:fluid_mixer` |
| IU 硬编码族 | `industrialupgrade:<机器俗名>` | `industrialupgrade:biofuel_generator` |

### IC2（16 台）

```
ic2_120:steam_generator          ic2_120:condenser
ic2_120:solar_distiller          ic2_120:water_generator
ic2_120:liquid_heat_exchanger    ic2_120:fermenter
ic2_120:blast_furnace            ic2_120:geo_generator
ic2_120:ore_washing_plant        ic2_120:miner
ic2_120:nuclear_reactor          ic2_120:replicator
ic2_120:animalmatron             ic2_120:cropmatron
ic2_120:steam_kinetic_generator  ic2_120:fluid_heat_generator
```

### Industrial Foregoing

方块实体族（15 台）：

```
industrialforegoing:bioreactor              industrialforegoing:biofuel_generator
industrialforegoing:latex_processing_unit   industrialforegoing:hydroponic_bed
industrialforegoing:plant_gatherer          industrialforegoing:sewage_composter
industrialforegoing:fermentation_station    industrialforegoing:fluid_sieving_machine
industrialforegoing:washing_factory         industrialforegoing:mechanical_dirt
industrialforegoing:material_stonework_factory
industrialforegoing:potion_brewer           industrialforegoing:sludge_refiner
industrialforegoing:spores_recreator        industrialforegoing:mycelial_reactor
```

datapack 配方族（4 台；其中只有 `dissolution_chamber` 有流体输入）：

```
industrialforegoing:dissolution_chamber     industrialforegoing:fluid_extractor
industrialforegoing:laser_drill             industrialforegoing:material_stonework_factory
```

### Industrial Upgrade

配方族（41 个管理器）：

```
obsidian              mixer                 replicator            mixer_double
gas_combiner          electrolyzer          refrigerator          item_divider
item_divider_fluid    fluid_separator       polymerizer           fluid_adapter
fluid_integrator      solid_electrolyzer    squeezer              oil_purifier
dryer                 oil_refiner           adv_oil_refiner       imp_oil_refiner
fluid_mixer           solid_fluid_mixer     heat                  gas_chamber
primal_fluid_integrator                     smeltery              empty
ingot_casting         gear_casting          solid_fluid_integrator
single_fluid_adapter  biomass               refractory_furnace    mini_smeltery
incubator             insulator             rna_collector         mutatron
reverse_transcriptor  genetic_stabilizer    genetic_replicator
```

物品侧配方族（流体内嵌在物品配方里，3 个管理器）：
`elec_refractory_furnace`、`plastic`、`plasticplate`

硬编码谓词族（发电机与热机等）：`biofuel_generator`、`diesel_generator`、
`petrol_generator`、`gas_generator`、`hydrogen_generator`、`steam_generator`、
`geo_generator`、`heat_machine`、`smeltery_fuel_tank`、`smeltery_controller` 等。

> 完整清单以源码 `core/MachineAdapters.java` 为准，上表未逐条展开的部分见该文件。

## 校验

**fail-fast**：机器 id 必须落在登记表内、模板流体必须可解析、`fluids` 列表非空且
全部可解析——配置写错当场抛异常并在日志中标出是哪台机器，不静默吞掉。

## 已知边界

- **无流体输入口的机器不在表内**：泵、装罐机（任意流体）、焦炉与炉箅（仅输出）、
  UU 物质发生器（仅输出）等。配置打到它们身上会在校验阶段报错。
- **半流质发电机是 tag 驱动**，不经本 API，直接用 KubeJS 的 tag 接。
- **多方块不在此表**覆盖范围内。
- 事件在 `TagsUpdatedEvent`（priority = LOWEST）触发，保证晚于 IU 的原生配方注册
  且 tag 已装载。单人档同 JVM 静态共享，服务端应用的补丁对客户端 JEI 直接可见。

## 架构

三层：

| 层 | 位置 | 职责 |
|---|---|---|
| 配置层 | `kubejs.FluidUnifyEvents` | KubeJS 插件注册自定义事件，脚本按六键声明补丁 |
| 核心引擎 | `core.UnifiedFluidRegistry` | 补丁解析 / 校验 / 查询中枢，幂等、无锁读 |
| 机器适配 | `iu/` `ifmod/` `ic2/` | IU 配方族走公开 API 写运行时配方表；IU / IF 硬编码族走谓词咽喉点 mixin 探针咨询；IC2 逐机判定点 mixin；IF datapack 族走 KubeJS 配方管线（服务端直写 + 客户端 `injectRuntimeRecipes`） |
