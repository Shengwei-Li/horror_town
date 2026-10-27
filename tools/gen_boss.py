"""潮母真身, the Tide Mother's true body: the boss model, its drops' icons and the tide-scale armour textures.

Usage: python tools/gen_boss.py <vanilla texture dir with diamond_layer_1/2.png and diamond_*.png item icons>
The model is drawn at an eighth of its size and rendered eight times as big: forty blocks of her standing in the
shallows on a mass of tentacles. Her arm poses are solved against the model itself, and the slams' landing points
printed at the end go into TideMotherTrue.SLAMS. Front is -Z.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image

from gen_mobs import (ROOT, TAU, Model, blotched, flat, frames, glowing, icon, loop, rng, sheet, striped, swing,
                      with_front)

VTEX = Path(sys.argv[1])


SCALE = 8  # the model is drawn at an eighth of its size: one unit is half a block
WATER = 8  # where her hands come down: four blocks above the sea floor she stands on
REACHES = (16, 28, 40)  # how far in front of her (blocks) each of her three slams lands
SHOULDER_X = 15


def fingertip(m, deltas):
    """Where the right hand's middle finger ends (mirrored model units) with these extra arm rotations."""
    from render_models import apply, mat_mul, rotation_about, translate
    bones = {b["name"]: b for b in m.bones}
    cache = {}

    def world(name):
        if name not in cache:
            b = bones[name]
            mat = world(b["parent"]) if "parent" in b else translate(0, 0, 0)
            r = [a + d for a, d in zip(b.get("rotation", [0, 0, 0]), deltas.get(name, (0, 0, 0)))]
            cache[name] = mat_mul(mat, rotation_about(b["pivot"], r))
        return cache[name]
    return apply(world("right_hand"), (SHOULDER_X, -52, 0))


def solve(m, forward, up, upper=(-200, 80), lower=(-170, 170)):
    """Extra X rotations of the right upper arm and forearm that put its fingertip `forward` units in front of
    her (towards -Z) and `up` units high, in the plane of the shoulder."""
    best = None
    for coarse in (True, False):
        ur = range(upper[0], upper[1] + 1, 3) if coarse else range(best[1] - 3, best[1] + 4)
        lr = range(lower[0], lower[1] + 1, 3) if coarse else range(best[2] - 3, best[2] + 4)
        for a in ur:
            for b in lr:
                x, y, z = fingertip(m, {"right_arm": (a, 0, 0), "right_forearm": (b, 0, 0)})
                err = (-z - forward) ** 2 + (y - up) ** 2 + (0.02 * abs(b)) ** 2  # straighter arms win ties
                if best is None or err < best[0]:
                    best = (err, a, b)
    return best[1], best[2]


