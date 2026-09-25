# 终局线素材：全包工序盘点总表

2026-09-13 整理。四路并行盘点（A: Mek 系/AE2 系，B: IU/IC2/PnC，C: Create 系/IE，D: IF/Thermal/MA/PB）。

**本文件只提供事实，不做设计。**

口径：
- id 一律取自 `local/kubejs/export/registries/{item,block,fluid}.json` 与 `export/recipes/`，每个都逐条命中过。
- 机制描述取自上游 mod 的源码仓库；某家若无源码仓库，则只用注册表证据，不做机制断言。
- **标「未核实」的不要直接写进设计稿。**

---

## 0. 先决问题：142 个 jar ≠ 142 个可认领 mod

你的设计前提是「每 mod 认领 ≥1 步 → 强制全 mod」。这个前提要先落在分母上。

**已核实：以下 jar 没有任何机器，认领不了工序**

| jar | 实际内容 |
|---|---|
| MekanismAdditions | 注册 295 项，全是塑料方块/发光板/气球/幼体生物 |
| MekanismTools | 只有工具/护甲/paxel |
| MekanismEmpowered + MekanismEmpoweredCore | 命名空间 `mekanism_empowered`；4 档 `gauge_dropper` + 6 个升级件，无机器无多方块 |
| JustEnoughMekanismMultiblocks | 是 JEI 的多方块搭建预览插件，不是机器。本包实际生效 8 个预览 |
| ifsoulsdisks | 只有灵魂存储元件（`souls_storage_cell_1k…256k`） |
| Applied-Mekanistics | 注册表 **0 方块**，只有 13 个化学品存储元件 |
| OneEnoughBlock / OneEnoughFluid / OneEnoughItem | OEI 统一系统，非机器 |
| Cucumber / Glodium / MaFgLib / OELib / titanium / balm / cloth-config / architectury / PuzzlesLib / guideme / Patchouli / rhino / kotlinforforge / fabric-api / fabric-language-kotlin / ftb-library / libIPN / constancy / zume | 前置库 |
| Jade / JadeAddons / JourneyMap / BoccHUD / Better_Modlist / InventoryProfilesNext / NoChatRestrictions / double_hotbar / ExtremeSoundMuffler / Forgematica / IMBlocker / I18nUpdateMod / jecharacters | UI/QoL（按名称归类，未逐个核实其是否附带方块） |
| embeddium / ferritecore / modernfix / memoryleakfix / spark / observable | 性能 |
| SimpleBackups / worldedit(.disabled) / DistantHorizons(.disabled) | 工具（其中两个已禁用） |
| patternchecker / jeitransferfix / ae2ipnswipefix / gtjeimbfix / gtnoboomfix / cofhlightfix / constructionstickfix / iespawnfix / ic2mekradfix / ic2reactorhopperfix / createindustrialforegoingfluidfix / creativetabfix / fabrictransferfix / mafgllibfix / macobaltfix / tweakergammafix / iuchancefix / iudebug / iufix* / iuspeed / iuunify / mekmmuufix / uubridge / fluidunifyapi / oeikjsfix / kjsexportcmd / lootjs / Tweakerge / kjsindustrialforegoing / kubejs-* / oeffix / acfix(.disabled) / iufix-1.9.1~1.9.5(.disabled) / oeifix(.disabled) | 你自己写的 fix mod 与脚本桥（**注意**：`gregfluxology` / `gregmek` / `applied_greg` 也在这类里，但它们是内容桥，各自有方块——见下文） |

**含义**：如果分母按 142 个 jar 算，上表这些永远无法被"认领"强制，你的定理不成立。若分母按"有机器、能认领工序的 mod"算，需要先把上表剔掉。**这一步的分母由你定，我不替你裁决。**

---

## 1. 认领候选：每个 mod 的招牌机器

### 1.1 GregTech 系

| mod | 招牌机器 | 理由 |
|---|---|---|
| gtceu | 见 `local/kubejs/export/recipes/gtceu/` 约 180 个工序目录 | 主体 |
| gtca | `gtca:thermal_reactor` 等 | **注意**：`thermal_reactor` 名字骗人，它吃浮选泡沫浆料，是高温化学反应器，**不是核堆** |
| gtse | `harvester` / `large_fisher` / `large_gas_collector` / `mob_simulator` / `nether_collector` / `steam_void_miner` / `tree_farm` | — |
| gregmek | Mek↔GT 桥 | 内容桥，有方块 |
| gregfluxology | GT flux 网络 | 内容桥，有方块 |
| applied_greg | GT↔AE2 桥 | 内容桥，有方块 |
| gtmoldraw | GregTech Molecule Drawings | — |

### 1.2 Mekanism 系（A 组）

| mod | 认领机 | id | 理由 |
|---|---|---|---|
| Mekanism | **SPS 超临界移相器** | `mekanism:sps_casing` + `mekanism:sps_port` | 反物质唯一入口；结构件含大量 `sps_port`；源码 `SPSMultiblockData` 输入槽硬编码只收 POLONIUM |
| Mekanism | 备选：反质子核合成器 | `mekanism:antiprotonic_nucleosynthesizer` | 负责"用掉反物质"；包内已改道：1mB 反物质 + 64 `ic2_120:scrap` → `mekmm:uu_matter`，duration 5000 |
| MekanismGenerators | **聚变反应堆** | `mekanismgenerators:fusion_reactor_controller` + `laser_focus_matrix` | 激光点火 + 氘氚，辨识度最高。备选：工业涡轮机 |
| mekmm | **复制机 Replicator** | `mekmm:replicator` | UU 物质复制；包内整条 UU 链已围绕它改道 |
| AE2 | **分子装配室** | `ae2:molecular_assembler` | 自动合成的符号。备选：空间 IO 端口 / 量子环 |
| ExtendedAE | **装配矩阵** | `expatternprovider:assembler_matrix_*` | 它独有的多方块 |
| MEGA Cells | **256M 合成存储器** | `megacells:256m_crafting_storage` | 容量档位顶点。备选：放射性化学品元件 |
| Applied Mekanistics | **256k 化学品存储元件** | `appmek:chemical_storage_cell_256k` | AE×Mek 的唯一接口 |

