"""Builds the Fog Harbor monsters and creatures for GeckoLib (geometry, texture, glow mask, animations) and the
icons of what they drop.

Usage: python tools/gen_mobs.py
Every creature keeps a clear, readable silhouette with one thing badly wrong about it: hair where a face should
be, a human eye on a crab, a ring of human teeth in a snail's shell, a human face on a fish. Front is -Z.
"""
import json
import math
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/horror_town"
rng = random.Random(1932)
TEX = 128


def clamp(c):
    return tuple(max(0, min(255, int(v))) for v in c[:3])


# ---------------------------------------------------------------- materials: (i, j, w, h, face) -> (rgb, glows)

def flat(colour, noise=8, light=True):
    def paint(i, j, w, h, face):
        k = {"up": 1.1, "down": 0.6}.get(face, 1.05 - 0.3 * j / max(1, h - 1)) if light else 1.0
        return [v * k + rng.randint(-noise, noise) for v in colour], False
    return paint


def blotched(colour, spot, chance=0.12, noise=8):
    base = flat(colour, noise)

    def paint(i, j, w, h, face):
        c, g = base(i, j, w, h, face)
        return (list(spot) if rng.random() < chance else c), g
    return paint


def striped(a, b, period=3, noise=6):
    def paint(i, j, w, h, face):
        c = a if (j // period) % 2 == 0 else b
        return flat(c, noise)(i, j, w, h, face)
    return paint


def glowing(colour):
    return lambda i, j, w, h, face: (list(colour), True)


def with_front(material, front):
    """A material whose front (north) face is painted by another painter."""
    def paint(i, j, w, h, face):
        return front(i, j, w, h, face) if face == "north" else material(i, j, w, h, face)
    return paint


def sheet(spec):
    """Draws a front face from a little pixel map: rows of characters, each mapped to a colour or skipped."""
    rows, palette, under = spec

    def paint(i, j, w, h, face):
        if j < len(rows) and i < len(rows[j]) and rows[j][i] in palette:
            c = palette[rows[j][i]]
            return list(c[:3]), len(c) > 3 and c[3]
        return under(i, j, w, h, face)
    return paint


# ---------------------------------------------------------------- model building


class Model:
    def __init__(self, name, tex=TEX):
        self.name, self.tex = name, tex
        self.x = self.y = self.row = 0
        self.img = Image.new("RGBA", (tex, tex), (0, 0, 0, 0))
        self.glow = Image.new("RGBA", (tex, tex), (0, 0, 0, 0))
        self.bones = [{"name": "root", "pivot": [0, 0, 0]}]
        self.has_glow = False

    def place(self, w, h):
        if self.x + w > self.tex:
            self.x, self.y, self.row = 0, self.y + self.row, 0
        assert self.y + h <= self.tex, f"{self.name}: texture full"
        p = (self.x, self.y)
        self.x += w
        self.row = max(self.row, h)
        return p

    def cube(self, origin, size, paint):
        sx, sy, sz = size
        dims = {"north": (sx, sy), "south": (sx, sy), "east": (sz, sy), "west": (sz, sy), "up": (sx, sz),
                "down": (sx, sz)}
        uv = {}
        px, gpx = self.img.load(), self.glow.load()
        for face, (w, h) in dims.items():
            w, h = max(1, math.ceil(w)), max(1, math.ceil(h))
            u, v = self.place(w, h)
            for i in range(w):
                for j in range(h):
                    c, g = paint(i, j, w, h, face)
                    px[u + i, v + j] = clamp(c) + (255,)
                    if g:
                        gpx[u + i, v + j] = clamp(c) + (255,)
                        self.has_glow = True
            uv[face] = {"uv": [u, v], "uv_size": [w, h]}
        return {"origin": list(origin), "size": list(size), "uv": uv}

    def bone(self, name, parent, pivot, cubes=(), rotation=None):
        b = {"name": name, "parent": parent, "pivot": list(pivot)}
        if cubes:
            b["cubes"] = [self.cube(*c) for c in cubes]
        if rotation:
            b["rotation"] = list(rotation)
        self.bones.append(b)

    def save(self, animations):
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": f"geometry.{self.name}", "texture_width": self.tex,
                            "texture_height": self.tex, "visible_bounds_width": 4, "visible_bounds_height": 4,
                            "visible_bounds_offset": [0, 1.5, 0]},
            "bones": self.bones}]}
        (ROOT / "geo/entity").mkdir(parents=True, exist_ok=True)
        (ROOT / f"geo/entity/{self.name}.geo.json").write_text(json.dumps(geo, indent=1), encoding="utf-8")
        self.img.save(ROOT / f"textures/entity/{self.name}.png")
        glow_path = ROOT / f"textures/entity/{self.name}_glowmask.png"
        if self.has_glow:
            self.glow.save(glow_path)
        elif glow_path.exists():
            glow_path.unlink()
        anims = {f"animation.{self.name}.{k}": v for k, v in animations.items()}
        (ROOT / "animations/entity").mkdir(parents=True, exist_ok=True)
        (ROOT / f"animations/entity/{self.name}.animation.json").write_text(
            json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1), encoding="utf-8")