def tide_mother_true():
    """Forty blocks of her standing in the shallows: a mass of tentacles where legs should be, a rotting robe over
    a bloated grey body, a grinning skull of a face with two red eyes behind long wet hair, and arms nearly sixty
    blocks long that rest bent on the water in front of her."""
    m = Model("tide_mother_true", 512)
    skin = blotched((150, 158, 160), (118, 126, 130), 0.18)
    belly_skin = blotched((168, 172, 170), (132, 120, 124), 0.12)
    robe = blotched((52, 58, 52), (34, 40, 36), 0.25)
    hair = striped((16, 20, 18), (30, 36, 32), 1)
    barnacle = blotched((176, 170, 150), (120, 116, 100), 0.3)
    tentacle = blotched((92, 104, 104), (60, 70, 72), 0.25)
    sucker = blotched((150, 120, 120), (110, 86, 88), 0.3)
    bone = flat((214, 208, 190), 6)
    face = sheet(([
        "..............",
        "..............",
        ".kkkk....kkkk.",
        ".kkrrk..krrkk.",
        ".kkrrk..krrkk.",
        ".kkkk....kkkk.",
        "......kk......",
        "......kk......",
        "..............",
        "..............",
        "..............",
        "wkwkwkwkwkwkwk",
        "kwkwkwkwkwkwkw",
        ".kkkkkkkkkkkk."], {"k": (14, 12, 12), "r": (230, 30, 26, True), "w": (214, 208, 190)}, bone))
    jaw_face = sheet((["wkwkwkwkwkw", "kkkkkkkkkkk", "kkkkkkkkkkk"], {"w": (214, 208, 190), "k": (24, 10, 12)}, bone))

    # the tentacles she stands on, eight of them curling out across the sea floor
    m.bone("body", "root", [0, 20, 0], [([-12, 14, -12], [24, 10, 24], robe)])
    for k in range(8):
        a = k * 45 + 22.5
        m.bone(f"tentacle_{k}_0", "body", [0, 18, 0], [([-2.5, 4, -2.5], [5, 14, 5], with_front(tentacle, sucker))],
               rotation=[42, a, 0])
        m.bone(f"tentacle_{k}_1", f"tentacle_{k}_0", [0, 4, 0], [([-2, -8, -2], [4, 12, 4], with_front(tentacle, sucker))],
               rotation=[32, 0, 0])
        m.bone(f"tentacle_{k}_2", f"tentacle_{k}_1", [0, -8, 0], [([-1.5, -18, -1.5], [3, 10, 3], tentacle)],
               rotation=[30, 0, 0])
    m.bone("robe", "body", [0, 20, 0], [([-14, 22, -13], [28, 12, 26], robe), ([-15, 18, -6], [2, 8, 2], robe),
                                        ([10, 17, 9], [3, 9, 2], robe), ([-4, 16, -14], [4, 8, 2], robe)])
    m.bone("belly", "body", [0, 40, -2], [
        ([-13, 34, -14], [26, 16, 24], with_front(skin, belly_skin)), ([-11, 36, -16], [22, 12, 2], belly_skin),
        ([-14, 40, -6], [2, 4, 4], barnacle), ([12, 38, -2], [2, 5, 5], barnacle), ([-5, 35, -16.5], [4, 3, 1], barnacle)])
    m.bone("chest", "body", [0, 50, 0], [([-10, 50, -8], [20, 12, 14], skin), ([-15, 58, -7], [30, 5, 12], skin),
                                         ([5, 52, -8.5], [4, 3, 1], barnacle)], rotation=[10, 0, 0])
    m.bone("neck", "chest", [0, 63, -2], rotation=[-6, 0, 0])
    m.bone("head", "neck", [0, 63, -2], [
        ([-7, 63, -9], [14, 14, 14], with_front(bone, face)),
        ([-7.5, 76.5, -9.5], [15, 2, 15], hair),
        ([-8, 36, 4.5], [16, 41, 2], hair),
        ([-8.5, 40, -9], [1.5, 37, 13.5], hair), ([7, 40, -9], [1.5, 37, 13.5], hair),
        ([-7.5, 42, -9.6], [2.5, 35, 1], hair), ([5, 40, -9.6], [2.5, 37, 1], hair), ([-2, 46, -9.7], [1.5, 17, 1], hair)])
    m.bone("jaw", "head", [0, 63, -3], [([-5.5, 59.5, -8.5], [11, 3.5, 10], with_front(bone, jaw_face))])
    for side, sign in (("right", -1), ("left", 1)):
        x = sign * SHOULDER_X
        m.bone(f"{side}_arm", "chest", [x, 60, 0], [([x - 2.5, 10, -2.5], [5, 50, 5], skin),
                                                    ([x - 3, 54, -3], [6, 4, 6], barnacle if sign > 0 else skin)])
        m.bone(f"{side}_forearm", f"{side}_arm", [x, 10, 0], [([x - 2, -40, -2], [4, 50, 4], skin)])
        m.bone(f"{side}_hand", f"{side}_forearm", [x, -40, 0], [([x - 3, -46, -3], [6, 6, 6], skin)]
               + [([x - 0.5 + dx, -58, -0.5 + dz], [1, 12, 1], skin) for dx, dz in ((-2, -2), (0, -2.5), (2, -2), (0, 2))])

    # the rest pose, worked out so her hands lie on the water a little way in front of her
    rest_u, rest_l = solve(m, 46, WATER)
    for side in ("right", "left"):
        next(b for b in m.bones if b["name"] == f"{side}_arm")["rotation"] = [rest_u, 0, 0]
        next(b for b in m.bones if b["name"] == f"{side}_forearm")["rotation"] = [rest_l, 0, 0]
    # sanity: the solver is only as good as the geometry it was handed
    print("rest fingertip", [round(v / 2, 1) for v in fingertip(m, {})])

    def writhe(length, amplitude, speed=1):
        bones = {}
        for k in range(8):
            for s in range(3):
                bones[f"tentacle_{k}_{s}"] = swing(length, amplitude * (0.6 + 0.4 * s), k * 0.8 + s * 0.9, axis=0 if s else 2)
        return bones

    def arms(length, a, ph=0.0):
        return {"right_arm": swing(length, a, ph), "left_arm": swing(length, a, ph + math.pi),
                "right_forearm": swing(length, a, ph + 1.0), "left_forearm": swing(length, a, ph + 1.0 + math.pi)}

    breathe = {"belly": {"scale": frames(5.0, 8, lambda t: (1 + 0.03 * math.sin(TAU * t),) * 3)}}
    idle = {**writhe(5.0, 10), **breathe, "head": swing(5.0, 3, 0.0, axis=2), "jaw": swing(5.0, 4, 1.0), **arms(5.0, 3)}
    walk = {**writhe(3.0, 16), **breathe, "body": swing(3.0, 3, 0.0, axis=2), "head": swing(3.0, 4, 1.0, axis=2),
            **arms(3.0, 5)}
    lash = {"loop": False, "animation_length": 1.4, "bones": {
        f"tentacle_{k}_{s}": {"rotation": {"0.0": [0, 0, 0], "0.3": [-35 - 10 * s, 0, 0], "0.6": [40 + 10 * s, 0, 0],
                                           "0.9": [-20, 0, 0], "1.4": [0, 0, 0]}}
        for k in range(8) for s in range(3)}}
    roar = {"loop": False, "animation_length": 2.5, "bones": {
        "head": {"rotation": {"0.0": [0, 0, 0], "0.5": [-30, 0, 0], "2.0": [-26, 0, 0], "2.5": [0, 0, 0]}},
        "jaw": {"rotation": {"0.0": [0, 0, 0], "0.5": [45, 0, 0], "2.0": [45, 0, 0], "2.5": [0, 0, 0]}},
        "chest": {"rotation": {"0.0": [0, 0, 0], "0.5": [-10, 0, 0], "2.0": [-10, 0, 0], "2.5": [0, 0, 0]}}}}
    rise = {"loop": False, "animation_length": 6.0, "bones": {
        "root": {"position": {"0.0": [0, -90, 0], "5.0": [0, -6, 0], "6.0": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [35, 0, 0], "5.0": [10, 0, 0], "6.0": [0, 0, 0]}}}}
    death = {"loop": "hold_on_last_frame", "animation_length": 4.0, "bones": {
        "root": {"position": {"0.0": [0, 0, 0], "4.0": [0, -95, 0]}, "rotation": {"0.0": [0, 0, 0], "4.0": [0, 0, 10]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.8": [-30, 0, 0], "4.0": [-30, 0, 0]}},
        "jaw": {"rotation": {"0.0": [0, 0, 0], "0.8": [45, 0, 0], "4.0": [45, 0, 0]}}}}
    anims = {"idle": loop(5.0, idle), "walk": loop(3.0, walk), "lash": lash, "roar": roar, "rise": rise, "death": death}

    # the slams: the arm goes up and back, then comes down straight onto the water at one of three reaches
    landings = []
    raise_by = -150 if fingertip(m, {"right_arm": (-150, 0, 0)})[1] > fingertip(m, {"right_arm": (150, 0, 0)})[1] else 150
    for i, reach in enumerate(REACHES):
        du, dl = solve(m, reach * 2, WATER, (-220, 220), (-200, 200))
        tip = fingertip(m, {"right_arm": (du, 0, 0), "right_forearm": (dl, 0, 0)})
        landings.append((round(-tip[2] / 2, 1), round(tip[0] / 2, 1)))
        for side in ("right", "left"):
            anims[f"slam_{side}_{i}"] = {"loop": "hold_on_last_frame", "animation_length": 1.0, "bones": {
                f"{side}_arm": {"rotation": {"0.0": [0, 0, 0], "0.75": [raise_by, 0, 0], "1.0": [du, 0, 0]}},
                f"{side}_forearm": {"rotation": {"0.0": [0, 0, 0], "0.75": [20, 0, 0], "1.0": [dl, 0, 0]}},
                "chest": {"rotation": {"0.0": [0, 0, 0], "0.75": [-8, 0, 0], "1.0": [12, 0, 0]}}}}
    m.save(anims)
    print("slam landings (forward, right) in blocks:", landings)


# ---------------------------------------------------------------- tide-scale armour and the boss's drops

def tide_palette(img, base=(46, 74, 76), light=(150, 176, 168), scale_period=3):
    """Recolours a vanilla texture into overlapping grey-green scales, keeping its shading and its alpha."""
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            if (x + (y // scale_period) * 2) % (scale_period * 2) == 0:
                lum *= 0.75  # the edge of a scale
            c = [base[k] + (light[k] - base[k]) * lum + rng.randint(-5, 5) for k in range(3)]
            px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (a,)
    return img


def armour():
    out = ROOT / "textures/models/armor"
    out.mkdir(parents=True, exist_ok=True)
    for layer in (1, 2):
        tide_palette(Image.open(VTEX / f"diamond_layer_{layer}.png"), scale_period=2).save(out / f"tide_scale_layer_{layer}.png")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        tide_palette(Image.open(VTEX / f"diamond_{piece}.png")).save(ROOT / f"textures/item/tide_scale_{piece}.png")


def drops():
    icon("mother_scale", [
        "................", "......ssss......", "....ssllllss....", "...sllllllllss..", "..slllssssllls..",
        "..sllsddddslls..", ".sllsddddddslls.", ".slsddddddddsls.", ".slsddddddddsls.", ".sllsddddddslls.",
        "..sllsddddslls..", "..slllssssllls..", "...sllllllllss..", "....ssllllss....", "......ssss......",
        "................"], {"s": (40, 64, 66), "l": (140, 168, 160), "d": (88, 116, 112)})
    icon("tide_heart", [
        "................", "................", "...rrr....rrr...", "..rhhhr..rhhhr..", ".rhhhhhrrhhhhhr.",
        ".rhhghhhhhhhhhr.", ".rhhhhhhhhhhhhr.", ".rhhhhhkhhhhhhr.", "..rhhhhkhhhhhr..", "...rhhhkkhhhr...",
        "....rhhhkhhr....", ".....rhhhhr.....", "......rhhr......", ".......rr.......", "................",
        "................"], {"r": (40, 50, 60), "h": (96, 130, 140), "g": (200, 220, 210), "k": (30, 40, 46)})
    icon("tide_offering", [
        "................", "......pppp......", ".....pyyyyp.....", ".....pyrryp.....", ".....pyyyyp.....",
        "......pppp......", ".......bb.......", "......beeb......", ".....beekeb.....", ".....bekkeb.....",
        ".....beeeeb.....", "......beeb......", ".......bb.......", "......mmmm......", ".....mmmmmm.....",
        "................"], {"p": (180, 160, 96), "y": (222, 196, 110), "r": (170, 30, 30), "b": (70, 52, 38),
                              "e": (214, 210, 190), "k": (14, 14, 12), "m": (58, 46, 34)})
    tri = Image.open(VTEX / "trident.png")
    tide_palette(tri, base=(30, 56, 62), light=(170, 200, 196)).save(ROOT / "textures/item/tide_harpoon.png")


# ---------------------------------------------------------------- recipes, loot, item models, names

M = "horror_town"
DATA = ROOT.parent.parent / "data" / M


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + chr(10), encoding="utf-8")


def item(name):
    return {"item": name if ":" in name else f"{M}:{name}"}


def shaped(name, pattern, key, category="equipment"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                                         "key": {k: item(v) for k, v in key.items()}, "result": {"id": f"{M}:{name}", "count": 1}})


def shapeless(name, ingredients, category="misc"):
    write(DATA / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                         "ingredients": [item(i) for i in ingredients], "result": {"id": f"{M}:{name}", "count": 1}})


NAMES = {
    "item.horror_town.mother_scale": ("潮母鳞", "Tide Mother Scale"),
    "item.horror_town.tide_heart": ("潮母之心", "Heart of the Tide Mother"),
    "item.horror_town.tide_offering": ("潮母祭品", "Tide Offering"),
    "item.horror_town.tide_harpoon": ("潮心鱼叉", "Tide-Heart Harpoon"),
    "item.horror_town.tide_scale_helmet": ("潮鳞盔", "Tide-Scale Helmet"),
    "item.horror_town.tide_scale_chestplate": ("潮鳞甲", "Tide-Scale Chestplate"),
    "item.horror_town.tide_scale_leggings": ("潮鳞护腿", "Tide-Scale Leggings"),
    "item.horror_town.tide_scale_boots": ("潮鳞靴", "Tide-Scale Boots"),
    "entity.horror_town.tide_mother_true": ("潮母真身", "The Tide Mother"),
    "message.horror_town.offering.no_idol": ("须在庙里的潮母像前献上。", "It has to be offered before the idol in the temple."),
    "message.horror_town.offering.day": ("白天，她不应。", "By day, she does not answer."),
    "message.horror_town.offering.risen": ("她已经上来了。", "She is already here."),
    "message.horror_town.offering.answered": ("海里有东西应了。", "Something in the sea has answered."),
}


def data():
    for piece, pattern in (("helmet", ["SSS", "S S"]), ("chestplate", ["S S", "SSS", "SSS"]),
                           ("leggings", ["SSS", "S S", "S S"]), ("boots", ["S S", "S S"])):
        shaped(f"tide_scale_{piece}", pattern, {"S": "mother_scale"})
    shapeless("tide_offering", ["bog_heart", "murky_eye", "paper_charm", "coarse_salt", "human_tooth"])
    shapeless("tide_harpoon", ["harpoon", "tide_heart", "mother_scale", "mother_scale"], "equipment")
    write(DATA / "loot_table/entities/tide_mother_true.json", {"type": "minecraft:entity", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{M}:mother_scale", "functions": [
            {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 10, "max": 16}},
            {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
             "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"{M}:tide_heart"}]}]})
    for name in ("mother_scale", "tide_heart", "tide_offering", "tide_harpoon", "tide_scale_helmet",
                 "tide_scale_chestplate", "tide_scale_leggings", "tide_scale_boots"):
        parent = "minecraft:item/handheld" if name == "tide_harpoon" else "minecraft:item/generated"
        write(ROOT / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": f"{M}:item/{name}"}})
    for lang, idx in (("zh_cn", 0), ("en_us", 1)):
        path = ROOT / f"lang/{lang}.json"
        d = json.loads(path.read_text(encoding="utf-8"))
        d.update({k: v[idx] for k, v in NAMES.items()})
        write(path, d)


tide_mother_true()
armour()
drops()
data()
print("ok")