**Mek 本体其余的机器**（`mekanism:` 共 199 方块，都是单方块机器，非多方块）：
`enrichment_chamber`（富集）、`crusher`（粉碎）、`metallurgic_infuser`（冶金灌注）、`purification_chamber`（提纯，吃氧气）、`chemical_injection_chamber`、`chemical_dissolution_chamber`（**全包唯一"固体+酸→浆液"**）、`chemical_washer`、`chemical_crystallizer`（**反物质从气变球的唯一机器**）、`chemical_oxidizer`（**包内 UU 晶体→UU 气体的唯一途径**）、`chemical_infuser`、`electrolytic_separator`（产物是 gas 不是 fluid）、`rotary_condensentrator`（**包内已用作 IC2 UU 流体 ↔ mekmm UU 气体 1:1 桥**，见 `added_recipes/mekmm/rotary/uu_matter_bridge.json`）、`pressurized_reaction_chamber`、`isotopic_centrifuge`、`solar_neutron_activator`、`energized_smelter`、`precision_sawmill`、`combiner`、`digital_miner`、`formulaic_assemblicator`、`modification_station`、`nutritional_liquifier`、`resistive_heater`、`pigment_extractor`/`pigment_mixer`/`painting_machine`（**全包唯一工业染色三件套**）、`logistical_sorter`、`oredictionificator`、`laser`/`laser_amplifier`/`laser_tractor_beam`、`seismic_vibrator`（阅读器是**物品** `mekanism:seismic_reader`）、QIO 全家、`teleporter`、`quantum_entangloporter`、`dimensional_stabilizer`、充能板、物流四件套、工厂系列（4 档 × 十余种工序）。

Mek 本体多方块：SPS / 热力蒸馏塔（`thermal_evaporation_block`+`_controller`+`_valve`）/ 热力锅炉（`boiler_casing`+`boiler_valve`+**`pressure_disperser`**）/ 动态储罐 / 感应矩阵。

MekanismGenerators：裂变堆（`fission_reactor_casing`/`_port`/`_logic_adapter`/`fission_fuel_assembly`/`control_rod_assembly`/`reactor_glass`）、工业涡轮机（`turbine_casing`/`_rotor`/`_valve`/`_vent` + `rotational_complex` + `electromagnetic_coil` + `saturating_condenser`）、聚变堆、燃气/热力/生物/太阳能/高级太阳能/风力发电机。

mekmm（`mekmm:` 88 方块）：`replicator`、`fluid_replicator`、`recycler`（**已改道产 `ic2_120:scrap`**：泥土 17% / 石头 17% / 基质 43%，原产 `mekmm:scrap` 已废弃）、`large_antiprotonic_nucleosynthesizer`、`large_electrolytic_separator`、`large_chemical_infuser`、`large_rotary_condensentrator`、`large_solar_neutron_activator`、`large_pigment_mixer`、`large_heat_generator`/`large_gas_burning_generator`/`large_wind_generator`、`cnc_stamper`（与 AE2 压印器、ExtendedAE 切片机**三者互为替代**）、`planting_station`、`wireless_charging_station`、4 档中型/大型化学品储罐、工序工厂（4 档 ×18 种）。

**mekmm 三台已被禁用**（方块仍注册，配方已删、JEI 隐藏、创造 tab 剔除）：`mekmm:cnc_lathe`、`mekmm:cnc_rolling_mill`、`mekmm:ambient_gas_collector`。
**未在本包注册**的档位：dense / multiversal / overclocked / quantum / creative 系工厂（jar 内有资源，需要 `evolvedmekanism`，本包未装）。

AE2 系其余：`ae2:inscriber`（压印器）、`ae2:charger`、`ae2:growth_accelerator`、`ae2:drive`、合成 CPU 全家、`ae2:io_port`、`ae2:condenser`（物质聚合器→`ae2:singularity`）、`ae2:vibration_chamber`、能源接收器/元件、`ae2:crystal_resonance_generator`（被动发电，**无 zh_cn 名**）、`ae2:quantum_ring`+`quantum_link`、空间 IO（`spatial_pylon`+`spatial_io_port`+`spatial_anchor`）、无线访问点。
ExtendedAE：`circuit_cutter`（实测配方 `export/recipes/expatternprovider/cutter/*`：金块 + 100mB 水 → 9 印刷处理器）、`crystal_fixer`、`caner`、装配矩阵、`ex_drive`/`ex_pattern_provider`/`ex_interface`/`ex_molecular_assembler`/`ex_inscriber`/`ex_charger`/`ex_io_port`、`infinity_cell`、`oversize_interface`、`ingredient_buffer`。
MEGA：`mega_crafting_*`、`mega_energy_cell`、超大存储元件（`item/fluid/chemical_storage_cell_{1m..256m}`）、`bulk_item_cell`、`radioactive_chemical_cell`、`mega_pattern_provider`/`mega_interface`、`accumulation_processor`、`sky_steel_ingot`（`ae2:transform` 配方，流体 tag `minecraft:lava`）。
AppMek（0 方块）：`chemical_storage_cell_{1k..256k}`、`portable_chemical_storage_cell_*`、`chemical_cell_housing`、`chemical_p2p_tunnel`。

### 1.3 IU / IC2 / PnC（B 组）

| mod | 认领候选 | 备注 |
|---|---|---|
| IndustrialUpgrade | **太空系统**（35 维度 + 自研世界生成器 + 太阳辐射发电 + 研究进度 + 装备门槛） | 体量最大的一张牌 |
| IndustrialUpgrade | **4 族裂变堆**（液体/高温/气冷/石墨水冷，各 40 结构方块，工质各不相同） | **GTCEu 7.5.3 里没有任何裂变堆**（只有 `chemical_reactor` 和聚变堆） |
| IC2 Refab | UU 复制链 | **IC2 的 UU 成本表已是本包事实标准**：`ic2_120:scrap` 是全包唯一废料来源，GT 侧 `uu_matter_amplificator` = 9 scrap → 1 mB；你自己的 mekmmuufix 和 uubridge 都以 IC2 为源往 MekMM/IU 灌成本 |
| IC2 Refab | 玩家自排元件网格核反应堆 | 机制独家 |
| PneumaticCraft | 无人机 + 60 拼图编程 | 包里没有第二家机器人实体 |
| PneumaticCraft | UV 曝光 + 概率蚀刻 PCB 链 | 别家没有"光刻"这道工序（GT 的是确定性配方） |
| PneumaticCraft | 方块级温度场 / 压力管网 + 负压 | — |

### 1.4 Create 系 / IE（C 组）

**本包改动（先看这个）**

- **Create 压床的锭→板配方被删**：`local/kubejs/export/removed_recipes/create/pressing/` 下 4 个文件，内容即被删配方原文，如 iron 那份：
  ```json
  { "type": "create:pressing",
    "ingredients": [ { "tag": "forge:ingots/iron" } ],
    "results": [ { "item": "create:iron_sheet" } ] }
  ```
  另三份同构：`copper_ingot`→`create:copper_sheet`、`gold_ingot`→`create:golden_sheet`（**肉金名字不规则**）、`brass_ingot`→`create:brass_sheet`。
  → **Create 压床在本包不再承担"锭→自家板"**，但仍保留 compat 的 IE 板配方（`recipes/create/pressing/compat/immersiveengineering/plate_*.json`：铝/康铜/琥珀金/铅/镍/银/钢/铀）。**它在包里的身份变成了"给 IE 出板"。**
