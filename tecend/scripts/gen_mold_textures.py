# -*- coding: utf-8 -*-
"""
tecend 模具材质生成器。

产出 9 张 16x16：
  物品：rough_mold_sand / rough_mold_wax / perfect_mold
  方块：mold_bottom / mold_side_x / mold_side_z
        mold_top_empty / mold_top_hot / mold_top_cooled

视觉语言沿用 GT 浇铸模具：一块方板，中央凹腔里是奖杯的负形。
三种模具靠材质色 + 表面完成度区分（沙=坑洼、蜡=圆润、完美=光洁）。

光照约定：光源在左上 —— 板的外沿上/左受光、下/右背光；
凹腔的内壁上/左压暗、下/右提亮。凹腔底部最深的是奖杯负形。
"""
import os
import random

from PIL import Image

OUT = r"D:\Projects\游戏\Minecraft\tecend\src\main\resources\assets\tecend\textures"

# ---------------------------------------------------------------- 奖杯侧影
# 依据 proof_of_honor championship_trophy 模型实测轮廓：
#   杯口 y12-16 宽16 / 杯肩 y10-11 宽14 / 杯肚 y7-9 宽8 / 杯柄 y3-6 宽4 / 底座 y0-2 宽6
#
# 两条硬规矩：
#   1) **横向一律偶数**。奖杯左右对称，奇数字宽没法精确居中——第一版 7 宽的字形
#      放进 9 宽的腔体，两边边距一个 4 一个 5，整体真的偏了一格。
#   2) 前两行是**杯口开口**不是实心顶盖：口沿一圈实心，下面只留两侧杯壁，
#      中间露出腔底色 —— 顶面是俯视面，得看得见「看进杯子里」。
#
# 每行都是回文（以 4.5 / 3.5 为轴镜像），两侧严格对齐。
GLYPH_BLOCK = [                 # 方块顶面用，10 宽 10 高（腔体 12x12，四周各留 1px）
    ".########.",                # 口沿
    ".#......#.",                # 杯口开口：中间露出腔底，顶面是俯视面，要看得见看进杯里
    ".#......#.",
    "..######..",                # 杯身内底
    "...####...",
    "....##....",                # 杯柄 3 行
    "....##....",
    "....##....",
    "...####...",                # 底座
    "...####...",
]
GLYPH_ITEM = [                  # 物品用，8 宽 8 高（腔体 10x10，四周各留 1px），同比例缩
    ".######.",
    ".#....#.",
    ".#....#.",
    "..####..",
    "...##...",
    "...##...",
    "..####..",
    "..####..",
]


# ---------------------------------------------------------------- 调色板
# edge 外沿暗边 / lit 受光边 / shade 背光边 / surf 板面 / surf2 板面暗纹
# pit 坑洼 / deep 深坑 / hole 凹腔底 / hole_lo 腔底最暗 / hole_lit 腔壁受光
PALETTES = {
    "sand": dict(
        edge=(0x5A, 0x4C, 0x33), lit=(0xE3, 0xD5, 0xA6), shade=(0x9C, 0x8B, 0x5B),
        surf=(0xC6, 0xB5, 0x7E), surf2=(0xB6, 0xA4, 0x6E),
        pit=(0xA2, 0x90, 0x5E), deep=(0x8A, 0x78, 0x4A),
        hole=(0x6B, 0x5B, 0x38), hole_lo=(0x3C, 0x32, 0x1D), hole_lit=(0x8E, 0x7C, 0x50),
        pits=24, round=False,
    ),
    # 蜡取样自资源蜜蜂 item/wax：本体是亮黄 #FFD32D，橙褐 #C55704/#AA4C13 只做描边，
    # 奶白 #FFEDA8 提亮。主色必须是黄，把橙当主色会读成陶土块。
    "wax": dict(
        edge=(0xAA, 0x4C, 0x13), lit=(0xFF, 0xED, 0xA8), shade=(0xD7, 0x6F, 0x18),
        surf=(0xFF, 0xD3, 0x2D), surf2=(0xF0, 0xBE, 0x22),
        pit=(0xE0, 0x8A, 0x14), deep=(0xC9, 0x5E, 0x06),
        hole=(0x9C, 0x46, 0x08), hole_lo=(0x5C, 0x26, 0x02), hole_lit=(0xC5, 0x66, 0x10),
        pits=11, round=True,
    ),
    "perfect": dict(
        edge=(0x3F, 0x49, 0x55), lit=(0xDD, 0xE4, 0xEB), shade=(0x9E, 0xA9, 0xB4),
        surf=(0xC6, 0xCF, 0xD7), surf2=(0xBB, 0xC5, 0xCE),
        pit=None, deep=(0xA6, 0xB0, 0xBB),
        hole=(0x55, 0x62, 0x70), hole_lo=(0x25, 0x2C, 0x35), hole_lit=(0x8B, 0x99, 0xA7),
        pits=0, round=False,
    ),
}


