# Cai's Techpack Suite

`1.20.1-Tec` 整合包配套的自研 Minecraft mod 工程合集。
A collection of hand-written Minecraft mods built for the `1.20.1-Tec` modpack.

**中文** ｜ [English](#english)

---

# 中文

## 这是什么

本仓库是 `1.20.1-Tec` 整合包配套的一批自研 mod。整合包以 **GregTech CEu Modern** 为主线，
把 **Industrial Upgrade**、**IC2 Refabricated**、**Industrial Foregoing**、**Mekanism**、
**Immersive Engineering**、**Thermal Series**、**Create** 等多条科技线接到同一个世界里，
并借 **Sinytra Connector** 让 Fabric 侧 mod 与 Forge 侧共存。

跨这么多 mod 会暴露大量上游 bug 与互不兼容；每条科技线又要统一材料与流体口径。
本仓库就是这些工作的产物——**26 个修复 / 桥接 mod、1 个内容 mod、1 个 addon、1 个翻译工程**。

设计原则有两条，贯穿全部工程：

1. **只调上游公开 API，或做 mixin 注入，不复制上游代码。** 上游里 GTCEu / GTCA 是 LGPL、
   Industrial Upgrade 是 AGPL，抄码会把 copyleft 拖进本仓库。
2. **尽量不改原版 Minecraft 类。** 原版类被 Forge 补丁和众多第三方 mixin 共同依赖，
   改动风险高、跨版本易碎；确需修改时先确认 Mojira 上有对应报告。

## 环境

| 项 | 版本 |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10 |
| Java | **17**（构建时必须显式指定，见下） |
| Gradle | 8.8（各工程带 wrapper） |
| Sinytra Connector | 1.0.0-beta.49（部分工程依赖其桥接行为） |

## 目录结构

```
Cais-Techpack-Suite/
├── <工程名>/            每个目录是一个独立 Gradle 工程，可单独 build
└── thermal_guide_zhcn/  Thermal Series 指南书简体中文翻译（资源包形态，非 mod）
```

## 工程清单

### Industrial Upgrade 系列

Industrial Upgrade（作者 Denfop）是 IC2 的精神续作，本包把它接回 IC2 与 GT 的双重语境。

| 工程 | 版本 | 用途 |
|---|---|---|
| `iufix` | 1.9.7 | IU 修复合集，27 个 mixin：污染 debuff 屏蔽、世界生成 NPE、镐挖等级门槛、WorldEdit 状态往返、多胞方块碰撞与铁砧朝向、Jade 数据守卫、分子重组机 GUI NPE、太空组装机 AE 自动化等 |
| `iuunify` | 0.5.7 | IU × IC2 流体统一：钚换源、分子重构机配方配置化、输入匹配 |
| `iuspeed` | 1.5.0 | IU 配方机提速：电编程台 25×、太空系统 10× |
| `iuchancefix` | 1.0.0 | 矿物分离机煤粉合金配方确定性化（chance 75 → 100），使 AE2 能编码确定样板 |
| `uubridge` | 0.1.3 | IU × IC2 的 UU 链桥：晶体记忆双向读写、UU 成本按族内最小值归一化 |
| `iudebug` | 1.2.1 | IU **破坏性**调试工具：按 config 取消指定注册；默认全关 |

### GregTech 系列

| 工程 | 版本 | 用途 |
|---|---|---|
| `gtjeimbfix` | 1.0.0 | GTCEu 多方块 JEI 信息页悬停抛 `IndexOutOfBoundsException`——槽名反查的 flat widget 下标在渲染期已过期 |
| `gtnoboomfix` | 1.0.0 | 关闭 GTCEu 全部爆炸与伤害机制（本整合包的设计决定，非上游 bug） |
| `creativetabfix` | 1.0.0 | GTCA 物品不进任何创造页：`GTRegistrate` 从不安装 `DisplayItemsGenerator`。用 Forge 公开事件实现，零 mixin、不引用 GTCEu 类型 |

### IC2 / Mekanism / Immersive Engineering 系列

| 工程 | 版本 | 用途 |
|---|---|---|
| `ic2reactorhopperfix` | 1.0.0 | IC2 核反应堆仓与接口的物品存储事务快照是空实现且 `setStack` 拒绝后仍计位移 → 模组物流插错位、丢物 |
| `ic2mekradfix` | 1.2.0 | IC2 × Mekanism × GregTech 三家防辐射服互通，各自闸门逐点补判 |
| `mekmmuufix` | 1.0.0 | 让 mekmm 的复制机 / 流体复制机 / 工厂走 IC2 的 UU 成本体系 |
| `iespawnfix` | 1.0.0 | IE 刷怪抑制设备在 Forge 47.4 上对已入世界的实体调 `setSpawnCancelled` 抛异常 → 崩服 |

### Industrial Foregoing / Create 系列

| 工程 | 版本 | 用途 |
|---|---|---|
| `iffermentfix` | 1.0.0 | IF 发酵站 `canIncrease` 不查流体身份，换矿种时静默丢弃产出而输入照扣 |
| `createindustrialforegoingfluidfix` | 1.0.0 | Create 软管滑轮把 IF 流体当作 flowing 变体抽取：`FluidHelper` 只特判水 / 岩浆 / `ForgeFlowingFluid` |

### 流体统一

| 工程 | 版本 | 用途 |
|---|---|---|
| `fluidunifyapi` | 0.1.0 | 可配置的「机器输入流体」API，覆盖 IC2 Refabricated、Industrial Foregoing、Industrial Upgrade。**只改输入侧定义**（配方表 / 谓词 / 判定点），输出侧、流体身份与 Forge `FluidTank` 混液语义零接触 |

### Connector / 前置库系列

| 工程 | 版本 | 用途 |
|---|---|---|
| `ae2ipnswipefix` | 1.0.0 | IPN 的容器 swipe 在 AE2 终端会误触第一张升级卡：AE2 的客户端专用槽绕过 `addSlot`，`Slot.index` 恒为 0 |
| `cofhlightfix` | 1.0.0 | CoFH Core 瞬时光源客户端 NPE：`TransientLightManager.tick` 第二个循环缺空守卫 |
| `constructionstickfix` | 1.0.0 | Construction Sticks 资源重载 NPE：`StickUtil.getAllUpgrades` 的静态缓存无同步，并发拷贝会读到未发布的数组槽 |
| `fabrictransferfix` | 1.0.0 | Forgified Fabric API 的 capability 缓存是无同步 `HashMap`，并发查询抛 `ConcurrentModificationException` |

### 数据层 / 小 mod 修复

| 工程 | 版本 | 用途 |
|---|---|---|
| `kjsexportcmd` | 1.1.0 | `/kjsexport` 导出命令，并在 `TagsUpdatedEvent` 时机注册 GT 全局 5 倍速 |
| `oeikjsfix` | 0.1.0 | OneEnoughItem 的替换文件路径重定向到 `kubejs/data`，使游戏内编辑器改的是真正生效的数据包 |
| `mafgllibfix` | 1.0.0 | MaFgLib 在 Forge 并行加载下的 `ArrayList` 损坏，以及 config 加载时机早于回调构造 |
| `tweakergammafix` | 1.0.0 | Tweakerge gamma 覆写重启后失效；`Options.save()` 的 codec 拒绝 16.0 |
| `oeffix` | 0.1.1 | OneEnoughFluid 的 reload-override NPE（先打日志后赋值） |
| `macobaltfix` | 1.1.0 | 绕过 MA 的作物 mod 门控，并把材料 provider 换成 GTCEu 物品 |

### 内容 mod

| 工程 | 版本 | 用途 |
|---|---|---|
| `tecend` | 0.1.0 | 终局奖杯 mod：15 步主线，从残片到金奖杯，需要 GTCEu / IE / IU / IC2 / Mek / Create / AE2 / PnC / Thermal / MA 十家的能量与工序 |
| `maaddon` | 0.1.0 | Mystical Agriculture addon：盐 / 塑料 / 粉黏液三种作物，配方对齐 Industrial Agriculture |

### 翻译

| 工程 | 用途 |
|---|---|
| `thermal_guide_zhcn` | Thermal Series 帕秋莉指南书简体中文翻译。产物在 `out/`，以资源包形态分发 |

## 构建

**必须显式指定 JDK 17**——若系统默认 JDK 较新（如 25），Gradle 8.8 会报
`Unsupported class file major version 69`：

```bash
cd <工程名>
JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew.bat build
```

产物在 `build/libs/`。

## 依赖

各工程把上游 mod 作为**编译期依赖**（`compileOnly`）放在 `libs/` 下。
这些 jar **不进仓库**——它们属于各自的作者，且部分带 copyleft 许可，再分发会造成许可问题。

构建前需自行收集：从整合包实例的 `mods/` 目录拷贝对应 jar 到 `libs/`。
`tecend/libs/README.md` 记录了它所需的 22 个上游 jar 与收集脚本（`tecend/scripts/collect-libs.sh`）。

> 注意：`tecend` 需要 IC2 的**重映射版**（Connector 生成的 `mods/.connector/` 下那份），
> 而不是 `mods/` 里的原始 jar。

## 许可

本仓库是**多个独立工程的合集**，不是一个单一作品。各工程适用其目录下 `LICENSE` 文件所载的许可：

| 范围 | 许可 |
|---|---|
| 除下列例外外的全部工程 | **MIT** |
| `tecend/` | **LGPL-3.0**（`LICENSE.GPL-3.0` 是 LGPL-3.0 正文引用到的 GPL-3.0 副本） |
| `maaddon/` 中的贴图 | **LGPL-2.1**，来自 Industrial Agriculture；详见该目录 `THIRD_PARTY_NOTICES.md` 与 `LICENSE.LGPL-2.1` |
| `thermal_guide_zhcn/` | **CC BY-NC-SA 4.0** |

`thermal_guide_zhcn/` 是 Thermal Series 指南书的翻译作品，原文权利归 Team CoFH 所有
（其许可为 "CoFH - Don't Be A Jerk"）。该翻译以**资源包形态**分发：只新增译文文件，
运行时覆盖显示，不修改也不重分发原 mod；使用者需自行安装原 mod。

## 已知情况

- 部分工程针对 **Sinytra Connector** 环境（Fabric 侧 mod 跑在 Forge 上）特有行为，
  上游未必视之为 bug（如 `constructionstickfix`、`fabrictransferfix`、`ae2ipnswipefix`）。
- 本仓库中的修复对应的是**特定上游版本**。上游若已自行修复，相应工程即失去意义；
  各工程 README / 注释中尽量注明了所针对的版本。
- 本仓库发布时锁定的上游版本已记录在各工程的 `build.gradle`。

---

# English

## What this is

This repository holds a set of hand-written Minecraft mods for the `1.20.1-Tec` modpack.

The pack centres on **GregTech CEu Modern** and wires several other tech lines into one world:
**Industrial Upgrade**, **IC2 Refabricated**, **Industrial Foregoing**, **Mekanism**,
**Immersive Engineering**, **Thermal Series** and **Create** — using **Sinytra Connector**
to let Fabric-side mods coexist with Forge ones.

Sharing a world between that many mods exposes a large number of upstream bugs and
incompatibilities, and the tech lines also need their materials and fluids reconciled.
This repository is the result: **26 fixes / bridges, 1 content mod, 1 addon and 1 translation project**.

Two principles run through everything here:

1. **Call upstream public APIs, or inject via mixins. Never copy upstream code.**
   GTCEu / GTCA are LGPL and Industrial Upgrade is AGPL upstream; copying would pull
   copyleft into this repository.
2. **Avoid modifying vanilla Minecraft classes.** Vanilla classes are relied on by Forge
   patches and many third-party mixins alike; changes there are risky and fragile across
   versions. Where a change is genuinely needed, a Mojira report is confirmed first.

## Environment

| Item | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.4.10 |
| Java | **17** (must be selected explicitly when building) |
| Gradle | 8.8 (wrapper included per project) |
| Sinytra Connector | 1.0.0-beta.49 (some projects depend on its bridging behaviour) |

## Layout

```
Cais-Techpack-Suite/
├── <project>/           each directory is a standalone Gradle project
└── thermal_guide_zhcn/  Simplified-Chinese translation of the Thermal Series guidebook
                         (shipped as a resource pack, not a mod)
```

## Projects

### Industrial Upgrade

Industrial Upgrade (by Denfop) is a spiritual successor to IC2; this pack reconnects it
to both IC2 and GregTech.

| Project | Version | Purpose |
|---|---|---|
| `iufix` | 1.9.7 | IU fix collection, 27 mixins: pollution debuff suppression, worldgen NPE, harvest-tier gate, WorldEdit state round-trip, multi-cell collision and anvil facing, Jade data guards, molecular transformer GUI NPE, space assembler AE automation, and more |
| `iuunify` | 0.5.7 | IU × IC2 fluid unification: plutonium sourcing, configurable molecular recipes, input matching |
| `iuspeed` | 1.5.0 | IU machine speed multipliers: electric workbench 25×, space systems 10× |
| `iuchancefix` | 1.0.0 | Makes the mineral separator's coal-dust alloy recipe deterministic (chance 75 → 100) so AE2 can encode a stable pattern |
| `uubridge` | 0.1.3 | IU × IC2 UU chain bridge: bidirectional crystal memory, UU cost normalised to the family minimum |
| `iudebug` | 1.2.1 | **Destructive** IU debug tool: cancels selected registrations via config; off by default |

### GregTech

| Project | Version | Purpose |
|---|---|---|
| `gtjeimbfix` | 1.0.0 | GTCEu multiblock JEI info page throws `IndexOutOfBoundsException` on hover — the flat-widget index baked from slot names goes stale at render time |
| `gtnoboomfix` | 1.0.0 | Disables all GTCEu explosion and damage mechanics (a pack design decision, not an upstream bug) |
| `creativetabfix` | 1.0.0 | GTCA items land in no creative tab: `GTRegistrate` never installs a `DisplayItemsGenerator`. Implemented purely with a documented Forge event — no mixins, no GTCEu types referenced |

### IC2 / Mekanism / Immersive Engineering

| Project | Version | Purpose |
|---|---|---|
| `ic2reactorhopperfix` | 1.0.0 | IC2 reactor chamber / access hatch expose a no-op item-storage snapshot, and `setStack` still counts movement after refusal → modded logistics mis-insert and lose items |
| `ic2mekradfix` | 1.2.0 | Radiation-suit interoperability between IC2, Mekanism and GregTech, each gate guarded individually |
| `mekmmuufix` | 1.0.0 | Routes mekmm's replicator, fluid replicator and factories through IC2's UU cost system |
| `iespawnfix` | 1.0.0 | IE spawn-interdiction devices call `setSpawnCancelled` on entities already added to the world on Forge 47.4 → server crash |

### Industrial Foregoing / Create

| Project | Version | Purpose |
|---|---|---|
| `iffermentfix` | 1.0.0 | IF fermentation station's `canIncrease` ignores fluid identity, so switching ore types silently voids output while still consuming input |
| `createindustrialforegoingfluidfix` | 1.0.0 | Create hose pulleys drain IF fluids as the flowing variant: `FluidHelper` only special-cases water, lava and `ForgeFlowingFluid` |

### Fluid unification

| Project | Version | Purpose |
|---|---|---|
| `fluidunifyapi` | 0.1.0 | A configurable *machine fluid-input* API covering IC2 Refabricated, Industrial Foregoing and Industrial Upgrade. It only changes input-side definitions (recipe tables, predicates, decision points) and never touches output, fluid identity, or Forge `FluidTank` mixing semantics |

### Connector / library mods

| Project | Version | Purpose |
|---|---|---|
| `ae2ipnswipefix` | 1.0.0 | IPN's container swipe grabs the first upgrade card in AE2 terminals: AE2's client-side slots bypass `addSlot`, leaving `Slot.index` at 0 |
| `cofhlightfix` | 1.0.0 | CoFH Core client NPE on transient lights: the second loop in `TransientLightManager.tick` lacks a null guard |
| `constructionstickfix` | 1.0.0 | Construction Sticks NPE on resource reload: the static cache in `StickUtil.getAllUpgrades` is unsynchronised, so a concurrent copy can read an unpublished array slot |
| `fabrictransferfix` | 1.0.0 | Forgified Fabric API's capability cache is an unsynchronised `HashMap`; concurrent queries throw `ConcurrentModificationException` |

### Data layer / small mod fixes

| Project | Version | Purpose |
|---|---|---|
| `kjsexportcmd` | 1.1.0 | `/kjsexport` command, plus GT global 5× speed registration at `TagsUpdatedEvent` |
| `oeikjsfix` | 0.1.0 | Redirects OneEnoughItem's replacement file paths to `kubejs/data` so the in-game editor edits the data pack that actually wins |
| `mafgllibfix` | 1.0.0 | MaFgLib `ArrayList` corruption under Forge's parallel mod loading, and config loading ordered after callback construction |
| `tweakergammafix` | 1.0.0 | Tweakerge gamma override lost after restart; `Options.save()` codec rejects 16.0 |
| `oeffix` | 0.1.1 | OneEnoughFluid reload-override NPE (logging before assignment) |
| `macobaltfix` | 1.1.0 | Bypasses MA's mod gating for certain crops and switches their material provider to GTCEu items |

### Content mods

| Project | Version | Purpose |
|---|---|---|
| `tecend` | 0.1.0 | Endgame trophy mod: a 15-step main line from fragments to the gold trophy, drawing on the energy and processing APIs of ten upstream tech mods |
| `maaddon` | 0.1.0 | Mystical Agriculture addon: salt, plastic and pink slime crops, with recipes mirroring Industrial Agriculture |

### Translation

| Project | Purpose |
|---|---|
| `thermal_guide_zhcn` | Simplified-Chinese translation of the Thermal Series Patchouli guidebook. Output lives in `out/`; distributed as a resource pack |

## Building

**JDK 17 must be selected explicitly.** With a newer default JDK (e.g. 25),
Gradle 8.8 fails with `Unsupported class file major version 69`:

```bash
cd <project>
JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew.bat build
```

Artifacts land in `build/libs/`.

## Dependencies

Each project declares its upstream mods as **compile-time dependencies** (`compileOnly`)
under `libs/`. Those jars **are not committed** — they belong to their respective authors,
and some are copyleft, so redistributing them would create licensing problems.

To build, collect them yourself by copying the matching jars from the modpack instance's
`mods/` directory into `libs/`. `tecend/libs/README.md` documents the 22 upstream jars it
needs and ships a collection script (`tecend/scripts/collect-libs.sh`).

> Note: `tecend` needs the **remapped** IC2 jar — the one Connector generates under
> `mods/.connector/`, not the original jar in `mods/`.

## Licensing

This repository is a **collection of independent projects**, not a single work.
Each project is governed by the license in its own `LICENSE` file:

| Scope | License |
|---|---|
| Every project except the ones below | **MIT** |
| `tecend/` | **LGPL-3.0** (`LICENSE.GPL-3.0` is the GPL-3.0 copy referenced by LGPL-3.0) |
| Textures inside `maaddon/` | **LGPL-2.1**, from Industrial Agriculture; see that directory's `THIRD_PARTY_NOTICES.md` and `LICENSE.LGPL-2.1` |
| `thermal_guide_zhcn/` | **CC BY-NC-SA 4.0** |

`thermal_guide_zhcn/` is a translation of the Thermal Series guidebook; copyright in the
original text belongs to Team CoFH (licensed under "CoFH - Don't Be A Jerk"). The
translation is distributed as a **resource pack**: it only adds translated files that
override display at runtime, and neither modifies nor redistributes the original mod.
Users must install the original mod themselves.

## Known caveats

- Some projects target behaviour specific to the **Sinytra Connector** environment
  (Fabric-side mods running on Forge); upstream may not consider these bugs
  (e.g. `constructionstickfix`, `fabrictransferfix`, `ae2ipnswipefix`).
- Each fix targets a **specific upstream version**. If upstream has since fixed the issue,
  the corresponding project becomes unnecessary; the targeted version is noted in each
  project's README or source comments where possible.
- The upstream versions this repository was released against are recorded in each
  project's `build.gradle`.

---

# AI 使用声明 ｜ AI Usage Statement

本仓库的代码、文档与提交信息在编写过程中使用了生成式 AI 辅助。

具体而言：多数 mixin 与桥接代码由人类作者确定问题根因、设计修法并逐条审查，
**Anthropic 的 Claude Code** 在实现、重构、文档撰写与上游排查环节参与协作。
仓库中部分提交带有 `Co-Authored-By: Claude Code` 尾注。

所有上游调研结论均经过实证核对（字节码比对、字节级 diff、崩溃报告复现），
不以模型输出本身作为依据；重大设计取舍由人类作者裁决并在提交信息或文档中记录。

使用者应自行评估是否符合自身场景，作者不对因 AI 辅助产生的任何错误承担责任。

---

The code, documentation and commit messages in this repository were produced with the
assistance of generative AI.

Specifically: for most mixins and bridges the human author identified the root cause,
designed the fix and reviewed each change, while **Anthropic's Claude Code** assisted with
implementation, refactoring, documentation and upstream investigation. Some commits carry a
`Co-Authored-By: Claude Code` trailer.

All upstream findings were verified empirically (bytecode comparison, byte-level diffs,
reproducing crash reports) and never taken from model output at face value; significant
design decisions were made by the human author and are recorded in commit messages or docs.

Users should evaluate suitability for their own use. The author accepts no liability for
any errors arising from AI assistance.