- **IE 的锭/粒/块互转与棒配方被删**（`removed_recipes/immersiveengineering/`），交给统一件。
- `added_recipes/immersiveengineering/crafting/fluid_pipe.json`：给 IE 流体管道补了一条合成。
- `added_recipes/gtceu/assembler/void_miner.json`：GT 虚空采矿机改为装配机内制作（LV+MV+HV 采矿机 + EV 电路）。
- `added_recipes/createoreexcavation/kjs/*.json`：**24 条 KubeJS 钻井配方，把 GT/IU 矿脉接进 Create Ore Excavation**（`kubejs:gt_ow_metaloxide_vein`、`gt_ow_sulfide_vein`、`gt_ds_rare_vein`、`iu_ow_adv1_vein`、`iu_ow_mineral2_vein`、`iu_ow_heavysulfide_vein` 等），要求下界合金钻头、stress 2048–8192；配套脚本 `kubejs/server_scripts/{gt_overworld,gt_nether,gt_end,iu,prosperity}_excavation.js`。

**认领候选**

| mod | 认领机 | id | 理由 |
|---|---|---|---|
| create | 动力冲压机（备选：鼓风机） | `create:mechanical_press` | 最 Create 味；备选代表"环境决定工序"的独家机制 |
| create_connected | 动力电池 | `create_connected:kinetic_battery` | 把应力变成可储存量的独有概念 |
| createaddition | 轧机（或交流发电机） | `createaddition:rolling_mill` | 轧制"锭→棒、板→线"；发电机代表 Create↔FE 桥 |
| create_dragons_plus | 批量染色 | `create_dragons_plus:<16色>_dye_bucket` + `create_connected:<16色>_fan_dyeing_catalyst` | 唯一能给实体/装备染色的批量工序 |
| create_jetpack | 喷气背包 | `create_jetpack:jetpack` | 铜背罐+鞘翅改装，mod 全部内容 |
| create_power_loader | 黄铜区块加载器 | `create_power_loader:brass_chunk_loader` | 唯一能跟随移动装置的区块加载 |
| createoreexcavation | 矿物钻井 | `createoreexcavation:drilling_machine` | 旋转驱动无限矿脉钻探（本包已接 GT/IU 矿脉） |
| appliedcreate | ME 齿轮箱 | `appliedcreate:me_gearbox` | 应力↔AE 双向转换，整个 addon 的身份 |
| immersiveengineering | 挖掘机 + 斗轮 | `immersiveengineering:excavator` + `immersiveengineering:bucket_wheel` | IE 的旗舰多方块（备选 `arc_furnace`） |

**create 其余机器**：`mechanical_mixer`+`basin`、`crushing_wheel`+`crushing_wheel_controller`、`millstone`、`encased_fan`+`nozzle`、`blaze_burner`、`deployer`、`mechanical_arm`、`mechanical_crafter`、`spout`/`item_drain`、`steam_engine`/`steam_whistle`、`water_wheel`/`large_water_wheel`/`windmill_bearing`/`flywheel`/`creative_motor`；应力控制组 `stressometer`/`speedometer`/`rotation_speed_controller`/`sequenced_gearshift`/`clutch`/`gearshift`/`gearbox`；移动装置组 `mechanical_bearing`/`clockwork_bearing`/`rope_pulley`/`gantry_carriage`(+`gantry_shaft`)/`elevator_pulley`(+`elevator_contact`)/`mechanical_piston`/`cart_assembler`/`chain_conveyor`/`turntable`/`contraption_controls`；列车套件 `track`/`track_station`/`track_signal`/`track_observer`/`controls`/`display_board`/`schedule`/`small_bogey`/`large_bogey`/`train_door`/`train_trapdoor`；包裹物流组 `packager`/`repackager`/`package_frogport`/`<颜色>_postbox`(16 色)/`stock_ticker`/`stock_link`/`redstone_requester`/`factory_gauge`/`item_hatch`/`<材质>_table_cloth`；`schematicannon`/`schematic_table`；存储输送 `portable_storage_interface`/`portable_fluid_interface`/`hose_pulley`/`item_vault`/`fluid_tank`/`mechanical_pump`/`chute`/`smart_chute`/`depot`/`weighted_ejector`/`belt`；移动中作业 `mechanical_saw`/`mechanical_drill`/`mechanical_harvester`/`mechanical_plough`/`mechanical_roller`。

**create_connected 其余**：`brake`/`centrifugal_clutch`/`freewheel_clutch`/`overstress_clutch`/`inverted_clutch`、`kinetic_bridge`(+`kinetic_bridge_destination`)、`brass_gearbox`/`vertical_brass_gearbox`/`parallel_gearbox`/`six_way_gearbox`、`item_silo`/`fluid_vessel`/`brass_chute`、`inventory_bridge`/`inventory_access_port`、触媒 11 种、`sequenced_pulse_generator`/`cross_connector`/`linked_transmitter`/`dashboard`/`crank_wheel`/`large_crank_wheel`。

**createaddition 其余**：`alternator`、`electric_motor`、`modular_accumulator`+`connector`/`large_connector`/`small_light_connector`/`redstone_relay`、`tesla_coil`、`liquid_blaze_burner`、`portable_energy_interface`。

**create_dragons_plus 其余**：`fluid_hatch`、批量终结/冷冻/磨砂（由 `fan_ending_catalyst_dragon_head`/`fan_ending_catalyst_dragons_breath`/`fan_freezing_catalyst`/`fan_sanding_catalyst` + 龙首/龙息/细雪/沙子触发）、`dragon_breath_cauldron`。

**createoreexcavation 其余**：`extractor`（流体钻井，本包仅 water）、`sample_drill`/`vein_finder`/`vein_atlas`/`drill`/`diamond_drill`/`netherite_drill`。

**appliedcreate 其余**：`kinetic_energy_acceptor`、`stress_storage_cell_1k…256m`、`stress_p2p_tunnel`、`andesite_pattern_provider`/`brass_pattern_provider`、`me_blueprint_cannon`。