def frames(length, steps, fn):
    return {f"{length * i / steps:.3f}": [round(v, 3) for v in fn(i / steps)] for i in range(steps + 1)}


TAU = 2 * math.pi


def swing(length, amplitude, phase=0.0, base=(0, 0, 0), axis=0):
    def fn(t):
        v = list(base)
        v[axis] += amplitude * math.sin(TAU * t + phase)
        return v
    return {"rotation": frames(length, 8, fn)}


def loop(length, bones):
    return {"loop": True, "animation_length": length, "bones": bones}


def humanoid(m, h):
    """Bones: body > torso > neck > head, right/left arm; body > right/left leg. h holds the measurements.
    "hunch" bends the torso forward at the hips; "neck" turns the head back up so a bent thing still looks at you."""
    hip, torso_h, leg_w, arm_len = h["hip"], h["torso"], h["leg_w"], h["arm"]
    tw, td = h["torso_w"], h["torso_d"]
    shoulder = hip + torso_h
    m.bone("body", "root", [0, hip, 0])
    m.bone("right_leg", "body", [-leg_w / 2 - 0.2, hip, 0], [([-leg_w - 0.2, 0, -leg_w / 2], [leg_w, hip, leg_w], h["legs"])])
    m.bone("left_leg", "body", [leg_w / 2 + 0.2, hip, 0], [([0.2, 0, -leg_w / 2], [leg_w, hip, leg_w], h["legs"])])
    m.bone("torso", "body", [0, hip, 0], [([-tw / 2, hip, -td / 2], [tw, torso_h, td], h["torso_paint"])],
           rotation=h.get("hunch"))
    m.bone("neck", "torso", [0, shoulder, 0], rotation=h.get("neck"))
    hw, hh, hd = h["head"]
    m.bone("head", "neck", [0, shoulder, 0], [([-hw / 2, shoulder, -hd / 2], [hw, hh, hd], h["head_paint"])]
           + h.get("head_extra", []))
    aw = h["arm_w"]
    for side, sign in (("right", -1), ("left", 1)):
        x = sign * (tw / 2 + aw / 2)
        m.bone(f"{side}_arm", "torso", [x, shoulder - 1, 0],
               [([x - aw / 2, shoulder - 1 - arm_len, -aw / 2], [aw, arm_len, aw], h["arms"])],
               rotation=h.get("arm_pose"))
    return shoulder


def walk_anim(stride=30, arm_swing=0, arm_base=(0, 0, 0), length=1.2):
    bones = {"right_leg": swing(length, stride), "left_leg": swing(length, stride, math.pi)}
    if arm_swing or any(arm_base):
        bones["right_arm"] = swing(length, arm_swing, math.pi, arm_base)
        bones["left_arm"] = swing(length, arm_swing, 0.0, arm_base)
    return loop(length, bones)


def idle_anim(arm_base=(0, 0, 0), length=4.0, sway=False):
    bones = {"torso": swing(length, 1.5)}
    if sway or any(arm_base):
        bones["right_arm"] = swing(length, 2.0, 0.0, arm_base)
        bones["left_arm"] = swing(length, 2.0, 1.0, arm_base)
    return loop(length, bones)


# ---------------------------------------------------------------- 水鬼: the drowned one

