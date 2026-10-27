"""Paints the townsfolk skins (64x64 player layout) and the copper coin icon.

Usage: python tools/gen_skins.py
Each role gets three skins, one per stage of turning: 0 pale, 1 gills and scales, 2 fish-eyed.
"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town/textures"
SKINS = ROOT / "entity/townsfolk"


def faces(u, v, w, h, d):
    """The six face rectangles (x, y, width, height) of a box in the skin layout."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h),
    }


HEAD, HAT = (0, 0, 8, 8, 8), (32, 0, 8, 8, 8)
BODY, JACKET = (16, 16, 8, 12, 4), (16, 32, 8, 12, 4)
R_ARM, R_SLEEVE = (40, 16, 4, 12, 4), (40, 32, 4, 12, 4)
L_ARM, L_SLEEVE = (32, 48, 4, 12, 4), (48, 48, 4, 12, 4)
R_LEG, R_PANT = (0, 16, 4, 12, 4), (0, 32, 4, 12, 4)
L_LEG, L_PANT = (16, 48, 4, 12, 4), (0, 48, 4, 12, 4)
SIDES = ("front", "back", "left", "right")


class Painter:
    def __init__(self, seed):
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.px = self.img.load()
        self.rng = random.Random(seed)

    def shade(self, colour, amount=10):
        n = self.rng.randint(-amount, amount)
        return tuple(max(0, min(255, c + n)) for c in colour[:3]) + (255,)

    def rect(self, x, y, w, h, colour, amount=10):
        for i in range(x, x + w):
            for j in range(y, y + h):
                self.px[i, j] = self.shade(colour, amount)

    def box(self, box, colour, amount=10, rows=None):
        """Fills every face of a box; {rows} (from, to) limits the side faces to those rows (sleeves, shorts)."""
        for name, (x, y, w, h) in faces(*box).items():
            if rows is None:
                self.rect(x, y, w, h, colour, amount)
            elif name in SIDES:
                self.rect(x, y + rows[0], w, min(h, rows[1]) - rows[0], colour, amount)
            elif name == "top":
                self.rect(x, y, w, h, colour, amount)

    def dot(self, x, y, colour):
        self.px[x, y] = tuple(colour[:3]) + (255,)

    def save(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        self.img.save(path)


SKIN = [(176, 168, 150), (150, 158, 140), (112, 136, 122)]


def body_base(p, stage):
    skin = SKIN[stage]
    for b in (HEAD, BODY, R_ARM, L_ARM, R_LEG, L_LEG):
        p.box(b, skin, 6)
    if stage >= 1:
        # scales: a scatter of darker, greener pixels over the skin, thicker at stage 2
        density = 0.12 if stage == 1 else 0.3
        for b in (HEAD, BODY, R_ARM, L_ARM):
            for x, y, w, h in faces(*b).values():
                for i in range(x, x + w):
                    for j in range(y, y + h):
                        if (i + j) % 2 == 0 and p.rng.random() < density:
                            p.dot(i, j, (skin[0] - 35, skin[1] - 15, skin[2] - 20))


def face(p, stage, hair):
    fx, fy = 8, 8  # head front
    if hair:
        for name in ("top", "back"):
            x, y, w, h = faces(*HEAD)[name]
            p.rect(x, y, w, h if name == "top" else 3, hair, 8)
        for name in ("right", "left"):
            x, y, w, h = faces(*HEAD)[name]
            p.rect(x, y, w, 2, hair, 8)
        p.rect(fx, fy, 8, 1, hair, 8)
    white = (205, 205, 190)
    dark = (25, 25, 25)
    if stage == 0:
        for ex in (fx + 2, fx + 5):
            p.dot(ex, fy + 4, white)
            p.dot(ex + 1, fy + 4, dark)
        p.rect(fx + 3, fy + 6, 2, 1, (95, 70, 65), 0)
    elif stage == 1:
        for ex in (fx + 1, fx + 5):
            p.rect(ex, fy + 3, 2, 2, white, 0)
            p.dot(ex + 1, fy + 4, dark)
        p.rect(fx + 2, fy + 6, 4, 1, (70, 60, 60), 0)
    else:
        # bulging pale fish eyes set wide apart, a lipless gash of a mouth
        for ex in (fx, fx + 6):
            p.rect(ex, fy + 3, 2, 2, (215, 205, 120), 0)
            p.dot(ex + (1 if ex == fx else 0), fy + 4, dark)
        p.rect(fx + 1, fy + 6, 6, 1, (30, 50, 40), 0)
        p.rect(fx + 2, fy + 7, 4, 1, (30, 50, 40), 0)
    if stage >= 1:
        # gill slits down both sides of the head
        for name in ("right", "left"):
            x, y, w, h = faces(*HEAD)[name]
            for row in ((5, 6, 7) if stage == 2 else (6, 7)):
                for i in range(x + 2, x + 6):
                    p.dot(i, y + row, (110, 45, 50))


def straw(p, b):
    for x, y, w, h in faces(*b).values():
        for i in range(x, x + w):
            for j in range(y, y + h):
                if p.rng.random() < 0.85:
                    p.dot(i, j, p.shade((122, 106, 62) if i % 2 else (100, 88, 52), 8))


def role_clothes(p, role, stage):
    if role == "fisher":
        for b in (R_LEG, L_LEG):
            p.box(b, (55, 60, 70), 6)
        for b in (BODY, R_ARM, L_ARM):
            p.box(b, (80, 75, 60), 6)
        # 蓑衣: a straw rain cape over body and arms, all hanging strands
        for b in (JACKET, R_SLEEVE, L_SLEEVE):
            straw(p, b)
        face(p, stage, (40, 36, 32))
        # straw hat on the hat layer
        x, y, w, h = faces(*HAT)["top"]
        p.rect(x, y, w, h, (140, 122, 70), 10)
        for name in SIDES:
            x, y, w, h = faces(*HAT)[name]
            p.rect(x, y, w, 2, (130, 112, 64), 10)
    elif role == "salter":
        p.box(BODY, (168, 164, 150), 10)
        p.box(R_ARM, (168, 164, 150), 10, rows=(0, 5))
        p.box(L_ARM, (168, 164, 150), 10, rows=(0, 5))
        for b in (R_LEG, L_LEG):
            p.box(b, (92, 76, 60), 6)
        x, y, w, h = faces(*BODY)["front"]
        for _ in range(10):
            p.dot(x + p.rng.randrange(w), y + p.rng.randrange(h), (225, 222, 210))  # salt stains
        face(p, stage, (52, 46, 40))
        for name in SIDES:
            x, y, w, h = faces(*HAT)[name]
            p.rect(x, y + 1, w, 1, (200, 196, 182), 6)  # cloth headband
    elif role == "net_mender":
        jacket = (42, 46, 66)
        p.box(BODY, jacket, 6)
        p.box(R_ARM, jacket, 6, rows=(0, 10))
        p.box(L_ARM, jacket, 6, rows=(0, 10))
        for b in (R_LEG, L_LEG):
            p.box(b, (30, 30, 34), 5)
        # an apron tangled with net twine
        x, y, w, h = faces(*BODY)["front"]
        p.rect(x + 1, y + 5, w - 2, h - 5, (90, 86, 72), 6)
        for i in range(x + 1, x + w - 1):
            for j in range(y + 5, y + h):
                if (i + j) % 3 == 0:
                    p.dot(i, j, (150, 146, 128))
        face(p, stage, (128, 128, 122))
        x, y, w, h = faces(*HAT)["back"]
        p.rect(x + 2, y + 1, 4, 3, (118, 118, 112), 6)  # hair bun
    elif role == "porter":
        vest = (70, 55, 40)
        x, y, w, h = faces(*BODY)["front"]
        p.rect(x, y, 2, h, vest, 6)
        p.rect(x + w - 2, y, 2, h, vest, 6)
        x, y, w, h = faces(*BODY)["back"]
        p.rect(x, y, w, h, vest, 6)
        for b in (R_LEG, L_LEG):
            p.box(b, (88, 70, 52), 6, rows=(0, 7))
        x, y, w, h = faces(*BODY)["front"]
        for k in range(8):
            p.dot(x + min(w - 1, k), y + min(h - 1, k + 1), (170, 150, 100))  # rope over the shoulder
        face(p, stage, (35, 30, 28))
    elif role == "keeper":
        robe = (26, 26, 31)
        for b in (BODY, R_ARM, L_ARM, R_LEG, L_LEG, JACKET, R_PANT, L_PANT):
            p.box(b, robe, 5)
        for name in SIDES:
            x, y, w, h = faces(*JACKET)[name]
            p.rect(x, y + 7, w, 2, (100, 30, 30), 6)  # dark red sash
        face(p, stage, None)
        # a black hood framing the face
        for name in ("top", "back", "left", "right"):
            x, y, w, h = faces(*HAT)[name]
            p.rect(x, y, w, h, (20, 20, 24), 4)
        x, y, w, h = faces(*HAT)["front"]
        p.rect(x, y, w, 2, (20, 20, 24), 4)
        p.rect(x, y, 1, h, (20, 20, 24), 4)
        p.rect(x + w - 1, y, 1, h, (20, 20, 24), 4)


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


def main():
    for index, role in enumerate(("fisher", "salter", "net_mender", "porter", "keeper")):
        for stage in range(3):
            p = Painter(index * 10 + stage)
            body_base(p, stage)
            role_clothes(p, role, stage)
            p.save(SKINS / f"{role}_{stage}.png")
    coin()


if __name__ == "__main__":
    main()
