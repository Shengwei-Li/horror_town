"""潮母真身, the Tide Mother's true body: the boss model, its drops' icons and the tide-scale armour textures.

Usage: python tools/gen_boss.py <vanilla texture dir with diamond_layer_1/2.png and diamond_*.png item icons>
The model is drawn at a third of its size and rendered three times as big: about ten blocks of bloated, robed,
grey flesh with a grinning skull of a face behind long wet hair, arms that hang to the ground. Front is -Z.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image

from gen_mobs import (ROOT, TAU, Model, blotched, flat, frames, glowing, icon, loop, rng, sheet, striped, swing,
                      with_front)

VTEX = Path(sys.argv[1])


def tide_mother_true():
    m = Model("tide_mother_true", 256)
    skin = blotched((150, 158, 160), (118, 126, 130), 0.18)
    belly_skin = blotched((168, 172, 170), (132, 120, 124), 0.12)
    robe = blotched((52, 58, 52), (34, 40, 36), 0.25)
    hair = striped((16, 20, 18), (30, 36, 32), 1)
    barnacle = blotched((176, 170, 150), (120, 116, 100), 0.3)
    bone = flat((214, 208, 190), 6)
    face = sheet(([
        "..........",
        "..........",
        ".kkk..kkk.",
        ".kgk..kgk.",
        ".kkk..kkk.",
        "....kk....",
        "..........",
        "..........",
        "wkwkwkwkwk",
        "kwkwkwkwkw"], {"k": (14, 12, 12), "g": (210, 220, 196, True), "w": (214, 208, 190)}, bone))
    jaw_face = sheet((["wkwkwkwk", "kkkkkkkk"], {"w": (214, 208, 190), "k": (24, 10, 12)}, bone))

    m.bone("body", "root", [0, 16, 0], [
        ([-11, 0, -9], [22, 10, 18], robe), ([-10, 10, -9.5], [20, 8, 18], robe),
        ([-12, 0, -10], [2, 4, 1], robe), ([3, 0, -10], [3, 3, 1], robe), ([9, 0, 8.5], [3, 5, 1], robe)])
    m.bone("belly", "body", [0, 24, -2], [
        ([-10, 17, -12], [20, 15, 19], with_front(skin, belly_skin)), ([-8, 19, -14], [16, 11, 2], belly_skin),
        ([-11, 22, -6], [2, 3, 3], barnacle), ([9, 20, -3], [2, 4, 4], barnacle), ([-4, 18, -14.5], [3, 2, 1], barnacle)])
    m.bone("chest", "body", [0, 32, 0], [
        ([-8, 32, -6], [16, 10, 10], skin), ([-10, 38, -5], [20, 4, 9], skin), ([4, 33, -6.5], [3, 2, 1], barnacle)],
        rotation=[12, 0, 0])
    m.bone("neck", "chest", [0, 42, -1], rotation=[-8, 0, 0])
    m.bone("head", "neck", [0, 42, -1], [
        ([-5, 44, -7], [10, 10, 10], with_front(bone, face)),
        ([-5.5, 53.5, -7.5], [11, 2, 11], hair),            # crown of hair
        ([-6, 24, 2.5], [12, 30, 2], hair),                 # down the back
        ([-6.5, 26, -7], [1.5, 28, 9], hair), ([5, 26, -7], [1.5, 28, 9], hair),  # either side
        ([-5.5, 30, -7.6], [2, 24, 1], hair), ([3.5, 28, -7.6], [2, 26, 1], hair),  # strands over the face
        ([-1.5, 34, -7.7], [1, 15, 1], hair)])
    m.bone("jaw", "head", [0, 44, -2], [([-4, 40.5, -6.5], [8, 3.5, 7.5], with_front(bone, jaw_face))])
    for side, sign in (("right", -1), ("left", 1)):
        x = sign * 13
        m.bone(f"{side}_arm", "chest", [x, 40, 0], [([x - 2, 21, -2], [4, 19, 4], skin),
                                                    ([x - 2.5, 36, -2.5], [5, 4, 5], barnacle if sign > 0 else skin)],
               rotation=[-14, 0, sign * 22])
        m.bone(f"{side}_forearm", f"{side}_arm", [x, 22, 0], [([x - 1.5, 5, -1.5], [3, 17, 3], skin),
                                                              ([x - 2.5, 1, -2.5], [5, 4, 5], skin)],
               rotation=[-10, 0, sign * -14])
        for k, dz in enumerate((-2, 0, 2)):
            m.bone(f"{side}_finger_{k}", f"{side}_forearm", [x, 1.5, dz], [([x - 0.5, -6, dz - 0.5], [1, 7.5, 1], skin)],
                   rotation=[(k - 1) * -12, 0, 0])

    def arms(length, a, ph=0.0):
        return {"right_arm": swing(length, a, ph), "left_arm": swing(length, a, ph + math.pi)}

    idle = {"belly": {"scale": frames(4.0, 8, lambda t: (1 + 0.03 * math.sin(TAU * t),) * 3)},
            "head": swing(4.0, 3, 0.0, axis=2), "jaw": swing(4.0, 4, 1.0), **arms(4.0, 3)}
    walk = {"body": {"rotation": frames(2.4, 8, lambda t: (0, 0, 3 * math.sin(TAU * t))),
                     "position": frames(2.4, 8, lambda t: (0, 0.6 * abs(math.sin(TAU * t)), 0))},
            "head": swing(2.4, 4, 1.0, axis=2), **arms(2.4, 14)}
    slam = {"loop": False, "animation_length": 1.2, "bones": {
        "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.5": [-150, 0, 10], "0.7": [-20, 0, 0], "1.2": [0, 0, 0]}},
        "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.5": [-150, 0, -10], "0.7": [-20, 0, 0], "1.2": [0, 0, 0]}},
        "chest": {"rotation": {"0.0": [0, 0, 0], "0.5": [-12, 0, 0], "0.7": [18, 0, 0], "1.2": [0, 0, 0]}}}}
    roar = {"loop": False, "animation_length": 2.0, "bones": {
        "head": {"rotation": {"0.0": [0, 0, 0], "0.4": [-28, 0, 0], "1.6": [-24, 0, 0], "2.0": [0, 0, 0]}},
        "jaw": {"rotation": {"0.0": [0, 0, 0], "0.4": [40, 0, 0], "1.6": [40, 0, 0], "2.0": [0, 0, 0]}},
        "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.5": [-40, 0, 50], "1.6": [-40, 0, 50], "2.0": [0, 0, 0]}},
        "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.5": [-40, 0, -50], "1.6": [-40, 0, -50], "2.0": [0, 0, 0]}}}}
    rise = {"loop": False, "animation_length": 4.0, "bones": {
        "root": {"position": {"0.0": [0, -60, 0], "3.0": [0, -6, 0], "4.0": [0, 0, 0]}},
        "head": {"rotation": {"0.0": [30, 0, 0], "3.0": [10, 0, 0], "4.0": [0, 0, 0]}}}}
    death = {"loop": "hold_on_last_frame", "animation_length": 3.0, "bones": {
        "root": {"position": {"0.0": [0, 0, 0], "3.0": [0, -62, 0]}, "rotation": {"0.0": [0, 0, 0], "3.0": [0, 0, 14]}},
        "head": {"rotation": {"0.0": [0, 0, 0], "0.6": [-30, 0, 0], "3.0": [-30, 0, 0]}},
        "jaw": {"rotation": {"0.0": [0, 0, 0], "0.6": [40, 0, 0], "3.0": [40, 0, 0]}}}}
    m.save({"idle": loop(4.0, idle), "walk": loop(2.4, walk), "slam": slam, "roar": roar, "rise": rise, "death": death})


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