def drowned_one():
    """Not a walker: a bloated trunk slung low between four very long, very thin limbs, hair hanging off the front
    where the face should be. It comes up out of the water the way a spider would."""
    m = Model("drowned_one")
    skin = blotched((112, 132, 140), (86, 100, 110), 0.18)
    hair = striped((26, 34, 30), (40, 50, 44), 1)
    rag = blotched((58, 62, 58), (40, 42, 40), 0.2)
    m.bone("body", "root", [0, 14, 0], [([-4.5, 12, -7], [9, 6, 15], with_front(rag, skin)),
                                        ([-3.5, 13, 8], [7, 4, 4], skin)])
    m.bone("head", "body", [0, 16, -7], [([-3.5, 11, -13], [7, 7, 6], with_front(skin, hair)),
                                         ([-3.5, 3, -13.5], [7, 8, 1], hair), ([-3, 1, -12.8], [6, 2, 1], hair)])
    # each limb: an upper bone rising steeply out from the trunk, a lower one reaching back down to the ground
    for name, x, z, yaw in (("right_arm", -4.5, -5, 35), ("left_arm", 4.5, -5, 35),
                            ("right_leg", -4.5, 6, -35), ("left_leg", 4.5, 6, -35)):
        sign = 1 if x > 0 else -1
        m.bone(f"{name}_upper", "body", [x, 15, z], [([x if sign > 0 else x - 18, 14.25, z - 0.75], [18, 1.5, 1.5], skin)],
               rotation=[0, sign * yaw, sign * 50])
        ex = x + sign * 18
        m.bone(f"{name}_lower", f"{name}_upper", [ex, 15, z],
               [([ex - 0.6, 15 - 26, z - 0.6], [1.2, 26, 1.2], skin), ([ex - 1.2, 15 - 27, z - 1.5], [2.4, 1, 3], skin)],
               rotation=[0, 0, sign * -50 + sign * -18])
    def limb(name, phase, length):
        return {f"{name}_upper": swing(length, 14, phase, axis=1), f"{name}_lower": swing(length, 10, phase + 1.2, axis=2)}
    crawl = {}
    for name, ph in (("right_arm", 0.0), ("left_leg", 0.4), ("left_arm", math.pi), ("right_leg", math.pi + 0.4)):
        crawl.update(limb(name, ph, 1.4))
    crawl["body"] = {"position": frames(1.4, 8, lambda t: (0, 0.8 * math.sin(2 * TAU * t), 0))}
    crawl["head"] = swing(1.4, 6, 0.0, axis=2)
    idle = {"head": swing(5.0, 10, 0.0, axis=2), "body": {"position": frames(5.0, 8, lambda t: (0, 0.5 * math.sin(TAU * t), 0))}}
    m.save({"idle": loop(5.0, idle), "walk": loop(1.4, crawl)})


# ---------------------------------------------------------------- 泥人: the mud lurker

def mud_lurker():
    """Drawn at half size and rendered twice as big: six blocks of mud bent nearly double. Waits as a mound in the
    flats ("buried"); the mound swells ("swell"), bursts, and it heaves itself up out of the mud ("rise")."""
    m = Model("mud_lurker", 256)
    mud = blotched((58, 46, 34), (36, 28, 22), 0.25)
    wet = blotched((46, 38, 30), (70, 58, 44), 0.3)
    face = sheet(([".........", ".........", "..e...e..", ".........", "..kkkkk..", "...kkk..."],
                  {"e": (226, 220, 150, True), "k": (18, 12, 10)}, mud))
    sh = humanoid(m, {"hip": 20, "torso": 24, "torso_w": 14, "torso_d": 9, "leg_w": 6, "arm": 36, "arm_w": 5,
                      "head": (9, 8, 9), "legs": mud, "torso_paint": mud, "head_paint": with_front(mud, face),
                      "arms": mud, "arm_pose": (-62, 0, 0), "hunch": (55, 0, 0), "neck": (-45, 0, 0),
                      "head_extra": [([-2, 20 + 24 - 3, -6], [1, 3, 1], wet), ([1.5, 20 + 24 - 4, -6], [1, 4, 1], wet)]})
    torso = next(b for b in m.bones if b["name"] == "torso")
    torso["cubes"] += [m.cube([-6, sh - 10, 3], [12, 10, 4], wet), m.cube([-4, sh - 4, 6], [8, 5, 3], mud)]
    for side in ("right", "left"):
        arm = next(b for b in m.bones if b["name"] == f"{side}_arm")
        x = arm["pivot"][0]
        arm["cubes"].append(m.cube([x - 3, sh - 1 - 36 - 2, -3], [6, 3, 6], wet))  # big clotted hands
    # the mound it waits in, not under root so it stays put while the body comes up through it
    m.bones.append({"name": "mound", "pivot": [0, 0, 0], "cubes": [
        m.cube([-9, -2, -10], [18, 5, 20], wet), m.cube([-7, 3, -8], [14, 3, 15], mud), m.cube([-4, 6, -5], [8, 2, 9], wet)]})
    under = [0, -70, 0]
    breathe = lambda length, a: {"scale": frames(length, 8, lambda t: (1 + a * math.sin(TAU * t),) * 3)}
    buried = loop(4.0, {"root": {"position": {"0": under}}, "mound": breathe(4.0, 0.03)})
    swell = {"loop": "hold_on_last_frame", "animation_length": 2.5, "bones": {
        "root": {"position": {"0": under}},
        "mound": {"scale": {f"{2.5 * i / 10:.2f}": [round(1 + 0.8 * (i / 10) ** 1.5 + 0.08 * math.sin(i * 2.1), 3)] * 3
                            for i in range(11)}}}}
    rise = {"loop": False, "animation_length": 1.8, "bones": {"root": {"position": {
        "0.000": under, "0.600": [0, -30, 0], "1.400": [0, -4, 0], "1.800": [0, 0, 0]}}}}
    m.save({"idle": idle_anim(), "walk": walk_anim(24, 10, length=2.0), "buried": buried, "swell": swell, "rise": rise})


