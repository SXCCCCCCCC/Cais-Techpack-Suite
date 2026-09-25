#!/usr/bin/env python3
"""生成三种残片液桶的物品贴图 -> assets/tecend/textures/item/bucket_*.png

做法：拿原版 bucket.png 当桶身，原版 bucket.png 与 water_bucket.png 的差异像素就是桶口液面区
（x 3..12、y 2..5，实测 32 个像素），这批像素用我们自己的流体贴图同坐标的颜色填上。
单层物品贴图，跟金猪桶（bucket_pig.png）同一种形式：模型里只用 layer0。

用法：python scripts/gen_bucket_textures.py
"""
import io
import os
import zipfile

from PIL import Image

# 原版客户端资源（jar 里有 textures/item/bucket.png 与 water_bucket.png）
VANILLA_JAR = (r"D:/Games/Minecraft/.minecraft/libraries/net/minecraft/client"
               r"/1.20.1-20230612.114412/client-1.20.1-20230612.114412-extra.jar")

# 输出贴图名 -> 流体贴图（相对 assets/tecend/textures/）
BUCKETS = {
    "bucket_fragment_solution": "block/fragment_solution_still.png",
    "bucket_activated_fragment_solution": "block/activated_fragment_solution_still.png",
    "bucket_acidic_fragment_solution": "block/acidic_fragment_solution_still.png",
}


def main():
    here = os.path.dirname(os.path.abspath(__file__))
    tex_root = os.path.normpath(os.path.join(here, "..", "src", "main", "resources",
                                             "assets", "tecend", "textures"))
    z = zipfile.ZipFile(VANILLA_JAR)
    bucket = Image.open(io.BytesIO(z.read("assets/minecraft/textures/item/bucket.png"))).convert("RGBA")
    water = Image.open(io.BytesIO(z.read("assets/minecraft/textures/item/water_bucket.png"))).convert("RGBA")

    # 桶口液面区：原版两只桶画得不一样的地方
    mask = [(x, y) for y in range(16) for x in range(16)
            if bucket.getpixel((x, y)) != water.getpixel((x, y))]

    for name, fluid_rel in BUCKETS.items():
        fluid = Image.open(os.path.join(tex_root, fluid_rel)).convert("RGBA")
        out = bucket.copy()
        for (x, y) in mask:
            # 流体贴图是同尺寸 16x16，直接取同坐标的像素
            out.putpixel((x, y), fluid.getpixel((x, y)))
        dest = os.path.join(tex_root, "item", name + ".png")
        out.save(dest)
        print(f"wrote {dest}")


if __name__ == "__main__":
    main()