**IE 全表**（**此表可能仍有缺漏**——C 组的 IE 清单不完整，已知漏过 `sawmill`，其余未逐条复核）：`crusher`、`arc_furnace`、`sawmill`（+`sawblade`/`sawdust`；配套工具 `buzzsaw` + 升级 `toolupgrade_buzzsaw_spareblades`）、`excavator`+`bucket_wheel`、`metal_press`、`assembler`/`auto_workbench`、`bottling_machine`、`fermenter`/`squeezer`、`refinery`、`mixer`、`coke_oven`、`blast_furnace`/`advanced_blast_furnace`/`furnace_heater`、`alloy_smelter`、`cloche`、`lightning_rod`、`diesel_generator`、`dynamo`/`windmill`/`watermill`、`thermoelectric_generator`、`capacitor_lv/mv/hv`、`transformer`/`transformer_hv`/`breaker_switch`/`current_transformer`/`logic_unit`、`tesla_coil`、`sorter`/`fluid_sorter`/`item_batcher`、`conveyor_basic`/`conveyor_splitter`/`conveyor_extract`/`conveyor_dropper`/`conveyor_redstone`/`conveyor_vertical`、`silo`/`tank`、`sample_drill`/`coresample`/`survey_tools`、`charging_station`/`floodlight`/`electromagnet`/`fluid_pump`、炮塔与武器（`turret_gun`/`turret_chem`/`railgun`/`chemthrower`/`buzzsaw`）、手工台（`workbench`/`circuit_table`/`craftingtable`/`blueprint`）。

**C 组的坑**
- `createaddition:digital_adapter` 依赖 ComputerCraft，**本包没有 CC**（`mods.json` 里无 computercraft）→ 在本包无实际用途。
- `create:<颜色>_postbox` 是 16 色，**没有无染色本体**。
- `create_connected` 的染料桶按 item.json 实测是 **16 色**（不是 17）。
- `create_connected:inventory_bridge`、`createaddition:portable_energy_interface`、`create_dragons_plus:dragon_breath_cauldron`、"IE 热传导发电机是否有等价物"——均**未核实**。

### 1.5 IF / Thermal / MA / PB（D 组）

| mod | 首推 | 理由 | 备选 |
|---|---|---|---|
| industrialforegoing 3.5.22 | `industrialforegoing:dissolution_chamber` | 49 条配方，IF 机器与高级框架的必经之路 | `mycelial_reactor`（结构感最强，适合"收集 16 种燃料"收尾）；`ore_laser_base` |
| industrialforegoingsouls 1.0.4 | `industrialforegoingsouls:soul_surge`（**名自译**） | 该 addon 唯一有功能的方块 | `soul_laser_base` |
| ifsoulsdisks 1.0-SNAPSHOT | `ifsoulsdisks:souls_storage_cell_256k`（**名自译**） | 该 addon 顶层产物 | `souls_cell_housing` |
| thermal_* 11.0.x | `thermal:machine_smelter`（感应炉，102 条配方） | Thermal 辨识度最高 | `thermal:upgrade_augment_3`（谐振整合组件，插件体系招牌）；`thermal:machine_insolator`（有机灌注器，**170 条配方、与 MA 打通**） |
| mysticalagriculture 7.0.24 | `mysticalagriculture:awakening_altar`（觉醒祭坛） | 终局材料 `awakened_supremium_ingot` 唯一出入口；4 基座 + 4 精华储罐 | `infusion_altar`（注魔祭坛，8 基座）作中段；`prosperity_shard` 作象征物 |
| mysticalagradditions 7.0.12 | `mysticalagradditions:insanium_essence`（究极精华，第六级） | 六级作物唯一产出 | `nether_star_crux`（把 boss 掉落变种植） |
| productivebees 12.6.0 | `productivebees:advanced_oak_beehive` + `expansion_box_oak` | "可扩容蜂箱"是结构招牌，必须与扩容盒配对（源码已确认 `ExpansionBox.updateStateWithDirection`） | `centrifuge`；`gene_indexer` |

**IF 机器全表**（`industrialforegoing:`）：`dissolution_chamber`、`fermentation_station`、`washing_factory`、`fluid_sieving_machine`、`fluid_extractor`、`latex_processing_unit`、`material_stonework_factory`（6 条 `stonework_generate`：andesite/cobblestone/diorite/granite/netherrack/obsidian）、`resourceful_furnace`、`bioreactor`、`sludge_refiner`、`sewage_composter`、`sewer`、`hydroponic_bed`（+`simulated_hydroponic_bed`）、`plant_sower`/`plant_gatherer`/`plant_fertilizer`、`mob_crusher`、`mob_slaughter_factory`、`mob_duplicator`、`animal_rancher`/`animal_feeder`/`animal_baby_separator`、`wither_builder`、`stasis_chamber`、`enchantment_extractor`/`enchantment_applicator`/`enchantment_factory`/`enchantment_sorter`、`potion_brewer`、`marine_fisher`、`water_condensator`、`spores_recreator`、`dye_mixer`、`block_breaker`/`block_placer`、`laser_drill`+`ore_laser_base`、`fluid_laser_base`、`laser_lens0…laser_lens15`、`infinity_charger`、`mycelial_reactor`、16 台 `mycelial_*` 发电机、`biofuel_generator`、`pitiful_generator`、`conveyor`/`transporter`、`machine_frame_pity`/`_simple`/`_advanced`/`_supreme`、插件 `speed_addon_1/2`、`efficiency_addon_1/2`、`processing_addon_1/2`、`range_addon0…range_addon11`、无限工具组。

**Thermal 机器全表**（`thermal:`）：`machine_furnace`、`machine_sawmill`(12)、`machine_pulverizer`(97)、`machine_smelter`(102)、`machine_insolator`(**170**)、`machine_centrifuge`(69)、`machine_chiller`(10)、`machine_crucible`(14)、`machine_refinery`(5)、`machine_press`(**293**)、`machine_crafter`、`machine_crystallizer`(10)、`machine_pyrolyzer`(3)、`machine_bottler`(32)、`machine_brewer`；7 台 `dynamo_stirling`/`_compression`/`_magmatic`/`_numismatic`/`_lapidary`/`_disenchantment`/`_gourmand`；11 台 `device_collector`/`_composter`/`_fisher`/`_hive_extractor`/`_nullifier`/`_potion_diffuser`/`_rock_gen`/`_soil_infuser`/`_tree_extractor`/`_water_gen`/`_xp_condenser`；`energy_cell`/`fluid_cell`/`item_buffer`/`tinker_bench`/`charge_bench`；`upgrade_augment_1/2/3` + 19 种专用升级。