# ---------------------------------------------------------------- 纸人: the paper effigy

def paper_effigy():
    m = Model("paper_effigy")
    paper = blotched((228, 224, 210), (200, 196, 182), 0.1, 4)
    stick = flat((120, 98, 64), 6)
    face = sheet(([
        "........",
        "........",
        ".kk..kk.",
        "........",
        "r......r",
        "r..rr..r",
        "...rr...",
        "........"], {"k": (12, 12, 12), "r": (200, 50, 50)}, paper))
    body = with_front(paper, lambda i, j, w, h, face: (([168, 40, 40], False) if 5 <= j <= 6 else paper(i, j, w, h, face)))
    humanoid(m, {"hip": 24, "torso": 16, "torso_w": 8, "torso_d": 2, "leg_w": 1.5, "arm": 18, "arm_w": 2,
                 "head": (8, 8, 2), "legs": stick, "torso_paint": body, "head_paint": with_front(paper, face),
                 "arms": paper})
    m.save({"idle": loop(2.0, {}), "walk": walk_anim(18, 4, length=0.8)})


# ---------------------------------------------------------------- 鱼人渔夫: the fishman

def fishman():
    m = Model("fishman")
    scales = striped((84, 104, 92), (66, 84, 74), 2)
    belly = flat((150, 160, 140))
    eye = glowing((220, 214, 150))
    rag = blotched((70, 64, 56), (50, 46, 40), 0.25)
    hip = 20
    sh = humanoid(m, {"hip": hip, "torso": 18, "torso_w": 9, "torso_d": 6, "leg_w": 3.5, "arm": 24, "arm_w": 3,
                      "head": (6, 6, 10), "legs": with_front(scales, rag), "torso_paint": with_front(scales, belly),
                      "head_paint": scales, "arms": scales, "arm_pose": (-58, 0, 0), "hunch": (38, 0, 0), "neck": (-30, 0, 0)})
    # the fish head: bulging eyes on either side, a gaping mouth, a fin down the back
    head = next(b for b in m.bones if b["name"] == "head")
    head["cubes"] += [m.cube([-4, sh + 3, -4], [1.2, 2, 2], eye), m.cube([2.8, sh + 3, -4], [1.2, 2, 2], eye),
                      m.cube([-2.5, sh, -6], [5, 2, 1], flat((40, 20, 24), 4)),
                      m.cube([-0.5, sh + 6, -3], [1, 3, 8], flat((60, 80, 70)))]
    torso = next(b for b in m.bones if b["name"] == "torso")
    torso["cubes"] += [m.cube([-0.5, sh - 14, 3], [1, 14, 3], flat((60, 80, 70))),  # a fin down the bent back
                       m.cube([-5, sh - 4, -3.5], [10, 4, 7], rag)]  # what is left of a collar
    # the harpoon in its right hand
    m.bone("harpoon", "right_arm", [-6, sh - 24, 0],
           [([-6.5, sh - 26, -16], [1, 1, 30], flat((96, 78, 56), 4)),
            ([-7, sh - 26.5, -18], [2, 2, 2], flat((140, 140, 136), 4))])
    m.save({"idle": idle_anim(sway=True), "walk": walk_anim(24, 8, length=1.4)})


