#!/usr/bin/env python3
"""生成「带铸造毛刺的奖杯」几何 -> models/block/trophy_sprue.json
（用在 age=6「粗胚奖杯（未剪水口）」和 age=3「粗胚奖杯（未处理）」两态上）

思路：不搞大块造型，改在**侧面**撒几块凸起的铸造毛刺，尺寸 0.5~1 单位
（= 用户那张 64x64 贴图的 2~4 像素），每个凸块只用贴图上**一个像素**做 UV
（贴图 1 像素 = 0.25 uv 单位，texel 对齐）→ 每块是一小块纯色金疙瘩，
深浅不一地拼出「坑坑洼洼」的粗铸面。0.25 单位 = 1 像素。

几何依据（proof_of_honor 的 custom/championship_trophy 实测元素范围）：
    底座  x5..11  y0..3   z5..11
    柱身  x6..10  y3..7   z6..10
    圆环  x5..11  y7..9   z5..11
    杯壁  y9..17，外圈 x4..12 与 z4..12（内腔 x5..11 / z5..11），杯沿顶 y=17
    把手  z7..9，y10..17，x0..4 与 x12..16

用法：python scripts/gen_sprue_model.py
"""
import json
import os

# (轴, 平面坐标, 外向符号, u, v, 面尺寸, 凸出深度, 采样像素)
#   轴 x：u = z 坐标，v = y 坐标；轴 z：u = x 坐标，v = y 坐标
#   面尺寸 = 贴着表面那一面的边长；凸出深度 = 离开表面的距离
#   （2026-09-16 第二版：长宽各 ×2、凸出 ×1.5、数量 29 → 8）
STUDS = [
    # 杯壁 · 西（外表面 x=4，向外 -x）
    #   注意 z 6.7~9.3 是把手与杯壁的重叠区，那里的毛刺会被埋住，避开
    ("x", 4.0, -1, 5.60, 10.30, 0.75, 0.45, (20, 20)),
    ("x", 4.0, -1, 9.90, 13.60, 0.50, 0.38, (12, 20)),
    # 杯壁 · 东（x=12，向外 +x）
    ("x", 12.0, 1, 5.40, 11.40, 1.00, 0.45, (28, 28)),
    ("x", 12.0, 1, 9.70, 15.00, 0.75, 0.38, (4, 12)),
    # 杯壁 · 北（z=4，向外 -z）
    ("z", 4.0, -1, 6.40, 12.20, 0.75, 0.45, (12, 12)),
    # 杯壁 · 南（z=12，向外 +z）
    ("z", 12.0, 1, 9.40, 14.40, 1.00, 0.45, (20, 4)),
    # 底座侧面（y0..3，外圈 z=5）
    ("z", 5.0, -1, 6.80, 1.00, 0.50, 0.38, (28, 20)),
    # 把手侧面（z=7，西把手 x0..4）
    ("z", 7.0, -1, 1.60, 12.40, 0.50, 0.38, (20, 28)),
]

# 贴图上的采样像素（都是左上 40x40 的实心金色区，逐像素采样确认过不透明）
# 注意：跟着 age6 的主题材质走 —— age6 用「未喷砂」那张，age3 用「未处理」那张
TEXTURE = "tecend:block/trophy_rough_trimmed"
EMBED = 0.2          # 嵌进表面的深度：避免与表面共面产生 z-fighting
TEXEL_UV = 0.25      # 贴图 1 像素 = 0.25 uv 单位（64px 贴图 → 16 单位 uv 空间）


def r(x):
    """坐标/UV 保留 3 位（免得写出 13.280000000000001 这种浮点尾巴）"""
    return round(x, 3)


def uv_box(px, py):
    """取贴图上第 (px, py) 个像素 -> uv 矩形"""
    u, v = px * TEXEL_UV, py * TEXEL_UV
    return [r(u), r(v), r(u + TEXEL_UV), r(v + TEXEL_UV)]


def make_stud(name, axis, plane, sign, u, v, size, depth, texel):
    uv = uv_box(*texel)
    if axis == "x":
        # 外表面垂直于 x：向外是 sign * x 方向，u 沿 z，v 沿 y
        x0, x1 = (plane - depth, plane + EMBED) if sign < 0 else (plane - EMBED, plane + depth)
        z0, z1 = u, u + size
        y0, y1 = v, v + size
    else:
        z0, z1 = (plane - depth, plane + EMBED) if sign < 0 else (plane - EMBED, plane + depth)
        x0, x1 = u, u + size
        y0, y1 = v, v + size
    faces = {d: {"uv": uv, "texture": "#0"} for d in ("north", "east", "south", "west", "up", "down")}
    return {"name": name, "from": [r(x0), r(y0), r(z0)], "to": [r(x1), r(y1), r(z1)], "faces": faces}


def main():
    elements = []
    for i, (axis, plane, sign, u, v, size, depth, texel) in enumerate(STUDS, 1):
        elements.append(make_stud(f"burr_{i:02d}", axis, plane, sign, u, v, size, depth, texel))

    model = {
        "render_type": "solid",
        "textures": {"0": TEXTURE, "particle": TEXTURE},
        "elements": elements,
    }
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, "..", "src", "main", "resources", "assets", "tecend",
                       "models", "block", "trophy_sprue.json")
    out = os.path.normpath(out)
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        json.dump(model, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"wrote {out}  ({len(elements)} burrs)")


if __name__ == "__main__":
    main()