**MA 机器全表**（`mysticalagriculture:`）：`infusion_altar`、`infusion_pedestal`、`awakening_altar`、`awakening_pedestal`、`essence_vessel`、`seed_reprocessor`、`soul_extractor`、`soul_jar`、`soulium_spawner`（19 条配方）、`harvester`、`enchanter`、`tinkering_table`、`machine_frame`、5 档 `*_growth_accelerator`、6 档 `*_furnace`、5 档 `*_farmland`、`infusion_crystal`/`master_infusion_crystal`、精华作物体系、`prosperity_seed_base`/`soulium_seed_base`、`soulium_dagger`、各元素团块。

**PB 机器全表**（`productivebees:`）：`centrifuge`、`powered_centrifuge`、`heated_centrifuge`、`incubator`、`breeding_chamber`、`honey_generator`、`bottler`、`feeder`、`catcher`、`gene_indexer`、`amber`、各树种 `advanced_*_beehive` + `expansion_box_*`、各种 nest、`configurable_honeycomb`（蜜脾）、`wax`、`gene`/`gene_bottle`、`honey_treat`、13 种升级。

---

## 2. 天然独家（别家做不了）

按证据强度排序。

**2.1 形态级独家（最强，是"物质形态"本身被锁死）**

1. **反物质**：全包只有 `mekanism:antimatter`（气体）+ `mekanism:pellet_antimatter`（物品）。GTCEu 只有 `antimatter_hazard_sign_block`（装饰牌，无物质）。
2. **"气体（gas）"这一形态本身是 Mek 私有的**：GT/IU/IC2 的机器、管道、储罐**读不了 gas**。→ `isotopic_centrifuge` / `solar_neutron_activator` / `chemical_infuser` / `oxidizer` / `washer` / `crystallizer` / PRC 气体槽 在"气体语义"上天然独家。
   **注意这是"形态独家"不是"工序独家"**——工序往往有液体版等价物。
3. **`ic2_120:uu_matter` / `gtceu:uu_matter` / `mekmm:uu_matter` 三套 UU 并存**（前者两种是流体、后者是物品/气体）——这既是"可替代"的好素材，也意味着**跨套时形态不通用**。

**2.2 机制级独家**

4. **Mek 反质子核合成器的物质嬗变**（物品 + 反物质 → 另一种物品：煤→钻石、蛋→龙蛋等，`NucleosynthesizingRecipeProvider`）。
5. **Mek 核废料→钋/钚链**（`centrifuging`、`activating` 两个配方类型是 Mek 私有）。
6. **AE2 空间 IO**（`ae2:spatial_storage` 维度）：全包唯一"把 128³ 一片空间连方块带实体剪下来存进元件、再异地/异维度粘贴"。
7. **AE2 量子环**：点对点传送不独家（IU 有 `industrialupgrade:tesseract`、Mek 有量子传送装置），但"**整个 ME 网络**跨维度"只有量子环；AppMek/ExtendedAE/MEGA 都得挂靠它。
8. **AppMek 的化学品存储元件 + 化学品 P2P**：AE2 网络内**存取/传输 Mek 气体的唯一手段**（MEGA 的 `chemical_storage_cell_*` 实际也由 AppMek 提供 key）。
9. **MEGA 的 1M–256M 档位 + `radioactive_chemical_cell`**：本包 AE2 生态最大容量（本体止于 256k）。
10. **ExtendedAE 装配矩阵 / `infinity_cell`**：唯一"样板级并行 + 省频道"的多方块；唯一无限容量元件。
11. **MA 的精华作物体系**（`<material>_seeds` → 精华 → 材料）+ **注魔祭坛（8 基座）+ 觉醒祭坛（4 基座 + 4 精华储罐）仪式**。
    证据：扫过 GTCEu 全部 32057 个配方文件，**GTCEu 没有注册任何作物/种子物品**（只有 `gtceu:seed_oil_bucket`）；Create/IE/Mek/PnC 无作物；IC2 作物只产 IC2 自己的；IU 农场种普通作物。→ **"任意材料以作物形式可再生"只有 MA**。
12. **Agradditions 六级作物 + 6 个 crux**：`nether_star_crux` / `dragon_egg_crux` / `awakened_draconium_crux` / `gaia_spirit_crux` / `neutronium_crux` / `nitro_crystal_crux`。
    **已核实**：6 个 crux 全部注册在 `mysticalagradditions:`；对应的 6 套种子与精华也**全部注册**在 `mysticalagriculture:`（`{nether_star,neutronium,dragon_egg,gaia_spirit,awakened_draconium,nitro_crystal}_{seeds,essence}`）。源码里那 4 个 crux 被 `withRequiredMods` 门控（botania/draconicevolution/avaritia/powah，本包都没装），但**注册表里它们在场**。（种子/精华的配方可用性未逐条核实。）
13. **PB 的基因体系 + 蜜蜂本身**：`bee_breeding`/`bee_conversion`/`bee_produce`/`centrifuge` 四类数据包配方；蜜脾能产 **10% 觉醒终极精华**（已核实 `centrifuge/mysticalagriculture/honeycomb_awakened_supremum.json`）。
    **诚实标注**：IU 在本包内也有自己的蜂类内容，**IU 蜜蜂的具体能力未核实**，所以"蜜蜂形态是否独家"给不出定论。能确定的是 PB 的**跨 mod 覆盖率（GTCEu/Mek/Thermal/IE/PnC/AE2/IF/MA 材料都能以蜜脾产出）与基因索引系统**是本包最完整的。