# ---------------------------------------------------------------- 吊者: the hanged one

def hanged_one():
    m = Model("hanged_one")
    skin = blotched((150, 150, 142), (110, 108, 104), 0.15)
    shroud = blotched((196, 192, 180), (150, 146, 134), 0.12)
    rope = striped((120, 96, 60), (90, 72, 44), 1)
    face = sheet(([".......", ".......", ".k...k.", ".......", "..kkk..", "...k..."], {"k": (20, 20, 20)}, skin))
    sh = humanoid(m, {"hip": 26, "torso": 18, "torso_w": 6, "torso_d": 3, "leg_w": 2.5, "arm": 32, "arm_w": 2,
                      "head": (6, 6, 6), "legs": skin, "torso_paint": shroud, "head_paint": with_front(skin, face),
                      "arms": skin, "neck": (0, 0, 28)})  # the broken neck lolls to one side
    m.bone("noose", "torso", [0, sh, 0], [([-3.5, sh - 1, -2], [7, 1.5, 4], rope), ([-0.5, sh + 0.5, -0.5], [1, 12, 1], rope)])
    hang = loop(4.0, {"root": {"rotation": frames(4.0, 8, lambda t: (180, 6 * math.sin(TAU * t), 0)),
                               "position": {"0": [0, 31, 0]}},
                      "right_arm": swing(4.0, 3.0), "left_arm": swing(4.0, 3.0, 1.0)})
    m.save({"idle": idle_anim(), "walk": walk_anim(22, 4, length=2.0), "hang": hang})


# ---------------------------------------------------------------- 盐尸: the salt husk

def salt_husk():
    m = Model("salt_husk")
    dry = blotched((120, 108, 92), (226, 222, 210), 0.3)
    crust = blotched((230, 226, 214), (200, 196, 184), 0.2, 4)
    face = sheet(([".......", ".......", ".kk.kk.", ".......", "..www..", "......."],
                  {"k": (18, 16, 14), "w": (230, 226, 214)}, dry))
    sh = humanoid(m, {"hip": 20, "torso": 16, "torso_w": 7, "torso_d": 4, "leg_w": 3, "arm": 22, "arm_w": 2.5,
                      "head": (7, 7, 7), "legs": dry, "torso_paint": dry, "head_paint": with_front(dry, face),
                      "arms": dry, "arm_pose": (-25, 0, 0), "hunch": (32, 0, 0), "neck": (-22, 0, 0),
                      "head_extra": [([-3, 20 + 16 + 6, -2], [5, 3, 5], crust)]})
    torso = next(b for b in m.bones if b["name"] == "torso")
    torso["cubes"] += [m.cube([-5, sh - 3, -2], [3, 3, 4], crust), m.cube([2.5, sh - 2, -2.5], [3, 2, 4], crust),
                       m.cube([-3, sh - 9, 2], [6, 7, 3], crust)]
    m.save({"idle": idle_anim(sway=True), "walk": walk_anim(20, 5, length=2.0)})


# ---------------------------------------------------------------- 人牙螺: the tooth snail

def tooth_snail():
    m = Model("tooth_snail")
    flesh = blotched((150, 128, 120), (120, 100, 96), 0.15)
    shell = striped((104, 86, 66), (70, 58, 46), 2)
    teeth = sheet(([
        "wkwkwkwkw",
        "k.......k",
        "w.......w",
        "k.......k",
        "w.......w",
        "kwkwkwkwk"], {"w": (222, 214, 186), "k": (90, 30, 34)}, flat((40, 14, 18), 3)))
    m.bone("body", "root", [0, 0, 0], [([-3, 0, -8], [6, 4, 16], flesh)])
    m.bone("shell", "body", [0, 4, 2], [([-4.5, 4, -2], [9, 9, 9], with_front(shell, teeth)),
                                        ([-3, 13, 0], [6, 4, 6], shell), ([-1.5, 17, 1.5], [3, 2, 3], shell)])
    m.bone("head", "body", [0, 3, -8], [([-2, 1, -11], [4, 4, 3], flesh)])
    for side, x in (("right", -1.5), ("left", 0.5)):
        m.bone(f"{side}_stalk", "head", [x + 0.5, 5, -10], [([x, 5, -10.5], [1, 5, 1], flesh),
                                                            ([x - 0.5, 10, -11], [2, 2, 2], flat((20, 20, 18), 3))],
               rotation=[-10, 0, (-12 if side == "right" else 12)])
    slide = loop(2.0, {"body": {"scale": frames(2.0, 8, lambda t: (1, 1, 1 + 0.06 * math.sin(TAU * t)))},
                       "right_stalk": swing(2.0, 10, 0.0, axis=2),
                       "left_stalk": swing(2.0, 10, 1.4, axis=2)})
    m.save({"idle": loop(4.0, {"right_stalk": swing(4.0, 8, 0.0, axis=2),
                               "left_stalk": swing(4.0, 8, 2.0, axis=2)}), "walk": slide})