def check_glyph(name, glyph):
    """字形自检：横向偶数 + 每行回文（左右对称）。奇数字宽没法在腔体里精确居中。"""
    w = len(glyph[0])
    assert w % 2 == 0, "%s: 字宽 %d 是奇数，横向对称的东西必须用偶数字宽" % (name, w)
    for i, row in enumerate(glyph):
        assert len(row) == w, "%s 第 %d 行长度 %d != %d" % (name, i, len(row), w)
        assert row == row[::-1], "%s 第 %d 行不对称: %s" % (name, i, row)


for _n, _g in (("GLYPH_BLOCK", GLYPH_BLOCK), ("GLYPH_ITEM", GLYPH_ITEM)):
    check_glyph(_n, _g)


def draw_cavity(img, pal, x0, y0, x1, y1, glyph, rim_light=True):
    """在 (x0,y0)-(x1,y1) 里画出凹腔：腔底 + 内壁光影 + 居中的奖杯负形。

    rim_light=False 时只留上/左的内壁阴影，去掉下/右的受光边——
    16x16 的物品图里那一圈亮边会读成「相框」，抢掉奖杯负形。
    """
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            img.putpixel((x, y), pal["hole"] + (255,))
    for x in range(x0, x1 + 1):
        img.putpixel((x, y0), pal["hole_lo"] + (255,))       # 上内壁吃阴影
    for y in range(y0, y1 + 1):
        img.putpixel((x0, y), pal["hole_lo"] + (255,))       # 左内壁吃阴影
    if rim_light:
        for x in range(x0, x1 + 1):
            img.putpixel((x, y1), pal["hole_lit"] + (255,))  # 下内壁受光
        for y in range(y0, y1 + 1):
            img.putpixel((x1, y), pal["hole_lit"] + (255,))  # 右内壁受光

    gw, gh = len(glyph[0]), len(glyph)
    gx = x0 + (x1 - x0 + 1 - gw) // 2
    gy = y0 + (y1 - y0 + 1 - gh) // 2
    for dy, row in enumerate(glyph):
        for dx, ch in enumerate(row):
            if ch == "#":
                img.putpixel((gx + dx, gy + dy), pal["hole_lo"] + (255,))
    return gx, gy