14. **IF 肉链**（`bioreactor` → `meat` → `raw_ore_meat` → `fermentation_station` → `fermented_ore_meat` → `washing_factory`/`fluid_sieving_machine`）：链上每个中间体都是 IF 专有流体。GT/Mek 的矿物处理都必须以**真实矿石**为输入。**"输入形态"（生物质/肉）在本包内没有第二家**。但**产物（矿物粉）完全可替代**——所以是"招牌链"而非"卡脖子工序"。
15. **IF 流体镭射钻的实体触发**：`laser_drill_fluid/ether_gas.json` 需要世界里存在 `minecraft:wither` 实体（+ `laser_lens10`，产 10mB）。本包其他采掘（GT 虚空矿机/大型采矿机/基岩采矿机、Create 挖掘机、Mek 数字采矿机、IU 采石场）都不看实体。
16. **IF 凋灵生成器 `industrialforegoing:wither_builder`**：自动摆灵魂沙+头颅并生成凋灵 boss。GTSE/IU 的刷怪机只处理普通怪，刷怪笼不放 boss。→ **本报告认为这是本包最干净的一处独家**，且是下界之星量产的前置。
17. **IF 冷凝室 `stasis_chamber`**：清除负面状态/持续治疗，本包没有第二台作用于实体的医疗方块。
18. **IF 魔咒提取机 `enchantment_extractor`**：从物品上**反向剥离**附魔成书。本包有 Enchanting Infuser 但没有提取。**"提取"方向独家，"施加"方向不独家。**
19. **IF 灵魂体系**（`soul_laser_base` → `soul_surge` 加速机器 → `soul_network_pipe` → `souls_storage_cell_*`）：**"以灵魂为可计量燃料"的体系独家**；但**"加速机器"这个功能本身不独家**（GT 有世界加速器、Mek 有工厂升级）。加速倍率**未核实**。
20. **IF 菌丝网络产能反应堆 + 16 种菌丝发电机**：发电功能完全可替代，但**结构辨识度与"燃料种类收集"玩法在本包内唯一**。
21. **PnC 无人机 + 60 拼图编程** / **UV 曝光 + 概率蚀刻 PCB** / **方块级温度场** / **压力管网+负压**（B 组判定，详见 §1.3）。
22. **IU 太空系统（35 维度）** / **IU 4 族裂变堆** / **IC2 UU 成本表** / **IC2 玩家自排元件网格**（B 组判定，详见 §1.3）。
23. **Create 应力体系（RPM + SU 预算）**：别家的电都是"能量池"（GT EU/t、Mek FE/J、IC2 EU、IE Flux、AE）；Create 是**转速 + 应力预算的机械网络**——速度与扭矩可互换、超载停机、每台机器有 SU 消耗与最低转速。IE 也有风车/水车，但手册写明是"接一台动能发电机产 Flux"，不存在网络与扭矩权衡。**包内没有第二个旋转动力 mod。**
    id：`create:stressometer`/`speedometer`/`rotation_speed_controller`/`sequenced_gearshift`/`clutch`/`gearshift`/`gearbox`；动力源 `steam_engine`/`water_wheel`/`large_water_wheel`/`windmill_bearing`/`flywheel`/`creative_motor`；附属 `create_connected:kinetic_battery`/`brake`/`centrifugal_clutch`/`overstress_clutch`/`freewheel_clutch`/`inverted_clutch`/`kinetic_bridge`/`brass_gearbox` 族。
24. **Contraption 移动装置 + 列车**：把**已放置的方块整体组装成移动实体**并在移动中干活。别家只有方块级破坏/放置（IF Block Breaker、GT 采矿机）；IE 转盘只能原地转 90/180°。列车更进一步：站点/信号分段防撞/司机时刻表/无人区块持续运行，包内无第二个铁路 mod。
25. **鼓风机"环境/触媒定义工序"**：工序由气流穿过的方块/触媒决定，且作用于**世界中的物品实体与生物实体**，不是 GUI 输入槽；速度只影响范围。注册表 `fan_processing_type` 共 **23 种**（create 4 + dragons_plus 19）。批量染色可染**实体与装备**、批量终结（苹果→紫颂果）均无等价物。
26. **Deployer 机械手**：模拟玩家"放方块/用物品/激活方块/收割/攻击生物"，可切左键模式。别家的自动化是配方执行器（IE 装配机、GT 装配机、AE 合成）或方块放置/破坏器（IF）；**没有一个能把任意手持物品当工具对世界使用**（本包配方里就有斧头去氧化、蜂巢打蜡）。
27. **物理包裹物流**：物品变成带地址的"包裹"，靠列车/蛙港运输、网络下单、工厂仪表自动排布动力合成器。别家（AE2/Mek/IE）都是数字物流，没有"包裹"这一层。
28. **Applied Create 应力↔AE**：唯一让 AE2 存/传 SU 的东西（lang 内 `key_type.appliedcreate.stress`、`mode.export`/`mode.import`）。倍率**未核实**。

**2.3 看起来独家、实则不独家（易误判）**

- **IF 镭射钻的"虚空产矿"**：**不独家**。`gtceu:void_miner`（由 `kubejs/server_scripts/gtceu_void_miner.js` 用 lv/mv/hv_miner + 力场发生器 + 世界数据扫描仪拼装，从 ATM-9 移植）、`gtceu:*_large_miner`、`gtceu:*_bedrock_ore_miner`、`createoreexcavation:drilling_machine`/`sample_drill`/`extractor`、`mekanism:digital_miner`、IU 采石场 都在。
  **而且本包还反向增强了 IF 镭射钻**：`kubejs/server_scripts/gtceu_laser_drill.js`（priority 300）把 GTCEu 的 deepslate/netherrack 变种矿石按维度稀有度灌进它的产出表；`prosperity_laser_drill.js` 又加入 MA 的 `prosperity_shard`。→ **镭射钻在本包是"被官方认可的通用矿石入口"，是很好的"多 mod 可互换步骤"，不是独家步骤。**
- **Thermal 整合组件体系**：功能被 Mek 工厂升级、GT 机器升级、IU 升级件覆盖。唯一性在于"一个基础组件 + 20 余种目的性插件"的组合粒度——是"更细"，不是"别人不能"。
- **MA 灵魂刷怪笼 / 收获机**：功能被 gtse mob simulator、IU 刷怪笼、Create/IU 农场覆盖；独家的只是**输入形态（用精华当货币）**。
- **MA 机器本体**（再处理器/附魔器/熔炉/骨架）：都能在其他 mod 找到对应物。
- **Mek 聚变 / 裂变 / UU 物质**：三者在本包都有第二家（GT 有聚变堆；IC2、IU 有裂变；UU 有三套）。
- **Create Ore Excavation 钻井不是独家**：能力上与 GT `void_miner`/大型采矿机、Mek `digital_miner`、IF 激光钻、IU 量子采石场、IC2 采矿机、IE 挖掘机同级。独家点仅在"旋转驱动 + 钻头/钻浆消耗 + 矿脉枯竭 + 本包已把 GT/IU 矿脉接进来"。
- **IE 几乎全可替代**：只有三条勉强算独家，且**全部未核实**（电弧炉装备回收 / 挖掘机探矿 / 避雷针，理由原文见 §1.4）。注意 IE 的"外露接线 / 电压分级 / 线烧断"是**表现独家、能力等价物遍地**。
- **Create 的机械臂、传送带、压床、搅拌器、粉碎轮、风车、蒸汽引擎**：均可替代（见 §3）。**区块加载是准独家**——AE2 `spatial_anchor` 也能加载区块（AE2 源码 `SpatialAnchorBlockEntity` 走 `appeng.server.services.ChunkLoadingService`），只是"旋转门控 + 可跟随移动装置 + 可挂列车站点"只有它。
- **Create 蓝图加农炮 / 滚筒**：是否真有等价物**未核实**。

---

## 3. 可替代工序（多 mod 互换候选）