# ---------------------------------------------------------------- 独眼蟹: the eye crab

def eye_crab():
    m = Model("eye_crab")
    shell = blotched((112, 52, 40), (80, 36, 30), 0.15)
    leg = flat((96, 46, 36))

    def eye(i, j, w, h, face):
        if face != "north":
            return [226, 220, 204], False
        cx, cy = (w - 1) / 2, (h - 1) / 2
        d = math.hypot(i - cx, j - cy)
        return ([20, 18, 16] if d < 0.8 else [96, 70, 40] if d < 1.6 else [226, 220, 204]), False

    m.bone("body", "root", [0, 4, 0], [([-5, 3, -4], [10, 4, 8], shell)])
    m.bone("stalk", "body", [0, 7, -1], [([-0.5, 7, -1.5], [1, 6, 1], leg), ([-2, 13, -3], [4, 4, 4], eye)])
    for side, sign in (("right", -1), ("left", 1)):
        for k in range(3):
            z = -2.5 + k * 2.5
            m.bone(f"{side}_leg_{k}", "body", [sign * 5, 5, z],
                   [([sign * 5 if sign > 0 else -11, 4.5, z - 0.5], [6, 1, 1], leg)], rotation=[0, 0, sign * 35])
        m.bone(f"{side}_claw", "body", [sign * 4, 5, -4],
               [([sign * 4 - 1.5, 4, -9], [3, 2, 5], shell)], rotation=[0, sign * 20, 0])
    legs = {f"{s}_leg_{k}": swing(0.5, 18, k * 1.3 + (0 if s == "right" else math.pi), axis=1)
            for s in ("right", "left") for k in range(3)}
    m.save({"idle": loop(3.0, {"right_claw": swing(3.0, 8, 0.0, axis=1),
                               "left_claw": swing(3.0, 8, 1.0, axis=1),
                               "stalk": swing(3.0, 12, 0.0, (0, 0, 0), 2)}),
            "walk": loop(0.5, legs)})


# ---------------------------------------------------------------- 水蛭鳗: the leech eel

def leech_eel():
    m = Model("leech_eel")
    skin = striped((24, 34, 28), (36, 48, 40), 1)
    mouth = sheet(([".www.", "wkkkw", "wkkkw", "wkkkw", ".www."], {"w": (214, 206, 178), "k": (60, 10, 14)}, skin))
    m.bone("head", "root", [0, 1.5, -6], [([-2.5, 0, -12], [5, 5, 6], with_front(skin, mouth))])
    m.bone("mid", "root", [0, 1.5, 0], [([-2, 0, -6], [4, 4, 7], skin)])
    m.bone("tail", "mid", [0, 1.5, 1], [([-1.5, 0.5, 1], [3, 3, 6], skin), ([-0.5, 1, 7], [1, 2, 3], skin)])
    hop = {"loop": False, "animation_length": 0.6, "bones": {
        "head": {"rotation": {"0.0": [0, 0, 0], "0.2": [-35, 0, 0], "0.6": [0, 0, 0]}},
        "tail": {"rotation": {"0.0": [0, 0, 0], "0.2": [40, 0, 0], "0.6": [0, 0, 0]}}}}
    m.save({"idle": loop(2.0, {"tail": swing(2.0, 15, 0.0, (0, 0, 0), 1), "head": swing(2.0, 6, 1.0, (0, 0, 0), 1)}),
            "walk": loop(1.0, {"tail": swing(1.0, 25, 0.0, (0, 0, 0), 1)}), "hop": hop})


# ---------------------------------------------------------------- 人面鱼: the face fish

