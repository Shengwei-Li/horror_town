"""Derives the Horror Town block textures from vanilla ones.

Usage: python tools/gen_textures.py <dir with vanilla mud.png, spruce_log.png, spruce_log_top.png, calcite.png>
"""
import random
import sys
from pathlib import Path

from PIL import Image

SRC = Path(sys.argv[1])
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town/textures/block"
OUT.mkdir(parents=True, exist_ok=True)
rng = random.Random(1906)


def grade(img, mul, add=(0, 0, 0), desat=0.0):
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            grey = (r + g + b) / 3
            r, g, b = (c + (grey - c) * desat for c in (r, g, b))
            px[x, y] = tuple(max(0, min(255, int(c * m + d))) for c, m, d in zip((r, g, b), mul, add)) + (a,)
    return img


# 腐泥: darker, greener, wet-looking mud with a few glossy and a few black specks
rot = grade(Image.open(SRC / "mud.png"), (0.62, 0.64, 0.46), (6, 9, 2), desat=0.35)
px = rot.load()
for _ in range(14):
    x, y = rng.randrange(16), rng.randrange(16)
    r, g, b, a = px[x, y]
    px[x, y] = (r + 28, g + 34, b + 26, a) if rng.random() < 0.5 else (r // 2, g // 2, b // 2, a)
rot.save(OUT / "rot_mud.png")

# 朽木: bleached, grey, waterlogged spruce
grade(Image.open(SRC / "spruce_log.png"), (0.9, 0.95, 0.95), (18, 20, 20), desat=0.75).save(OUT / "rotwood_log.png")
grade(Image.open(SRC / "spruce_log_top.png"), (0.85, 0.9, 0.9), (16, 18, 18), desat=0.7).save(OUT / "rotwood_log_top.png")

# 盐壳: off-white crust broken into plates by dark cracks
salt = grade(Image.open(SRC / "calcite.png"), (1.02, 1.02, 0.98), (8, 8, 4), desat=0.6)
px = salt.load()
cracks = [((0, 5), (1, 0), 6), ((5, 5), (0, 1), 7), ((5, 11), (1, 0), 11), ((10, 0), (0, 1), 5),
          ((10, 5), (1, 0), 6), ((12, 11), (0, 1), 5), ((2, 11), (0, 1), 5)]
for (x, y), (dx, dy), n in cracks:
    for i in range(n):
        cx, cy = (x + dx * i) % 16, (y + dy * i) % 16
        if rng.random() < 0.85:
            px[cx, cy] = (118, 112, 104, 255)
salt.save(OUT / "salt_crust.png")
