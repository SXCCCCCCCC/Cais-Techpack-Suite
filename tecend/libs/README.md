# libs/

上游依赖 jar 的落脚处。**`libs/*.jar` 不进 git**（合计约 160M），需要时重跑
`bash scripts/collect-libs.sh` 从整合包收集（脚本里写死了 MODS_DIR，可用环境变量覆盖）。

## 两条拿 jar 的路线（别搞混）

| 上游类型 | 拿哪里的 jar | build.gradle 里怎么写 |
|---|---|---|
| Forge 系（GTCEu / IE / IU / Mek / Create / IF / Thermal / PNC / AE2 / MA / 蜜蜂 / 超越维度 / Pylons / 懒人厨房 / CreateOE / GTCA / titanium / cofh_core） | 整合包 `mods/` 里的原始 jar | `compileOnly fg.deobf(files(...))` |
| Fabric 系（**只有 IC2 Refabricated**，经 Sinytra Connector 运行） | **`mods/.connector/industrialcraft2-refabricated-1.20.1-fabric.0.6_mapped_srg_1.20.1.jar`** —— 不是 `mods/` 里的原始 jar | `compileOnly fg.deobf(files(...))` |

理由：`mods/` 里的 IC2 原始 jar 是 intermediary 命名（`class_1799`），`fg.deobf` 遇它会直接编译报错；
而 `.connector/` 下那个是 Connector 重映射缓存，已经是「mojmap 类名 + SRG 成员名」，
与 Forge mod 生产 jar 同形（javap 实证：`Method net/minecraft/core/DefaultedRegistry.m_6612_`），
而且它就是**运行时的真身**。IC2 或 Connector 升级后要重新取一份。

## jarjar/

GTCEu 把 LDLib 和 MixinExtras 压在自己的 `META-INF/jarjar/` 里，javac 看不到嵌套 jar 的类
（SOP §5 记过这个坑），所以要单独解出来放 `libs/jarjar/` 供编译期解析。运行时不冲突 ——
那两样由 gtceu 自己的 jarjar 展开提供。

## 查 API 用哪里的源码

按 SOP：**先读 `D:\Projects\游戏\Minecraft\tools\src\` 里已有的源码参照，不要对已有的用 javap/vineflower**。

| 上游 | 源码参照 |
|---|---|
| GTCEu | `tools/src/GregTech-Modern-1.20.1`（注意：那是 8.0.0 源码，包内跑的是 7.5.3，查包内行为以 `mods/gtceu jar` 为准） |
| GTCA | `tools/src/GTCA-7.5.x` |
| IC2 Refabricated | `tools/src/ic2-120-0.6-runtime-src/`（vineflower 解的重映射运行时 jar，**0.6 运行时真相源**）、`tools/src/ic2-fabric-main` |
| IU | `tools/src/iu-3.4.0.10-runtime-src/`、`tools/src/industrialupgrade-1-20-1-dev` |
| Mekanism | `tools/src/Mekanism-release-1.20.x` |
| PNC:R | `tools/src/pnc-repressurized-1.20.1` |
| AE2 | `tools/src/Applied-Energistics-2-fix-8329-forge-1.20.1` |
| 神秘农业 | `tools/src/MysticalAgriculture-1.20`、`tools/src/MysticalAgradditions-1.20` |
| 资源蜜蜂 | `tools/src/productive-bees-dev-1.20.0` |
| IE / Create / Thermal / IF / 超越维度 / Pylons / 懒人厨房 | 没有源码参照 → 用 IDE 反编译，或 `javap` 对应 jar（目标 jar 的类名/成员名以 jar 为准，不是以 GitHub 上的源码为准） |

## 几条实证结论（省得再查一遍）

- IC2 的类在 `ic2_120.*`：机器方块在 `ic2_120.content.block.*`（如 `DirectionalMachineBlock`、`AdvancedMachineCasingBlock`），
  对应方块实体是同级/子包的 `*BlockEntity`（实测 `CropBlockEntity`、`ComposeDebugBlockEntity` 等）；
  **没有公开的 `ic2.api` 包**，能量是 TR EnergyStorage（事务制 long）。
  跨 mod 交互要直接引用 Kotlin 类或反射，注意 Kotlin getter 名是 `getXxx`。
  机器名（如洗矿场）用 `unzip -l` 列 `ic2_120/content/block/` 找，别去 `assets/` 里翻（那里只有模型和 lang）。
- GTCEu 的配置门面：`com.gregtechceu.gtceu.api.GTCEuAPI.isHighTier()`；常量表 `GTValues`。
- **包内 KubeJS 会注册机器**：`gtceu:void_miner` 与 `world_data_scanner` 就是
  `kubejs/startup_scripts/gtceu_void_miner.js` 用 `KJSWrappingMachineBuilder`/`createKJSMulti` 注册的
  （从 ATM9 移植），不是任何 mod 的机器 —— 这类机器在 Java 侧没有类可引用，配方与产出走 KubeJS。
  查设计流程里的机器时，先 `grep -ril <名字> kubejs/`，再去 jar 里找。
