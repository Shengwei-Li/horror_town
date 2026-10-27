"""Paints the copper coin (铜钱) item icon.

Usage: python tools/gen_items.py
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town/textures"


def coin():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(7)
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            if r > 6.8 or (abs(dx) < 1.6 and abs(dy) < 1.6):
                continue  # outside, or the square hole
            rim = r > 5.6 or (abs(dx) < 2.6 and abs(dy) < 2.6)
            base = (176, 110, 64) if rim else (150, 92, 54)
            if rng.random() < 0.18:
                base = (86, 140, 110)  # verdigris
            n = rng.randint(-10, 10)
            px[x, y] = tuple(max(0, min(255, c + n)) for c in base) + (255,)
    out = ROOT / "item/copper_coin.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


if __name__ == "__main__":
    coin()
