# Cai's Techpack Suite

`1.20.1-Tec` 整合包（Minecraft 1.20.1 / Forge 47.4.10 / Sinytra Connector）配套的自研 mod 工程合集。

## 结构

```
Cais-Techpack-Suite/
├── <工程名>/            每个目录是一个独立 Gradle 工程
├── disabled/            已退役 / 已取消的工程，保留源码备查，不部署
└── thermal_guide_zhcn/  Thermal Series 指南书简体中文翻译（资源包形态，非 mod）
```

## 工程一览

### Industrial Upgrade 线

| 工程 | 版本 | 用途 |
|---|---|---|
| `iufix` | 1.9.7 | IU 3.4.0.10 修复合集（27 个 mixin：污染 debuff、世界生成 NPE、镐挖门槛、WorldEdit 状态往返、多胞碰撞与铁砧、Jade、分子重组机 GUI 等） |
| `iuunify` | 0.5.7 | IU × IC2 流体统一（钚换源、分子重构机配方配置化） |
| `iuspeed` | 1.5.0 | IU 配方机提速（电编程台 25×、太空系统 10×） |
| `iuchancefix` | 1.0.0 | IU 矿物分离机煤粉合金配方确定性化（chance 75 → 100，供 AE2 编码样板） |
| `uubridge` | 0.1.3 | IU × IC2 的 UU 链桥（晶体记忆双向桥、UU 成本族内归一化） |
| `iudebug` | 1.2.1 | IU **破坏性**调试工具（按 config 取消注册；默认全关） |

### GregTech 线

| 工程 | 版本 | 用途 |
|---|---|---|
| `gtjeimbfix` | 1.0.0 | GTCEu 多方块 JEI 信息页悬停 `IndexOutOfBoundsException` 修复 |
| `gtnoboomfix` | 1.0.0 | 关闭 GTCEu 全部爆炸/伤害机制（本包的设计决定，非上游 bug） |
| `creativetabfix` | 1.0.0 | GTCA 物品不进创造页修复（监听 `BuildCreativeModeTabContentsEvent`，零 mixin） |

### IC2 / Mekanism / Immersive Engineering 线

| 工程 | 版本 | 用途 |
|---|---|---|
| `ic2reactorhopperfix` | 1.0.0 | IC2 核反应堆仓/接口的物品存储事务快照为空导致物流丢物 |
| `ic2mekradfix` | 1.2.0 | IC2 × Mekanism × GregTech 三家防辐射服互通 |
| `mekmmuufix` | 1.0.0 | 让 mekmm 复制机走 IC2 的 UU 成本系统 |
| `iespawnfix` | 1.0.0 | IE 刷怪抑制在 Forge 47.4 上 `setSpawnCancelled` 崩服 |

### Industrial Foregoing / Create 线

| 工程 | 版本 | 用途 |
|---|---|---|
| `iffermentfix` | 1.0.0 | IF 发酵站换矿种时静默丢产出（`canIncrease` 不查流体身份） |
| `createindustrialforegoingfluidfix` | 1.0.0 | Create 软管滑轮把 IF 流体判成 flowing 变体 |

### 流体统一

| 工程 | 版本 | 用途 |
|---|---|---|
| `fluidunifyapi` | 0.1.0 | 可配置的「机器输入流体」API（IC2 / Industrial Foregoing / Industrial Upgrade），只改输入侧定义，不接触流体身份与 Forge `FluidTank` 混液语义 |

### Connector / 前置库线

| 工程 | 版本 | 用途 |
|---|---|---|
| `ae2ipnswipefix` | 1.0.0 | IPN 的容器 swipe 在 AE2 终端会误触第一张升级卡 |
| `cofhlightfix` | 1.0.0 | CoFH Core 瞬时光源客户端 NPE（第二循环缺空守卫） |
| `constructionstickfix` | 1.0.0 | Construction Sticks 资源重载时的 NPE |
| `fabrictransferfix` | 1.0.0 | Forgified Fabric API 的 capability 缓存并发 `ConcurrentModificationException` |

### 数据层 / 小 mod 修复

| 工程 | 版本 | 用途 |
|---|---|---|
| `kjsexportcmd` | 1.1.0 | `/kjsexport` 导出命令 + GT 全局 5 倍速注册 |
| `oeikjsfix` | 0.1.0 | OneEnoughItem 的替换文件路径重定向到 `kubejs/data` |
| `mafgllibfix` | 1.0.0 | MaFgLib 并行 mod 加载下的 ArrayList 损坏 + config 加载时机 |
| `tweakergammafix` | 1.0.0 | Tweakerge gamma 覆写重启失效 + `Options.save()` codec 拒值 |
| `oeffix` | 0.1.1 | OneEnoughFluid 的 reload-override NPE |
| `macobaltfix` | 1.1.0 | MA 作物门控绕过 + 材料 provider 换 GTCEu |

### 内容 mod

| 工程 | 版本 | 用途 |
|---|---|---|
| `tecend` | 0.1.0 | 终局奖杯 mod：15 步主线，跨 mod 能量充能 |
| `maaddon` | 0.1.0 | Mystical Agriculture addon：盐 / 塑料 / 粉黏液作物 |

### 翻译

| 工程 | 用途 |
|---|---|
| `thermal_guide_zhcn` | Thermal Series 帕秋莉指南书简体中文翻译，以资源包形态分发（产物在 `out/`） |

### disabled/

| 工程 | 退役原因 |
|---|---|
| `acfix` | Applied Create 应力 P2P 隧道守卫，法医审计后定案关闭 |
| `oeifix` | OEI 性能修复，实测无效后全量回滚 |
| `fluidqinshihuangdi` | `fluidunifyapi` 的前身 |
| `jeitransferfix` | JEI × 精妙背包升级台 `+` 按钮修复；上游 SophisticatedCore `1.20.1-1.3.78.2249` 已修，退役 |

## 构建

**必须显式指定 JDK 17**——本机默认 JDK 25 会让 Gradle 8.8 报 `Unsupported class file major version 69`：

```bash
JAVA_HOME="C:/Program Files/Java/jdk-17" ./gradlew.bat build
```

`libs/` 下的依赖 jar **不入库**（它们多为上游 mod，编译期 `compileOnly` 引用）。
需要自行从整合包的 `mods/` 目录收集；`tecend` 提供了 `scripts/collect-libs.sh`。

## 许可

本仓库是**多个独立工程的合集**，各工程的许可见其目录下的 `LICENSE` 文件：

- 除 `tecend/`（**LGPL-3.0**）外，均为 **MIT**。
- `maaddon/` 含来自 Industrial Agriculture 的贴图，其许可为 **LGPL-2.1**，详见该目录的 `THIRD_PARTY_NOTICES.md`。
- `thermal_guide_zhcn/` 是 Thermal Series 指南书的翻译作品，以**资源包形态**分发（不修改、不重分发原 mod），使用者需自行安装原 mod。

## 说明

- 全部工程只通过**上游公开 API 调用**或 **mixin 注入**实现，不包含上游代码。
- `libs/`、`build/`、`.gradle/` 及 `thermal_guide_zhcn/work/`（含解包的上游素材）均不入库。
