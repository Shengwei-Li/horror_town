"""Builds the 潮母 (Tide Mother) idol for GeckoLib: geometry, texture, glow mask and idle animation.

Usage: python tools/gen_idol.py
The model is built at half size (about 8 blocks tall) and rendered at 2x, so its pixels are a little
chunkier than a block's. Front is -Z (north). A hooded, faceless figure: no face under the hood, only a cluster
of pale eyes; a robe that rots into a skirt of tentacles spilling across the floor; long arms with hooked claws
reaching forward and down.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town"
NAME = "tide_mother"
ATLAS_W, ATLAS_H = 512, 512
rng = random.Random(1890)

# ---------------------------------------------------------------- materials (how each face gets painted)

PALETTE = {
    "robe": ((28, 38, 34), 10),
    "flesh": ((62, 78, 70), 10),
    "tentacle": ((34, 54, 48), 12),
    "hood": ((20, 22, 24), 6),
    "void": ((6, 6, 8), 2),
    "eye": ((226, 222, 196), 6),
    "claw": ((196, 190, 164), 10),
}


class Atlas:
    """Packs face rectangles into the texture, shelf by shelf."""

    def __init__(self):
        self.x = self.y = self.row = 0
        self.img = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (ATLAS_W, ATLAS_H), (0, 0, 0, 0))

    def place(self, w, h):
        w, h = max(1, w), max(1, h)
        if self.x + w > ATLAS_W:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        assert self.y + h <= ATLAS_H, "idol texture atlas is full"
        pos = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return pos

    def paint(self, u, v, w, h, material, face):
        base, noise = PALETTE[material]
        px = self.img.load()
        gpx = self.glow.load()
        for i in range(w):
            for j in range(h):
                c = [base[k] + rng.randint(-noise, noise) for k in range(3)]
                if material == "robe":
                    if i % 5 == 0:
                        c = [k - 8 for k in c]  # folds
                    if (i + 2 * j) % 7 == 0 and rng.random() < 0.5:
                        c = [k + 14 for k in c]  # a sheen of scales
                elif material == "tentacle":
                    if face == "down" and (i % 4 == 1) and (j % 3 == 1):
                        c = [150, 160, 140]  # suckers along the underside
                    elif (i + j) % 6 == 0:
                        c = [k + 10 for k in c]
                elif material == "flesh":
                    if rng.random() < 0.05:
                        c = [k - 25 for k in c]  # veins and blotches
                elif material == "eye":
                    centre = (w // 2, h // 2)
                    if (i, j) == centre or (w > 3 and abs(i - centre[0]) + abs(j - centre[1]) == 0):
                        c = [12, 12, 10]  # pupil
                    gpx[u + i, v + j] = tuple(max(0, min(255, k)) for k in c) + (255,)
                px[u + i, v + j] = tuple(max(0, min(255, k)) for k in c) + (255,)


atlas = Atlas()


def cube(origin, size, material, face_materials=None):
    """A cube with per-face UVs, each face painted with its material."""
    sx, sy, sz = size
    faces = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    uv = {}
    for face, (w, h) in faces.items():
        w, h = max(1, math.ceil(w)), max(1, math.ceil(h))
        u, v = atlas.place(w, h)
        mat = (face_materials or {}).get(face, material)
        atlas.paint(u, v, w, h, mat, face)
        uv[face] = {"uv": [u, v], "uv_size": [w, h]}
    return {"origin": list(origin), "size": list(size), "uv": uv}


bones = [{"name": "root", "pivot": [0, 0, 0]}]


def bone(name, parent, pivot, cubes=(), rotation=None):
    b = {"name": name, "parent": parent, "pivot": list(pivot), "cubes": list(cubes)}
    if rotation:
        b["rotation"] = list(rotation)
    bones.append(b)
    return name


# ---------------------------------------------------------------- body

bone("body", "root", [0, 0, 0], [
    cube([-28, 0, -20], [56, 22, 40], "robe"),
    cube([-25, 22, -17], [50, 22, 34], "robe"),
    cube([-21, 44, -14], [42, 24, 28], "robe"),
])
bone("torso", "body", [0, 68, 0], [
    cube([-17, 68, -12], [34, 26, 24], "robe", {"north": "flesh"}),
    cube([-20, 88, -10], [40, 8, 20], "robe"),  # shoulders
])
# a few eyes open in the chest
for ex, ey in [(-9, 80), (6, 74), (0, 86), (11, 83)]:
    bone(f"chest_eye_{ex}_{ey}", "torso", [ex, ey, -12], [cube([ex - 1.5, ey - 1.5, -13], [3, 3, 1], "eye")])

# ---------------------------------------------------------------- hood and the eyes inside it

bone("hood", "torso", [0, 96, 0], [
    cube([-16, 124, -14], [32, 6, 30], "hood"),       # crown
    cube([-17, 96, -14], [5, 30, 30], "hood"),        # right side
    cube([12, 96, -14], [5, 30, 30], "hood"),         # left side
    cube([-12, 96, 10], [24, 28, 6], "hood"),         # back
    cube([-14, 116, -18], [28, 8, 6], "hood"),        # brow, overhanging the face
    cube([-12, 96, -8], [24, 28, 2], "void"),         # nothing where the face should be
])
eyes = [(-6, 112, 3), (5, 113, 3), (0, 108, 4), (-8, 104, 2), (8, 105, 2), (-2, 101, 3), (4, 99, 2),
        (-5, 118, 2), (6, 119, 2), (0, 115, 2), (-9, 110, 2), (9, 99, 2)]
for i, (ex, ey, s) in enumerate(eyes):
    bone(f"eye_{i}", "hood", [ex, ey, -9], [cube([ex - s / 2, ey - s / 2, -9.5], [s, s, 1], "eye")])

# ---------------------------------------------------------------- arms: long, thin, reaching, hooked claws

for side, sign in (("right", -1), ("left", 1)):
    sx = sign * 22
    bone(f"{side}_upper_arm", "torso", [sx, 92, 0], [cube([sx - 3, 54, -3], [6, 40, 6], "robe")],
         rotation=[35, 0, sign * -8])
    bone(f"{side}_forearm", f"{side}_upper_arm", [sx, 55, 0], [cube([sx - 2.5, 17, -2.5], [5, 40, 5], "flesh")],
         rotation=[40, 0, 0])
    bone(f"{side}_hand", f"{side}_forearm", [sx, 18, 0], [cube([sx - 4, 10, -3], [8, 8, 6], "flesh")])
    for f in range(4):
        fx = sx - 3 + f * 2
        bone(f"{side}_claw_{f}", f"{side}_hand", [fx, 11, -2], [cube([fx - 0.75, -3, -2.75], [1.5, 14, 1.5], "claw")],
             rotation=[20 + f * 4, 0, 0])

# ---------------------------------------------------------------- tentacles: the robe's hem coils out over the floor

TENTACLES = 11
SEGMENTS = 4
for t in range(TENTACLES):
    angle = 360.0 * t / TENTACLES + rng.uniform(-10, 10)
    root = f"tentacle_{t}"
    bone(root, "body", [0, 4, 0], rotation=[0, angle, 0])
    parent = root
    x = 24
    for s in range(SEGMENTS):
        length = 16 - s * 2
        thick = 10 - s * 2
        name = f"tentacle_{t}_{s}"
        curl = (6 + s * 10) * (1 if t % 2 else -1) * 0.5 + (12 if s == SEGMENTS - 1 else 0)
        bone(name, parent, [x, 4, 0], [cube([x, 4 - thick / 2, -thick / 2], [length, thick, thick], "tentacle")],
             rotation=[0, curl * 0.6, 10 + s * 12])
        parent = name
        x += length

# ---------------------------------------------------------------- write geometry

geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": f"geometry.{NAME}", "texture_width": ATLAS_W, "texture_height": ATLAS_H,
                    "visible_bounds_width": 12, "visible_bounds_height": 12, "visible_bounds_offset": [0, 4, 0]},
    "bones": bones}]}
for b in bones:
    if not b.get("cubes"):
        b.pop("cubes", None)
(ROOT / "geo/entity").mkdir(parents=True, exist_ok=True)
(ROOT / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8")
(ROOT / "textures/entity").mkdir(parents=True, exist_ok=True)
atlas.img.save(ROOT / f"textures/entity/{NAME}.png")
atlas.glow.save(ROOT / f"textures/entity/{NAME}_glowmask.png")

# ---------------------------------------------------------------- idle animation: slow, breathing, never still

LENGTH = 8.0
STEPS = 16


def wave(amplitude, phase, base=(0, 0, 0), axis=0, cycles=1):
    frames = {}
    for i in range(STEPS + 1):
        t = LENGTH * i / STEPS
        value = list(base)
        value[axis] += amplitude * math.sin(2 * math.pi * cycles * i / STEPS + phase)
        frames[f"{t:.3f}"] = [round(v, 3) for v in value]
    return frames


anim_bones = {
    "torso": {"rotation": wave(1.5, 0.0), "scale": {f"{LENGTH * i / STEPS:.3f}": [
        round(1 + 0.015 * math.sin(2 * math.pi * i / STEPS), 4)] * 3 for i in range(STEPS + 1)}},
    "hood": {"rotation": wave(4.0, 1.3, axis=2)},
    "right_upper_arm": {"rotation": wave(3.0, 0.4, base=(35, 0, 8))},
    "left_upper_arm": {"rotation": wave(3.0, 2.0, base=(35, 0, -8))},
}
for t in range(TENTACLES):
    for s in range(SEGMENTS):
        b = next(x for x in bones if x["name"] == f"tentacle_{t}_{s}")
        anim_bones[f"tentacle_{t}_{s}"] = {"rotation": wave(5 + s * 3, t * 0.9 + s * 0.6, base=tuple(b["rotation"]),
                                                             axis=1, cycles=1)}
animation = {"format_version": "1.8.0", "animations": {f"animation.{NAME}.idle": {
    "loop": True, "animation_length": LENGTH, "bones": anim_bones}}}
(ROOT / "animations/entity").mkdir(parents=True, exist_ok=True)
(ROOT / f"animations/entity/{NAME}.animation.json").write_text(json.dumps(animation, indent=1), encoding="utf-8")
print("bones", len(bones), "atlas rows used up to", atlas.y + atlas.row)