def face_fish():
    """A carp, fins and fan tail and all, with a man's face where its own should be."""
    m = Model("face_fish", 64)
    scales = striped((150, 160, 160), (120, 132, 134), 1)
    fin = flat((112, 122, 126), 6)
    face = sheet(([
        "......",
        ".kwwk.",
        "......",
        "..nn..",
        ".rrrr.",
        "......"], {"k": (20, 18, 16), "w": (210, 200, 180), "n": (170, 140, 126), "r": (130, 70, 70)},
        flat((196, 170, 150), 4)))
    m.bone("body", "root", [0, 3, 0], [([-2, 0.5, -3], [4, 6, 8], scales), ([-1.5, 1, 5], [3, 5, 3], scales),
                                       ([-1, 1.5, 8], [2, 4, 2], scales)])
    m.bone("head", "body", [0, 3, -3], [([-2, 0.5, -7], [4, 6, 4], with_front(scales, face))])
    m.bone("dorsal", "body", [0, 6.5, -1], [([-0.25, 6.5, -2], [0.5, 4, 7], fin)], rotation=[-15, 0, 0])
    m.bone("anal", "body", [0, 1, 6], [([-0.25, -1.5, 5], [0.5, 2.5, 3], fin)])
    for side, sign in (("right", -1), ("left", 1)):
        m.bone(f"{side}_pectoral", "body", [sign * 2, 1.5, -2],
               [([sign * 2 if sign > 0 else -6, 1.5, -2], [4, 0.5, 3], fin)], rotation=[0, sign * -30, sign * -25])
        m.bone(f"{side}_pelvic", "body", [sign * 1, 0.5, 2],
               [([sign * 1 - 0.25, -1.5, 2], [0.5, 2, 2.5], fin)], rotation=[0, 0, sign * 20])
    m.bone("tail", "body", [0, 3.5, 10], [([-0.5, 2.5, 10], [1, 2, 2], scales)])
    m.bone("tail_top", "tail", [0, 3.5, 11.5], [([-0.25, 3.5, 11.5], [0.5, 5, 5], fin)], rotation=[-35, 0, 0])
    m.bone("tail_bottom", "tail", [0, 3.5, 11.5], [([-0.25, -1.5, 11.5], [0.5, 5, 5], fin)], rotation=[35, 0, 0])
    def swim(length, a):
        return loop(length, {"tail": swing(length, a, 0.0, axis=1), "body": swing(length, a / 4, math.pi, axis=1),
                             "right_pectoral": swing(length, 15, 0.0, axis=2), "left_pectoral": swing(length, 15, 0.0, axis=2)})
    m.save({"idle": swim(1.6, 18), "walk": swim(0.6, 32)})


# ---------------------------------------------------------------- 无毛犬: the hairless dog

def hairless_dog():
    m = Model("hairless_dog")
    skin = blotched((206, 178, 172), (150, 116, 112), 0.15)
    ribs = with_front(skin, skin)

    def ribbed(i, j, w, h, face):
        c, g = skin(i, j, w, h, face)
        if face in ("east", "west") and i % 3 == 0 and 1 < j < h - 1:
            c = [v * 0.75 for v in c]
        return c, g

    face = sheet(([".....", ".k.k.", ".....", "..k..", "....."], {"k": (16, 12, 12)}, skin))
    m.bone("body", "root", [0, 9, 0], [([-3, 7, -6], [6, 6, 13], ribbed)])
    m.bone("head", "body", [0, 12, -6], [([-2.5, 10, -12], [5, 5, 6], with_front(skin, face)),
                                         ([-1.5, 10, -15], [3, 3, 3], skin),
                                         ([-2.5, 15, -8], [1, 2, 1], skin), ([1.5, 15, -8], [1, 2, 1], skin)])
    m.bone("tail", "body", [0, 12, 7], [([-0.5, 11.5, 7], [1, 1, 7], skin)], rotation=[30, 0, 0])
    for name, x, z in (("front_right", -2, -4.5), ("front_left", 1, -4.5), ("back_right", -2, 5), ("back_left", 1, 5)):
        m.bone(f"{name}_leg", "body", [x + 0.5, 7, z], [([x, 0, z - 0.5], [1.5, 8, 1.5], skin)])
    legs = {f"{n}_leg": swing(0.8, 30, ph) for n, ph in
            (("front_right", 0.0), ("back_left", 0.0), ("front_left", math.pi), ("back_right", math.pi))}
    m.save({"idle": loop(4.0, {"tail": swing(4.0, 6, 0.0, axis=1), "head": swing(4.0, 4, 0.0)}),
            "walk": loop(0.8, legs)})


# ---------------------------------------------------------------- item icons for the drops

