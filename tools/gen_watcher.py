"""Builds 观潮者 (the Watcher in the Tide) for GeckoLib: geometry, texture, glow mask and animations.

Usage: python tools/gen_watcher.py
After references/watcher_c.png: an enormous pale bald head risen out of the sea up to the chin, eyes wide
open and staring, a grin far too wide and full of small teeth, long thin fingers hooked over the water on either
side. Built at 1/6 size and drawn 6x (about 20 blocks tall). Front is -Z.
Bones the game drives: jaw (opens), *_lid (blinks), *_pupil (follows you), *_finger_* (curl).
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town"
NAME = "watcher"
TEX_W, TEX_H = 256, 256
rng = random.Random(1924)
SKIN = (178, 190, 176)

atlas_pos = {"x": 0, "y": 0, "row": 0}
img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
glow = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
px, gpx = img.load(), glow.load()


def place(w, h):
    if atlas_pos["x"] + w > TEX_W:
        atlas_pos["x"], atlas_pos["y"], atlas_pos["row"] = 0, atlas_pos["y"] + atlas_pos["row"], 0
    assert atlas_pos["y"] + h <= TEX_H, "watcher texture is full"
    pos = (atlas_pos["x"], atlas_pos["y"])
    atlas_pos["x"] += w
    atlas_pos["row"] = max(atlas_pos["row"], h)
    return pos


def shade(c, k):
    return [max(0, min(255, int(v * k))) for v in c]


def skin(i, j, w, h, face):
    """Pale, damp skin: lit from above, darker towards the water, blotched."""
    k = {"up": 1.12, "down": 0.55}.get(face, 1.05 - 0.35 * j / max(1, h - 1))
    if face in ("east", "west"):
        k *= 0.88
    c = shade(SKIN, k)
    c = [v + rng.randint(-6, 6) for v in c]
    if rng.random() < 0.05:
        c = [v - 22 for v in c]
    return c, False


def cube(origin, size, painter=skin):
    sx, sy, sz = size
    dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    uv = {}
    for face, (w, h) in dims.items():
        w, h = max(1, math.ceil(w)), max(1, math.ceil(h))
        u, v = place(w, h)
        for i in range(w):
            for j in range(h):
                c, glows = painter(i, j, w, h, face)
                c = tuple(max(0, min(255, k)) for k in c) + (255,)
                px[u + i, v + j] = c
                if glows:
                    gpx[u + i, v + j] = c
        uv[face] = {"uv": [u, v], "uv_size": [w, h]}
    return {"origin": list(origin), "size": list(size), "uv": uv}


def solid(colour, glows=False, noise=5):
    return lambda i, j, w, h, face: ([v + rng.randint(-noise, noise) for v in colour], glows)


# ---------------------------------------------------------------- the face, painted onto the front of the skull

FACE_W, FACE_H = 40, 34  # skull front: x -20..20, y 16..50


def face_painter(i, j, w, h, face):
    c, _ = skin(i, j, w, h, face)
    if face != "north":
        return c, False
    x = i - w / 2 + 0.5          # -20..20, left to right as you look at it
    y = (h - 1 - j) + 16         # model height 16..50
    # deep sockets round the eyes
    for ex in (-9.5, 9.5):
        d = math.hypot((x - ex) / 7.5, (y - 33) / 6.0)
        if d < 1.0:
            return shade(c, 0.35 + 0.4 * d * d), False
    # heavy brow ridge and furrows
    if 39 <= y <= 41 and abs(x) < 17:
        return shade(c, 0.78), False
    if y in (44, 46) and abs(x) < 8 and (i % 3):
        return shade(c, 0.8), False
    # a flat, broad nose
    if 24 <= y <= 30 and abs(x) < 3:
        return shade(c, 0.86 if y > 25 else 0.6), False
    # the grin's creases running up from the corners of the mouth into the cheeks
    for side in (-1, 1):
        cx = side * (15 + (y - 17) * 0.55)
        if 17 <= y <= 26 and abs(x - cx) < 0.9:
            return shade(c, 0.62), False
    return c, False


def eye_painter(i, j, w, h, face):
    if face != "north":
        return [200, 196, 172], True
    # bloodshot white
    c = [222, 218, 196] if rng.random() > 0.08 else [170, 92, 88]
    return c, True


def teeth_painter(i, j, w, h, face):
    if face != "north":
        return [60, 22, 26], False
    if i % 3 == 0:
        return [50, 30, 28], False  # gaps
    c = [212, 204, 170] if rng.random() > 0.15 else [176, 160, 110]
    return c, False


bones = [{"name": "root", "pivot": [0, 0, 0]}]


def bone(name, parent, pivot, cubes=(), rotation=None):
    b = {"name": name, "parent": parent, "pivot": list(pivot)}
    if cubes:
        b["cubes"] = list(cubes)
    if rotation:
        b["rotation"] = list(rotation)
    bones.append(b)


bone("head", "root", [0, 16, 0], [
    cube([-20, 16, -17], [40, FACE_H, 34], face_painter),
    cube([-18, 50, -15], [36, 5, 30]),
    cube([-14, 55, -11], [28, 3, 22]),
    cube([-9, 58, -6], [18, 1.5, 12]),
    cube([-23, 26, -4], [3, 12, 8]),     # right ear
    cube([20, 26, -4], [3, 12, 8]),      # left ear
    cube([-14, 9, -14], [28, 8, 26], solid((12, 8, 10), noise=2)),   # the dark inside of the mouth
    cube([-15, 13, -18], [30, 4, 2], teeth_painter),                 # upper teeth
])
# the lower jaw: hinged near the ears, it swings down to open the mouth
bone("jaw", "head", [0, 16, 8], [
    cube([-18, 2, -16], [36, 9, 28]),
    cube([-14, 11, -17], [28, 3, 2], teeth_painter),                 # lower teeth
])
for side, sign in (("right", -1), ("left", 1)):
    ex = sign * 9.5
    bone(f"{side}_eye", "head", [ex, 33, -17], [cube([ex - 4.5, 29, -18], [9, 8, 1.5], eye_painter)])
    bone(f"{side}_pupil", f"{side}_eye", [ex, 33, -18.5], [cube([ex - 1, 32, -18.8], [2, 2, 0.5],
                                                                  solid((14, 14, 12), glows=False, noise=1))])
    # the lid: a flap of skin above the eye that drops down over it to blink (scaled to nothing when open)
    bone(f"{side}_lid", f"{side}_eye", [ex, 37, -18.5], [cube([ex - 5, 29, -19.2], [10, 8, 1])])
    # a hand hooked over the edge of the water on either side, long fingers splayed
    hx = sign * 38
    bone(f"{side}_hand", "root", [hx, 4, -6], [cube([hx - 6, 2, -12], [12, 3, 12])])
    for f in range(4):
        fx = hx - 5 + f * 3.3
        bone(f"{side}_finger_{f}", f"{side}_hand", [fx, 4, -12], [cube([fx - 0.9, 3, -26], [1.8, 1.8, 14])],
             rotation=[-12, (f - 1.5) * 8 * sign, 0])

geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": f"geometry.{NAME}", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 16, "visible_bounds_height": 12, "visible_bounds_offset": [0, 5, 0]},
    "bones": bones}]}
(ROOT / "geo/entity").mkdir(parents=True, exist_ok=True)
(ROOT / f"geo/entity/{NAME}.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8")
img.save(ROOT / f"textures/entity/{NAME}.png")
glow.save(ROOT / f"textures/entity/{NAME}_glowmask.png")

# ---------------------------------------------------------------- animations


def frames(length, steps, fn):
    return {f"{length * i / steps:.3f}": [round(v, 4) for v in fn(i / steps)] for i in range(steps + 1)}


tau = 2 * math.pi
lids_open = {"0": [1, 0.02, 1]}
fingers = {}
for side, sign in (("right", -1), ("left", 1)):
    for f in range(4):
        base = (-12, (f - 1.5) * 8 * sign, 0)
        fingers[f"{side}_finger_{f}"] = {"rotation": frames(12.0, 24, lambda t, b=base, f=f: (
            b[0] - 10 * max(0.0, math.sin(tau * t + f * 0.7)), b[1], b[2]))}

animations = {
    # the head barely moves: a slow sway, a breath; the fingers flex now and then
    f"animation.{NAME}.idle": {"loop": True, "animation_length": 12.0, "bones": {
        "head": {"rotation": frames(12.0, 24, lambda t: (1.2 * math.sin(tau * t), 0, 1.6 * math.sin(tau * t * 0.5)))},
        "right_lid": {"scale": lids_open},
        "left_lid": {"scale": lids_open},
        **fingers,
    }},
    # a blink: the lids drop, hold for a heartbeat, and lift
    f"animation.{NAME}.blink": {"loop": False, "animation_length": 0.5, "bones": {
        side + "_lid": {"scale": {"0.000": [1, 0.02, 1], "0.120": [1, 1, 1], "0.300": [1, 1, 1], "0.500": [1, 0.02, 1]}}
        for side in ("right", "left")}},
    # the grin shut tight
    f"animation.{NAME}.closed": {"loop": True, "animation_length": 1.0, "bones": {
        "jaw": {"rotation": {"0": [0, 0, 0]}}}},
    # while it has you: the jaw sags open, wider and wider, trembling
    f"animation.{NAME}.gape": {"loop": True, "animation_length": 4.0, "bones": {
        "jaw": {"rotation": frames(4.0, 16, lambda t: (22 + 4 * math.sin(tau * t) + (1.5 if int(t * 16) % 2 else 0), 0, 0))}}},
}
(ROOT / "animations/entity").mkdir(parents=True, exist_ok=True)
(ROOT / f"animations/entity/{NAME}.animation.json").write_text(
    json.dumps({"format_version": "1.8.0", "animations": animations}, indent=1), encoding="utf-8")
print("texture rows used", atlas_pos["y"] + atlas_pos["row"])
