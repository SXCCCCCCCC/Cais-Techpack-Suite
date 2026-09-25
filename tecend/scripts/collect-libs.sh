#!/usr/bin/env bash
# 从整合包收集本工程上游 jar 到 libs/。
#
# libs/*.jar 不进 git（合计约 160M），换机、重建、上游升级后跑一次这个脚本。
#   bash scripts/collect-libs.sh
# 可用 MODS_DIR 覆盖整合包 mods 目录。

set -uo pipefail

MODS="${MODS_DIR:-D:/Games/Minecraft/.minecraft/versions/1.20.1-Tec/mods}"
CONNECTOR="$MODS/.connector"
DST="$(cd "$(dirname "$0")/.." && pwd)/libs"

[ -d "$MODS" ] || { echo "找不到整合包 mods 目录：$MODS" >&2; exit 1; }
mkdir -p "$DST/jarjar"

# ---------- Forge 系上游：直接用 mods/ 里的 jar ----------
JARS=(
    gtceu-*.jar
    gtca-*.jar
    ImmersiveEngineering-*.jar
    IndustrialUpgrade-*.jar
    Mekanism-1.20.1-*.jar
    create-1.20.1-*.jar
    createoreexcavation-*.jar
    titanium-*.jar
    industrial-foregoing-*.jar
    cofh_core-*.jar
    thermal_expansion-*.jar
    thermal_foundation-*.jar
    pneumaticcraft-repressurized-*.jar
    appliedenergistics2-forge-*.jar
    MysticalAgriculture-*.jar
    MysticalAgradditions-*.jar
    productivebees-*.jar
    beyonddimensions-*.jar
    pylons-*.jar
    balm-*.jar
    cookingforblockheads-*.jar
    jei-*.jar
)

copied=0
for pattern in "${JARS[@]}"; do
    for f in "$MODS"/$pattern; do
        [ -e "$f" ] || continue
        cp -f "$f" "$DST/" && copied=$((copied + 1))
    done
done

# ---------- IC2：取 Connector 的重映射版，不是 mods/ 里的原始 jar ----------
# 原始 jar 是 intermediary 的，fg.deobf 会报错；.connector 里的是运行时真身
# （mojmap 类名 + SRG 成员名）。IC2 或 Connector 升级后要重新取。
IC2="$(ls -1 "$CONNECTOR"/industrialcraft2-refabricated-*_mapped_srg_*.jar 2>/dev/null | grep -v '\.input$' | grep -v '\$' | head -1)"
if [ -n "$IC2" ]; then
    cp -f "$IC2" "$DST/" && copied=$((copied + 1))
    echo "IC2: $(basename "$IC2")"
else
    echo "警告：.connector 里没找到 IC2 的重映射 jar，先在整合包里启动一次游戏让它生成" >&2
fi

# ---------- GTCEu 的 jarjar 嵌套依赖（javac 看不到嵌套 jar 里的类）----------
GTCEU="$(ls -1 "$DST"/gtceu-*.jar 2>/dev/null | head -1)"
if [ -n "$GTCEU" ]; then
    unzip -j -o "$GTCEU" 'META-INF/jarjar/ldlib-*.jar' 'META-INF/jarjar/mixinextras-*.jar' -d "$DST/jarjar/" >/dev/null
    echo "jarjar: $(ls -1 "$DST/jarjar" | tr '\n' ' ')"
fi

echo "共收集 $copied 个 jar 到 $DST"
du -sh "$DST"