def icon(name, rows, palette):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in palette:
                c = palette[ch]
                n = rng.randint(-8, 8)
                px[i, j] = tuple(max(0, min(255, int(v + n))) for v in c) + (255,)
    out = ROOT / "textures/item"
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / f"{name}.png")


def blob_icon(name, colour, accent, shape="round"):
    rows = []
    for y in range(16):
        row = ""
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            inside = (dx * dx + dy * dy < 30) if shape == "round" else (abs(dx) + abs(dy) < 7)
            edge = inside and (dx * dx + dy * dy > 20 if shape == "round" else abs(dx) + abs(dy) > 5)
            row += ("a" if (x * 7 + y * 3) % 11 == 0 else "e" if edge else "c") if inside else "."
        rows.append(row)
    icon(name, rows, {"c": colour, "e": [v * 0.7 for v in colour], "a": accent})


def items():
    blob_icon("fish_scale", (130, 160, 150), (210, 220, 210), "diamond")
    icon("murky_eye", [
        "................", "................", "................", ".....wwwwww.....", "...wwwwwwwwww...",
        "..wwwwbbbbwwww..", "..wwwbbkkbbwww..", ".wwwwbkkkkbwwww.", ".wwwwbkkkkbwwww.", "..wwwbbkkbbwww..",
        "..wwwwbbbbwwww..", "...wwrwwwwwww...", ".....wwwrww.....", "................", "................",
        "................"], {"w": (214, 210, 190), "b": (90, 110, 96), "k": (14, 14, 12), "r": (170, 70, 70)})
    blob_icon("coarse_salt", (230, 226, 214), (180, 176, 164))
    icon("paper_charm", [
        "................", "....yyyyyyyy....", "....yyyyyyyy....", "....yyrrrryy....", "....yyyryyyy....",
        "....yrrrrryy....", "....yyyryyyy....", "....yyrryryy....", "....yyyyryyy....", "....yrrrrryy....",
        "....yyyyyyyy....", "....yyrryryy....", "....yyyyyyyy....", "....yyyyyyyy....", "......y..y......",
        "................"], {"y": (222, 196, 110), "r": (170, 30, 30)})
    blob_icon("bog_heart", (70, 52, 38), (150, 60, 50))
    blob_icon("leech", (30, 40, 32), (70, 20, 24), "diamond")
    icon("human_tooth", [
        "................", "................", "................", ".....wwwwww.....", "....wwwwwwww....",
        "....wwwwwwww....", "....wwwwwwww....", "....wwwwwwww....", ".....wwwwww.....", ".....ww..ww.....",
        ".....ww..ww.....", ".....yw..wy.....", "......y..y......", "................", "................",
        "................"], {"w": (226, 218, 192), "y": (176, 160, 110)})
    icon("face_fish", [
        "................", "................", "................", "..ssssssssss....", ".sffffssssssss.t",
        ".sfkfkssssssssst", ".sffffssssssss.t", ".sfrrfssssssss.t", ".sffffssssssss.t", "..ssssssssss...t",
        "................", "................", "................", "................", "................",
        "................"], {"s": (150, 160, 160), "f": (200, 170, 150), "k": (20, 18, 16), "r": (140, 70, 70),
                             "t": (120, 132, 134)})
    blob_icon("pale_hide", (206, 178, 172), (150, 116, 112), "diamond")
    blob_icon("snail_flesh", (150, 128, 120), (200, 170, 160))
    icon("hanging_rope", [
        ".......rr.......", "......r..r......", "......r..r......", ".......rr.......", ".......r........",
        "........r.......", ".......r........", "........r.......", ".......r........", "........r.......",
        ".......r........", "........r.......", ".......r........", "........r.......", ".......rr.......",
        "......r..r......"], {"r": (120, 96, 60)})
    icon("harpoon", [
        "..............mm", ".............mmm", "............mmm.", "...........wm...", "..........w.....",
        ".........w......", "........w.......", ".......w........", "......w.........", ".....w..........",
        "....w...........", "...w............", "..w.............", ".w..............", "w...............",
        "................"], {"m": (150, 150, 146), "w": (110, 86, 58)})


if __name__ == "__main__":
    for build in (drowned_one, mud_lurker, paper_effigy, fishman, hanged_one, salt_husk, tooth_snail, eye_crab,
                  leech_eel, face_fish, hairless_dog):
        build()
    items()
    print("ok")