def make_item_mold(key, seed=0):
    """物品形态：正面看的一块方板，中央凹腔里是奖杯负形。"""
    pal = PALETTES[key]
    rng = random.Random(seed + sum(map(ord, key)))
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))

    # --- 板面：外沿一圈暗边，上/左那半圈用较亮的 shade，做出光从左上来的立体感。
    #     不再用整圈 lit 亮边——那会在 16x16 里读成一个「相框」。
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            if d == 0:
                c = pal["shade"] if (x == 0 or y == 0) else pal["edge"]
            else:
                c = pal["surf"]
            img.putpixel((x, y), c + (255,))

    # --- 表面质感：沙是密而碎的颗粒，蜡是疏而软的凹坑，完美没有。
    #     一律单像素 —— 让颗粒贴着邻格连成块，放大后会读成「污渍」而不是「质地」。
    if pal["pits"]:
        for _ in range(pal["pits"]):
            x, y = rng.randint(2, 13), rng.randint(2, 13)
            img.putpixel((x, y), pal["pit"] + (255,))

    # --- 凹腔：8 宽 8 高的字形 → 腔体 10x10，(3,3) 起，左右上下各留 1 px 腔底、3 px 板面
    gw, gh = len(GLYPH_ITEM[0]), len(GLYPH_ITEM)
    x0 = (16 - (gw + 2)) // 2          # 腔外框比字形大 1 px
    y0 = (16 - (gh + 2)) // 2
    draw_cavity(img, pal, x0, y0, x0 + gw + 1, y0 + gh + 1, GLYPH_ITEM, rim_light=False)

    # --- 收尾：沙磕角、蜡圆角、完美上镜面反光
    if key == "sand":
        for cx, cy in ((15, 15), (0, 0)):
            img.putpixel((cx, cy), (0, 0, 0, 0))
        img.putpixel((14, 15), pal["deep"] + (255,))
        img.putpixel((15, 14), pal["deep"] + (255,))
    elif key == "wax":
        for cx, cy in ((0, 0), (15, 0), (0, 15), (15, 15)):
            img.putpixel((cx, cy), pal["deep"] + (255,))
        # 蜡的软反光：板面上撒几点浅色，免得整块板面平得像色卡
        for _ in range(7):
            x, y = rng.randint(2, 13), rng.randint(2, 13)
            if img.getpixel((x, y))[:3] == pal["surf"]:
                img.putpixel((x, y), pal["surf2"] + (255,))
    else:
        # 完美模具：对角一对抛光面 —— 左上受光、右下背光。
        # 全部用贴近板面的浅/深色，纯白在 16x16 里会读成脏点。
        for (x, y) in ((1, 1), (2, 2), (1, 2), (2, 1)):
            img.putpixel((x, y), pal["lit"] + (255,))
        for (x, y) in ((14, 14), (13, 13), (14, 13), (13, 14)):
            img.putpixel((x, y), pal["surf2"] + (255,))
    return img


# ---------------------------------------------------------------- 方块面
def _sand_frame(img, seed=5, pits=22):
    sand = PALETTES["sand"]
    rng = random.Random(seed)
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c = sand["edge"]
            elif x == 1 or y == 1:
                c = sand["lit"]
            elif x == 14 or y == 14:
                c = sand["shade"]
            else:
                c = sand["surf"]
            img.putpixel((x, y), c + (255,))
    for _ in range(pits):
        x, y = rng.randint(2, 13), rng.randint(2, 13)
        img.putpixel((x, y), sand["pit"] + (255,))


