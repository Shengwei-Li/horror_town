"""Builds the townsfolk model for GeckoLib: geometry, animations, and one texture (+ glow mask) per role and stage.

Usage: python tools/gen_townsfolk.py
Almost human, but not quite: a head taller than a player, narrow shoulders, a neck a little too long, thin
legs, arms that hang down to the knee with long fingers. Front is -Z. The fisher's straw hat and the keeper's
robe and hood are bones that the renderer shows only for them.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town"
TEX_W, TEX_H = 128, 64
ROLES = ("fisher", "salter", "net_mender", "porter", "keeper")
SKIN = [(176, 168, 150), (148, 156, 138), (110, 134, 120)]

# ---------------------------------------------------------------- geometry, with a fixed UV layout

faces_of = {}  # cube id -> {face: (u, v, w, h)}
layout = {"x": 0, "y": 0, "row": 0}


def place(w, h):
    if layout["x"] + w > TEX_W:
        layout["x"], layout["y"], layout["row"] = 0, layout["y"] + layout["row"], 0
    pos = (layout["x"], layout["y"])
    layout["x"] += w
    layout["row"] = max(layout["row"], h)
    assert layout["y"] + h <= TEX_H, "townsfolk texture is full"
    return pos


def cube(cid, origin, size):
    sx, sy, sz = size
    dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz), "down": (sx, sz)}
    uv, rects = {}, {}
    for face, (w, h) in dims.items():
        w, h = max(1, math.ceil(w)), max(1, math.ceil(h))
        u, v = place(w, h)
        rects[face] = (u, v, w, h)
        uv[face] = {"uv": [u, v], "uv_size": [w, h]}
    faces_of[cid] = rects
    return {"origin": list(origin), "size": list(size), "uv": uv}


bones = [{"name": "root", "pivot": [0, 0, 0]}]


def bone(name, parent, pivot, cubes=(), rotation=None):
    b = {"name": name, "parent": parent, "pivot": list(pivot)}
    if cubes:
        b["cubes"] = list(cubes)
    if rotation:
        b["rotation"] = list(rotation)
    bones.append(b)


HIP = 17
SHOULDER = 29
bone("body", "root", [0, HIP, 0])
bone("right_leg", "body", [-1.6, HIP, 0], [cube("right_leg", [-3, 0, -1.5], [3, HIP, 3])])
bone("left_leg", "body", [1.6, HIP, 0], [cube("left_leg", [0, 0, -1.5], [3, HIP, 3])])
bone("robe", "body", [0, HIP, 0], [cube("robe", [-4, 0, -2.5], [8, HIP, 5])])
bone("torso", "body", [0, HIP, 0], [cube("torso", [-3.5, HIP, -1.75], [7, 12, 3.5])])
bone("neck", "torso", [0, SHOULDER, 0], [cube("neck", [-1, SHOULDER, -1], [2, 5, 2])])
bone("head", "neck", [0, SHOULDER + 5, 0], [cube("head", [-3, SHOULDER + 5, -3], [6, 7, 6])])
bone("hat", "head", [0, SHOULDER + 12, 0], [
    cube("hat_brim", [-6, SHOULDER + 11, -6], [12, 1, 12]),
    cube("hat_mid", [-3.5, SHOULDER + 12, -3.5], [7, 2, 7]),
    cube("hat_top", [-1.5, SHOULDER + 14, -1.5], [3, 1, 3]),
])
bone("hood", "head", [0, SHOULDER + 5, 0], [
    cube("hood_top", [-3.5, SHOULDER + 12, -3.5], [7, 1, 7.5]),
    cube("hood_back", [-3.5, SHOULDER + 4, 3], [7, 8, 1]),
    cube("hood_right", [-3.5, SHOULDER + 4, -3.5], [0.5, 8, 6.5]),
    cube("hood_left", [3, SHOULDER + 4, -3.5], [0.5, 8, 6.5]),
])
for side, sign in (("right", -1), ("left", 1)):
    x = sign * 4.5
    bone(f"{side}_arm", "torso", [x, SHOULDER - 1, 0],
         [cube(f"{side}_arm", [x - 1.25, SHOULDER - 20, -1.25], [2.5, 20, 2.5])])
    bone(f"{side}_hand", f"{side}_arm", [x, SHOULDER - 20, 0],
         [cube(f"{side}_hand", [x - 1, SHOULDER - 25, -1], [2, 5, 2])])

geo = {"format_version": "1.12.0", "minecraft:geometry": [{
    "description": {"identifier": "geometry.townsfolk", "texture_width": TEX_W, "texture_height": TEX_H,
                    "visible_bounds_width": 3, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
    "bones": bones}]}
(ROOT / "geo/entity").mkdir(parents=True, exist_ok=True)
(ROOT / "geo/entity/townsfolk.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8")

# ---------------------------------------------------------------- painting


def clothes(role):
    """(torso, sleeves, legs, hair) colours per role; None hair = bald (under a hood)."""
    return {
        "fisher": ((96, 86, 58), (96, 86, 58), (55, 60, 70), (40, 36, 32)),
        "salter": ((168, 164, 150), (168, 164, 150), (92, 76, 60), (52, 46, 40)),
        "net_mender": ((42, 46, 66), (42, 46, 66), (30, 30, 34), (128, 128, 122)),
        "porter": ((70, 55, 40), None, (88, 70, 52), (35, 30, 28)),
        "keeper": ((26, 26, 31), (26, 26, 31), (26, 26, 31), None),
    }[role]


def paint(role, stage):
    rng = random.Random(ROLES.index(role) * 10 + stage)
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    glow = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    px, gpx = img.load(), glow.load()
    skin = SKIN[stage]
    torso_c, sleeve_c, leg_c, hair = clothes(role)

    def put(x, y, c, n=8, glowing=False):
        c = tuple(max(0, min(255, k + rng.randint(-n, n))) for k in c[:3]) + (255,)
        px[x, y] = c
        if glowing:
            gpx[x, y] = c

    def fill(cid, colour_at):
        for face, (u, v, w, h) in faces_of[cid].items():
            for i in range(w):
                for j in range(h):
                    c = colour_at(face, i, j, w, h)
                    put(u + i, v + j, c)

    def skin_at(face, i, j, w, h):
        if stage >= 1 and (i + j) % 2 == 0 and rng.random() < (0.15 if stage == 1 else 0.35):
            return (skin[0] - 35, skin[1] - 15, skin[2] - 20)
        return skin

    # head: hair, then a face that changes with the stage
    def head_at(face, i, j, w, h):
        if hair and (face in ("up", "south") or (face in ("east", "west") and j < 2) or (face == "north" and j == 0)):
            return hair
        return skin_at(face, i, j, w, h)

    fill("head", head_at)
    u, v, w, h = faces_of["head"]["north"]
    dark, white = (20, 20, 18), (205, 205, 190)
    if stage == 0:
        # small eyes set a little too close together, a thin mouth
        put(u + 1, v + 3, white, 2)
        put(u + 2, v + 3, dark, 2)
        put(u + 3, v + 3, dark, 2)
        put(u + 4, v + 3, white, 2)
        put(u + 2, v + 5, (95, 70, 65), 2)
        put(u + 3, v + 5, (95, 70, 65), 2)
    elif stage == 1:
        for ex in (0, 4):
            for dx in range(2):
                for dy in range(2):
                    put(u + ex + dx, v + 2 + dy, white, 3)
            put(u + ex + (1 if ex == 0 else 0), v + 3, dark, 2)
        for x in range(1, 5):
            put(u + x, v + 5, (70, 60, 60), 2)
    else:
        eye = (220, 210, 120)
        for ex in (0, 4):
            for dx in range(2):
                for dy in range(2):
                    put(u + ex + dx, v + 2 + dy, eye, 3, glowing=True)
            put(u + ex + (1 if ex == 0 else 0), v + 3, dark, 1, glowing=True)
        for x in range(6):
            put(u + x, v + 5, (28, 46, 38), 2)
        for x in range(1, 5):
            put(u + x, v + 6, (28, 46, 38), 2)

    def neck_at(face, i, j, w, h):
        if stage >= 1 and face in ("east", "west") and j in ((1, 2, 3) if stage == 2 else (2, 3)):
            return (110, 45, 50)  # gill slits
        return skin_at(face, i, j, w, h)

    fill("neck", neck_at)

    def torso_at(face, i, j, w, h):
        if role == "fisher" and face != "up":
            return (122, 106, 62) if i % 2 else (100, 88, 52)  # 蓑衣, a straw rain cape
        if role == "porter" and face == "north" and 1 < i < w - 2:
            return skin_at(face, i, j, w, h)  # open vest
        if role == "net_mender" and face == "north" and j > 4:
            return (150, 146, 128) if (i + j) % 3 == 0 else (90, 86, 72)  # apron tangled with twine
        if role == "keeper" and j in (9, 10):
            return (100, 30, 30)  # red sash
        if role == "salter" and face == "north" and rng.random() < 0.12:
            return (225, 222, 210)  # salt stains
        return torso_c

    fill("torso", torso_at)

    def arm_at(face, i, j, w, h):
        if face in ("up",):
            return sleeve_c or skin
        sleeve_rows = {"fisher": 12, "salter": 7, "net_mender": 14, "porter": 0, "keeper": 18}[role]
        if j < sleeve_rows and sleeve_c:
            return (122, 106, 62) if role == "fisher" and i % 2 else sleeve_c
        return skin_at(face, i, j, w, h)

    for side in ("right", "left"):
        fill(f"{side}_arm", arm_at)

        def hand_at(face, i, j, w, h):
            if j >= h - 1:
                return (40, 38, 34) if stage == 2 else (120, 110, 96)  # long nails
            return skin_at(face, i, j, w, h)

        fill(f"{side}_hand", hand_at)

    def leg_at(face, i, j, w, h):
        if j >= h - 2:
            return (34, 30, 28)  # bare, filthy feet
        if role == "porter" and j > 8:
            return skin_at(face, i, j, w, h)  # shorts
        return leg_c

    fill("right_leg", leg_at)
    fill("left_leg", leg_at)
    fill("robe", lambda face, i, j, w, h: (100, 30, 30) if role == "keeper" and j < 2 else (26, 26, 31))
    for cid in ("hat_brim", "hat_mid", "hat_top"):
        fill(cid, lambda face, i, j, w, h: (140, 122, 70) if (i + j) % 2 else (118, 102, 58))
    for cid in ("hood_top", "hood_back", "hood_right", "hood_left"):
        fill(cid, lambda face, i, j, w, h: (20, 20, 24))

    out = ROOT / "textures/entity/townsfolk"
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / f"{role}_{stage}.png")
    if stage == 2:  # only fish eyes glow; GeckoLib rejects an empty glow mask
        glow.save(out / f"{role}_{stage}_glowmask.png")


for role in ROLES:
    for stage in range(3):
        paint(role, stage)

# ---------------------------------------------------------------- animations

LENGTH_IDLE, LENGTH_WALK = 6.0, 1.6


def frames(length, steps, fn):
    return {f"{length * i / steps:.3f}": [round(v, 3) for v in fn(i / steps)] for i in range(steps + 1)}


s = math.sin
tau = 2 * math.pi
anims = {
    # standing: barely breathing, now and then the head gives a small sharp twitch
    "animation.townsfolk.idle": {"loop": True, "animation_length": LENGTH_IDLE, "bones": {
        "torso": {"rotation": frames(LENGTH_IDLE, 12, lambda t: (1.5 * s(tau * t), 0, 0))},
        "right_arm": {"rotation": frames(LENGTH_IDLE, 12, lambda t: (2 * s(tau * t + 0.5), 0, 2))},
        "left_arm": {"rotation": frames(LENGTH_IDLE, 12, lambda t: (2 * s(tau * t + 1.5), 0, -2))},
        "neck": {"rotation": {"0.000": [0, 0, 0], "3.900": [0, 0, 0], "4.000": [0, 0, 9], "4.250": [0, 0, 9],
                              "4.350": [0, 0, 0], "6.000": [0, 0, 0]}},
    }},
    # walking: long stiff strides, arms hanging almost still, the head never bobbing
    "animation.townsfolk.walk": {"loop": True, "animation_length": LENGTH_WALK, "bones": {
        "right_leg": {"rotation": frames(LENGTH_WALK, 8, lambda t: (28 * s(tau * t), 0, 0))},
        "left_leg": {"rotation": frames(LENGTH_WALK, 8, lambda t: (-28 * s(tau * t), 0, 0))},
        "right_arm": {"rotation": frames(LENGTH_WALK, 8, lambda t: (-6 * s(tau * t), 0, 2))},
        "left_arm": {"rotation": frames(LENGTH_WALK, 8, lambda t: (6 * s(tau * t), 0, -2))},
        "torso": {"rotation": frames(LENGTH_WALK, 8, lambda t: (4, 2 * s(tau * t), 0))},
    }},
}
(ROOT / "animations/entity").mkdir(parents=True, exist_ok=True)
(ROOT / "animations/entity/townsfolk.animation.json").write_text(
    json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")
print("texture rows used", layout["y"] + layout["row"])
