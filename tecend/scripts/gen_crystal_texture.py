# -*- coding: utf-8 -*-
"""
raw_casting_mold_crystal_memory：16x16 → 32x32，右下角贴角放一枚迷你奖杯。

晶体按用户要求 **最近邻 2x 直接放大**，原有像素结构逐格保留（每个原像素 = 2x2 方块），
不重绘、不插值。奖杯画在 32x32 原生分辨率上。

输入源是 `scripts/raw_casting_mold_crystal_memory_16x16.png`（原始 16x16 的留档），
**不要读输出的那张 32x32**——它已经带奖杯了，再读会重复叠加。

落位 (20,21)：12 宽 10 高的字形 dx 最大 10、dy 最大 9，描边再各 +1，
所以 ox<=20、oy<=21 才不会让右侧/底部描边掉出画布。取满值即「描边正好压在
x=31 / y=31 上」——奖杯完整，同时顶死右下角。再往下一格 (20,22) 底座就会被切平。
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "scripts", "raw_casting_mold_crystal_memory_16x16.png")
DST = os.path.join(ROOT, "src", "main", "resources", "assets", "tecend",
                   "textures", "item", "raw_casting_mold_crystal_memory.png")

# 12 宽 10 高，偶数宽 + 逐行回文（pixel-art-rules）。
# 关键是**突然收腰**：10→8 连续锥形收窄会读成马提尼杯，必须杯身收到 6 停住、
# 一档掉到 2 宽的柄、底座再张回 6。
TROPHY = [
    ".##########.",
    ".##########.",
    "..########..",
    "..########..",
    "...######...",
    ".....##.....",
    ".....##.....",
    ".....##.....",
    "...######...",
    "...######...",
]
POS = (20, 21)

OUTLINE = (0x2E, 0x12, 0x2C, 255)     # 比晶体自带暗紫再深一档
BODY = (0xFF, 0xF6, 0xE2, 255)        # 奶白：在橙宝石上对比最强
SHADE = (0xE0, 0xB0, 0x80, 255)       # 底座两行压暗


def build():
    src = Image.open(SRC).convert("RGBA")
    if src.size != (16, 16):
        raise SystemExit("源图不是 16x16（%s）—— 别读带奖杯的输出版" % (src.size,))
    im = src.resize((32, 32), Image.NEAREST)          # 直接放大，不破坏像素结构

    h, w = len(TROPHY), len(TROPHY[0])
    ox, oy = POS
    body = {(x, y) for y in range(h) for x in range(w) if TROPHY[y][x] == "#"}

    ring = set()                                       # 描边 = 字形外扩 1 px
    for (x, y) in body:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                if (x + dx, y + dy) not in body:
                    ring.add((x + dx, y + dy))

    for (dx, dy) in ring:
        x, y = ox + dx, oy + dy
        if 0 <= x < 32 and 0 <= y < 32:
            im.putpixel((x, y), OUTLINE)
    for (dx, dy) in body:
        x, y = ox + dx, oy + dy
        if 0 <= x < 32 and 0 <= y < 32:
            im.putpixel((x, y), SHADE if dy >= h - 2 else BODY)
    return im


if __name__ == "__main__":
    out = build()
    out.save(DST)
    print("wrote", DST, out.size, out.mode)