| 工序类别 | 本包内的可替代者 |
|---|---|
| 粉碎研磨 | `mekanism:crusher`/`enrichment_chamber` ↔ `thermal:machine_pulverizer`(97) ↔ GT 研磨/粉碎 ↔ IC2 打粉机 ↔ IE 粉碎机 ↔ Create 粉碎轮 |
| 分离离心 | `mekanism:…` ↔ `thermal:machine_centrifuge`(69) ↔ `productivebees:centrifuge` ↔ GT 离心机 ↔ IC2 提取机 |
| 高温熔炼/合金 | `mekanism:energized_smelter` ↔ `thermal:machine_smelter`(102)/`machine_furnace` ↔ `industrialforegoing:resourceful_furnace` ↔ GT 合金炉/电弧炉 ↔ IE 电弧炉 ↔ Create 混合熔炼 |
| 压制成型 | `mekanism:precision_sawmill`/`combiner` ↔ `thermal:machine_press`(**293**) ↔ GT 冲压/弯曲 ↔ IE 金属压床 ↔ Create 压床 ↔ `mekmm:cnc_stamper` |
| 切割锯解 | `thermal:machine_sawmill`(12) ↔ GT 锯床 ↔ Create 锯 ↔ IE 锯木机 |
| 木材/焦油热解 | `thermal:machine_pyrolyzer`(3) ↔ IE 焦炉 ↔ GT 热解炉 |
| 流体精炼/分馏 | `thermal:machine_refinery`(5) ↔ `immersiveengineering:refinery` ↔ `pneumaticcraft:refinery` |
| 冷却/凝固 | `thermal:machine_chiller`(10) ↔ GT 流体固化/冷凝（具体机器 id 未核实） |
| 熔融/岩浆 | `thermal:machine_crucible`(14) ↔ Agradditions `molten_*` ↔ GT 熔炉流体模式 ↔ IE 熔炉 ↔ Mek 熔化器 |
| 造石 | `thermal:device_rock_gen`(6) ↔ `industrialforegoing:material_stonework_factory`(6) ↔ GT 造石机（未核实 id） ↔ Create 石料生成 |
| 蓄水 | `industrialforegoing:water_condensator` ↔ `thermal:device_water_gen` ↔ GT 水塔/抽水机 ↔ IE 抽水泵 ↔ PnC 液体泵 |
| 树汁/树脂 | `thermal:device_tree_extractor`(8) ↔ IF 乳胶线 ↔ IC2 橡胶 ↔ GT 橡胶（本包已把橡胶挂到 `forge:ingots/rubber`） |
| 钓鱼 | `industrialforegoing:marine_fisher` ↔ `thermal:device_fisher` |
| 堆肥 | `industrialforegoing:sewage_composter` ↔ `thermal:device_composter` ↔ GT 堆肥（未核实） |
| 药水 | `industrialforegoing:potion_brewer` ↔ `thermal:machine_brewer`/`device_potion_diffuser` |
| 附魔施加 | `industrialforegoing:enchantment_applicator`/`enchantment_factory` ↔ `enchantinginfuser` |
| 生物掉落刷取 | `industrialforegoing:mob_crusher`/`mob_slaughter_factory` ↔ MA `soulium_spawner`(19) ↔ gtse mob simulator ↔ IU 刷怪笼 |
| 农牧收割 | MA `harvester` ↔ IF `plant_gatherer`/`hydroponic_bed` ↔ gtse 收获机 ↔ Create 收割 ↔ IU 农场 ↔ IC2 作物架 |
| 发电 | Thermal 7 台 dynamo ↔ `industrialforegoing:biofuel_generator`/`mycelial_*` ↔ `productivebees:honey_generator` ↔ GT 发电机全系 ↔ Mek 发电机 ↔ IE 发电机 |
| 能量/流体/物品缓冲 | `thermal:energy_cell`/`fluid_cell`/`item_buffer` ↔ IF 黑洞系列 ↔ GT 超能罐/储罐 ↔ AE2 存储 ↔ Mek 量子储罐 |
| 物流 | `industrialforegoing:conveyor`/`transporter` ↔ `thermal:item_buffer` ↔ GT 传送带 ↔ Create 传送带 ↔ IE 传送带 ↔ Mek 物流管道 |
| 采矿/钻探 | IF `laser_drill`+`ore_laser_base`(35 条) ↔ `gtceu:void_miner`/`*_large_miner`/`*_bedrock_ore_miner` ↔ `createoreexcavation:drilling_machine` ↔ `mekanism:digital_miner` ↔ IU 采石场 ↔ GTCA 太空采矿 |
| 电路制造 | AE2 `inscriber` ↔ `expatternprovider:circuit_cutter` ↔ `mekmm:cnc_stamper`（**本包已实证的三方互替**） |
| 电解分离 | Mek 电解分离器（出气）↔ GT/IU（出流体）——形态不同但功能可替代 |
| 化学灌注 | `mekanism:chemical_infuser`（气+气）↔ GT 化学反应釜（液向） |
| 机器强化 | Thermal 整合组件 + 19 插件 ↔ PB 13 升级 ↔ Mek 工厂升级 ↔ GT 机器升级 ↔ IU 升级件 |
| 传送/跨维度 | `mekanism:quantum_entangloporter` ↔ `industrialupgrade:tesseract`；`mekanism:teleporter` ↔ IC2/IU 传送；`mekanism:dimensional_stabilizer` ↔ `create_power_loader` 4 个加载器 |
| 网络/存储 | Mek QIO ↔ AE2 元件 + 无线终端（QIO 免布线，AE2+MEGA 容量更高） |

**不宜列入可替代的（容易被当成可替代但其实断链）**：`mekanism:thermal_evaporation_*`（水→盐水→液锂三级温度链，GT/IU 是否有等价蒸发塔**未核实**）、`mekanism:resistive_heater`、Mek `pigment_*` 三件套。

---

## 4. 硬坑清单（写错就废）

**4.1 id 不存在 / 已被改道**

