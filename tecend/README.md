# Thank You for Playing Cai's Modpack

`1.20.1-Tec` 整合包的终局 mod。modid `tecend`，包 `com.sxcccccccc.tecend`，
Forge 1.20.1 / 47.4.10 / Java 17。许可 LGPL-3.0（LICENSE + LICENSE.GPL-3.0，
后者是 LGPL-3.0 正文引用到的 GPL-3.0 副本）。

流程设计见 **[docs/设计流程.md](docs/设计流程.md)**（15 步，每步标了出自哪个上游的哪台机器，
附依赖事实的核实出处）。

## 构建

必须显式带 JDK 17（本机默认 java 25 会报 `major version 69`）：

```bash
JAVA_HOME="C:\Program Files\Java\jdk-17" cmd //c "gradlew.bat build"
```

## 上游依赖

22 个上游全部 compileOnly（编译期从 `libs/` 解析，运行期由整合包提供）；
"哪个 jar 对应设计流程哪一步"写在 `build.gradle` 的注释里。
**IC2 是特例**：要取 `mods/.connector/` 里的重映射版，不是 `mods/` 里的原始 jar ——
详见 [libs/README.md](libs/README.md)。

`libs/*.jar` 不进 git，换机或重建时 `bash scripts/collect-libs.sh` 从整合包收集。

## 三条纪律

1. 只调上游 API，**不复制上游代码**（GTCEu 是 LGPL、IU 是 AGPL，抄码会拖进 copyleft）。
2. 引用上游类前先过 `Compat.isLoaded(...)`（上游在 mods.toml 里全是 `mandatory=false`）。
3. 数值走配置，不写死。

## 目录

```
build.gradle            上游接线（每条注明对应设计流程哪一步）
docs/设计流程.md         15 步设计 + 依赖事实核实
libs/README.md          jar 来源、IC2 特例、源码参照索引
scripts/collect-libs.sh 从整合包收集 libs/
src/main/java/com/sxcccccccc/tecend/
    TecEnd.java          主类
    registry/            物品 / 创造标签
    compat/Compat.java   上游门禁
```