def make_block_top(state):
    """方块顶面：砂型外框 + 内凹腔，腔里按状态给 空 / 熔融 / 凝固。"""
    sand = PALETTES["sand"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    _sand_frame(img)
    gx, gy = draw_cavity(img, sand, 2, 2, 13, 13, GLYPH_BLOCK)
    gw, gh = len(GLYPH_BLOCK[0]), len(GLYPH_BLOCK)

    if state == "empty":
        return img

    if state == "hot":
        hi, mid, lo = (0xFF, 0xF6, 0xCE), (0xFF, 0xC8, 0x36), (0xDE, 0x74, 0x0E)
    else:
        hi, mid, lo = (0xFF, 0xEC, 0xA6), (0xE6, 0xB2, 0x2C), (0xA4, 0x73, 0x12)

    for dy, row in enumerate(GLYPH_BLOCK):
        for dx, ch in enumerate(row):
            if ch != "#":
                continue
            edge = (dx == 0 or dy == 0 or dx == len(row) - 1 or dy == gh - 1)
            img.putpixel((gx + dx, gy + dy), (lo if edge else mid) + (255,))

    # 亮斑只从字形内部的格子里挑 —— 写死偏移会有几颗落到奖杯轮廓外的腔底上，
    # 放大看就是凭空多出来的白点。
    cells = [(dx, dy) for dy, row in enumerate(GLYPH_BLOCK)
             for dx, ch in enumerate(row) if ch == "#"]

    if state == "hot":
        # 熔融：滚动的亮斑 + 热光溢到腔壁上
        for i in (3, 6, 14, 22, 31, 38):
            dx, dy = cells[i % len(cells)]
            img.putpixel((gx + dx, gy + dy), hi + (255,))
        glow, warm = (0xE0, 0x8A, 0x3C), (0xB8, 0x92, 0x5E)
        for x in range(4, 12):
            img.putpixel((x, 2), warm + (255,))
            img.putpixel((x, 13), glow + (255,))
        for y in range(4, 12):
            img.putpixel((2, y), warm + (255,))
            img.putpixel((13, y), glow + (255,))
    else:
        # 凝固：金属的锐利高光
        for i in (1, 8, 20, 30, 35):
            dx, dy = cells[i % len(cells)]
            img.putpixel((gx + dx, gy + dy), hi + (255,))
    return img


def make_block_side(axis):
    """方块侧面：砂型外壁，中间一道上下模合缝，浇口从顶沿接进合缝。"""
    sand = PALETTES["sand"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(11 if axis == "x" else 23)

    SEAM = 10
    for y in range(16):
        for x in range(16):
            if x in (0, 15):
                c = sand["edge"]                       # 左右立边
            elif y == 0:
                c = sand["lit"]                        # 顶沿受光
            elif y == 15:
                c = sand["edge"]                       # 底沿压暗
            elif y == SEAM:
                c = sand["hole"]                       # 合缝阴影
            elif y == SEAM + 1:
                c = sand["hole_lit"]                   # 下模顶沿反光
            elif y > SEAM:
                c = sand["surf2"]                      # 下半模整体略暗
            else:
                c = sand["surf"]
            img.putpixel((x, y), c + (255,))

    # 砂粒：只在上下模的壁面上撒，别落到合缝线上
    for _ in range(34):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        if y in (SEAM, SEAM + 1):
            continue
        img.putpixel((x, y), sand["pit"] + (255,))

    # 浇口：从顶沿直落进合缝，x 轴侧靠左、z 轴侧靠右，两个轴一眼分得开
    gx = 4 if axis == "x" else 11
    for y in range(1, SEAM):
        img.putpixel((gx, y), sand["deep"] + (255,))
    img.putpixel((gx, 1), sand["hole"] + (255,))
    return img


def make_block_bottom():
    sand = PALETTES["sand"]
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(31)
    for y in range(16):
        for x in range(16):
            d = min(x, y, 15 - x, 15 - y)
            img.putpixel((x, y), (sand["edge"] if d == 0 else sand["deep"]) + (255,))
    for _ in range(38):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        img.putpixel((x, y), sand["pit"] + (255,))
    return img


def main():
    os.makedirs(os.path.join(OUT, "item"), exist_ok=True)
    os.makedirs(os.path.join(OUT, "block"), exist_ok=True)
    jobs = [
        ("item/rough_mold_sand.png", make_item_mold("sand")),
        ("item/rough_mold_wax.png", make_item_mold("wax")),
        ("item/perfect_mold.png", make_item_mold("perfect")),
        ("block/mold_top_empty.png", make_block_top("empty")),
        ("block/mold_top_hot.png", make_block_top("hot")),
        ("block/mold_top_cooled.png", make_block_top("cooled")),
        ("block/mold_side_x.png", make_block_side("x")),
        ("block/mold_side_z.png", make_block_side("z")),
        ("block/mold_bottom.png", make_block_bottom()),
    ]
    for rel, im in jobs:
        im.save(os.path.join(OUT, rel.replace("/", os.sep)))
    print("wrote", len(jobs), "textures")


if __name__ == "__main__":
    main()