| 想写的东西 | 实际情况 |
|---|---|
| `industrialupgrade:iufluiduu_matter`、`iufluidcoolant` | **已被删除**，iuunify 重定向到 `ic2_120:*`。涉及 UU/冷却液只能写 `ic2_120:uu_matter` / `ic2_120:coolant` 或 GT 的 `gtceu:uu_matter` / `gtca:uu_matter_amplifier`。IU 存活的流体一律 `industrialupgrade:iufluid*` |
| `mekmm:scrap` | 已废弃，改产 `ic2_120:scrap` |
| `mekmm:cnc_lathe`、`mekmm:cnc_rolling_mill`、`mekmm:ambient_gas_collector` | 方块仍注册但配方已删、JEI 隐藏。在 JEI 里看到它们是隐藏脚本的残留 |
| `thermal:dynamo_steam` | **不存在**。只有 stirling/compression/magmatic/numismatic/lapidary/disenchantment/gourmand 7 台 |
| `thermal:device_heat_sink`、`device_air_pump` | **不存在** |
| `thermal:device_arboreal_extractor` | 真名是 `thermal:device_tree_extractor` |
| `industrialforegoing:supreme_machine_frame` | **不存在**，正确形式是 `machine_frame_supreme`（`machine_frame_<级>`） |
| `industrialforegoing:mycelial_pink_slime` | **不存在**，正确是 `mycelial_pink` |
| `industrialforegoing:laser_lens1`…`laser_lens15` | 是 `laser_lens0`…`laser_lens15`，**从 0 起** |
| `productivebees:upgrade_time_1`…`_4` | **不存在后缀**，速度升级就是 `productivebees:upgrade_time` |
| `mysticalagradditions:awakened_supremium_apple` | **不存在** |

**4.2 IU 的 lang 文件里有未注册的方块键**（看着像 id，用了就废）
`basemachine3/chamber_iu`、`heat_limiter`、`crop_harvester_iu`、`crop_matron`、`solid_canner_iu`、`research_table`、`{avaritia,botania,draconic,thaumcraft}panel` 太阳能板、`basemachine1/{adv,imp,per}_reactor`。

**4.3 望文生义会写错的**
- `basemachine3/moon_spotter` 中文是"**夜间填充器**"，不是月球探测器。
- `basemachine3/centrifuge` 是"**蜜蜂产品**离心机"，通用那台是 `moremachine3/centrifuge_iu`。
- `gtca:thermal_reactor` 是**高温化学反应器**（吃浮选泡沫浆料），**不是核堆**。

**4.4 机制上的坑**
- **本包核爆已关**：`config/ic2_120.json` 里 `enableReactorExplosion: false`，`enableOvervoltageExplosion: false`。想让"核风险"当某一步的代价——它不存在。只剩 >4000 起火 / >7000 伤害 / ≥10000 熔毁的判定，不炸家。
- `pylons:*_pylon` **没有任何能量接口**，别在产线里给它们算电。
- `pneumaticcraft:drone_redstone_emitter` **只有方块没有物品**，写不进配方。
- **IU 的机器配方查不到 kubejs 导出**：`export/recipes/industrialupgrade/` 那 2835 个文件是它自己的工作台配方，机器配方在 Java 里。排步骤时要查"IU 这台吃什么"，得查源码或它自带指南书。
- **`soulplied_energistics_backport_...jar` 的真实 modId 是 `industrialforegoingsouls`**，显示名 "Industrial Foregoing Souls"——**它不属于 AE2 系**，与 "energistics" 字样无关。
- **JEMB 配置里的 `better_fusion_reactor` / `mekanism_extras` 两段是空配置**（mod 未装，注册表 0 条目）。不要被误导成"包里有更好的聚变堆/强化感应矩阵"。

**4.5 命名撞车但物质不互通**
Mek 有钋（`mekanism:polonium`，**气体**），IU 也有自己的一整套 polonium（矿石/锭/粉/**熔融流体**，`industrialupgrade:baseore2/polonium` 等）——**两者不互通**。

---

## 5. 未核实清单（不要直接写进设计稿）

1. **mekmm 版本差**：大型机结构尺寸结论来自源码 1.2.2，运行 jar 是 1.2.1。id 以运行时为准，"大型机多大、怎么搭"待实测。
2. **AE2 源码是 `fix-8329` 分支**，仅用于行为解释；id 与配置以运行时 15.4.10 为准。
3. **GT/IU 侧只做了命名空间级扫描**：`thermal_evaporation` 是否有等价物、`resistive_heater` 是否独家，未核实。
4. **`ae2:mysterious_cube` 获取方式未核实**，不宜作为步骤。
5. **`ae2:crystal_resonance_generator` 无 zh_cn 名**，入表需补。
6. **IE 的三条"独家"全部未核实**：电弧炉装备回收、挖掘机探矿、避雷针。
7. **C 组未核实项**：Applied Create 的倍率与 AE/t 数值（资料有 1–16× / 1–256× 两种说法，只采信"方向"）；IE 电弧炉回收的具体产出规则（jar 内是代码生成的 `generated_list`）；Create 蓝图加农炮 / 滚筒是否有等价物；Thermal 光合机在本包的 id；GT `*_world_data_scanner` 的实际行为；`create_connected:inventory_bridge` / `createaddition:portable_energy_interface` / `create_dragons_plus:dragon_breath_cauldron` / IE 热传导发电机 是否有等价物。C 组本次**未反编译**，能力结论取自各 mod jar 内的 lang/ponder 文本与 IE 自带手册。
8. **IF 与 Thermal 的官方英文名未对照 en_us 原文**（tools\src 下无这两家源码），表中英文名是按 registry 路径转写。中文名来自 i18n 包，可靠。
9. **`industrialforegoingsouls` / `ifsoulsdisks` 无 zh_cn**：文中"灵魂镭射基座/灵魂涌动器/灵魂网络管道/灵魂存储元件"是自译，不是官方译名。**IF Souls 的加速倍率未重新验证。**
10. **MA 灵魂瓶的用途存疑**：`recipes/` 全目录搜 `filled_soul_jar` **0 命中**，所以"装满灵魂瓶参与合成"在本包可能未启用。
11. **IU 蜜蜂的具体能力未核实**，因此"蜜蜂形态是否独家"无定论。
12. 标注"配方未逐条核对"的 IF 机器（洗矿厂、流体筛分机、乳胶加工机等）只有"机器存在 + 配方类型存在"两项证据，**具体输入输出未逐条读过**。
13. **Agradditions 6 个 crux 与 6 套种子/精华的配方可用性未逐条核实**（注册表命中已确认）。
14. 本次四路盘点**没有打开任何 jar、没有反编译**，所有 id 均来自 kubejs 导出目录。

---

## 附：现成的"成本"量化基础

你提到"通过成本来设计"。本包已有一份量化过的成本表：

- `config/ic2_120.json` → `uuReplication.replicationWhitelist`：**423 条**手工定价，格式「物品 id → 需要多少 uB」，跨 mod（minecraft 296 / industrialupgrade 54 / gtceu 32 / ic2_120 27 / emendatusenigmatica 14）。
- 外层还有 **uubridge 0.1.3** 的运行时 Bellman-Ford：遍历全包配方给**所有**物品算动态价。

→ **IC2 的 UU 成本表是本包事实上的"物质复制标准"。**
