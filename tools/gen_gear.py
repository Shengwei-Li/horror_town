"""The gear made from the Tide Mother and from the creatures' drops: icons, block and armour textures, models,
blockstates, loot, recipes, tags and names.

Usage: python tools/gen_gear.py <vanilla texture dir with diamond_*.png, diamond_layer_1/2.png, soul_lantern.png>
Most icons are shrunk from the PixelLab references in references/gear (see tools/gear_refs.sh); the staff and the
talisman are drawn here by hand.
"""
import json
import sys
from pathlib import Path

from PIL import Image

from gen_mobs import ROOT, icon, rng
from ref_icon import icon_from, tile_from

VTEX = Path(sys.argv[1])
REFS = ROOT.parents[4] / "references/gear"
M = "horror_town"
DATA = ROOT.parent.parent / "data"
ITEM_TEX = ROOT / "textures/item"
BLOCK_TEX = ROOT / "textures/block"
ARMOUR_TEX = ROOT / "textures/models/armor"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + chr(10), encoding="utf-8")


def recolour(img, dark, light, scale_period=0):
    """A vanilla texture in other colours, keeping its shading and alpha; optionally drawn over with scales."""
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            if scale_period and (x + (y // scale_period) * 2) % (scale_period * 2) == 0:
                lum *= 0.75
            c = [dark[k] + (light[k] - dark[k]) * lum + rng.randint(-5, 5) for k in range(3)]
            px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (a,)
    return img


# ---------------------------------------------------------------- textures

def textures():
    for d in (ITEM_TEX, BLOCK_TEX, ARMOUR_TEX):
        d.mkdir(parents=True, exist_ok=True)
    for name, ref in (("water_sword", "water_sword"), ("tide_veil", "tide_veil"), ("coin_sword", "coin_sword"),
                      ("salt_line", "salt_pouch"), ("soul_lamp", "soul_lantern"), ("tide_glass", "tide_glass")):
        icon_from(REFS / f"ref_{ref}.png", ITEM_TEX / f"{name}.png")

    icon("tide_staff", [
        "...........t.t..",
        "..........tkkkt.",
        ".........tkrrkt.",
        "..........krrk..",
        ".........tkkkt..",
        "........bt.t....",
        ".......bb.......",
        "......bh........",
        ".....bb.........",
        "....hb..........",
        "...bb...........",
        "..bh............",
        ".bb.............",
        ".b..............",
        "w...............",
        "................"], {"b": (204, 198, 180), "h": (26, 30, 28), "k": (70, 86, 86), "t": (104, 120, 118),
                              "r": (225, 34, 26), "w": (90, 170, 184)})
    icon("soul_talisman", [
        "................",
        ".....oyyyyo.....",
        ".....yrrrry.....",
        ".....yyryyy.....",
        ".....yrrrry.....",
        ".....yyryyy.....",
        ".....yryryy.....",
        ".....yyyyyy.....",
        ".....yrrrry.....",
        ".....yryyry.....",
        ".....yrrrry.....",
        ".....yyryyy.....",
        ".....yyrryy.....",
        ".....yyyyyd.....",
        ".....oyyydd.....",
        "................"], {"y": (214, 180, 70), "o": (170, 136, 50), "r": (168, 30, 24), "d": (84, 58, 30)})

    # fish-scale armour: silver-green scales over the diamond pieces
    fish = dict(dark=(40, 58, 62), light=(176, 198, 192))
    for layer in (1, 2):
        recolour(Image.open(VTEX / f"diamond_layer_{layer}.png"), scale_period=2, **fish).save(
            ARMOUR_TEX / f"fish_scale_layer_{layer}.png")
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        recolour(Image.open(VTEX / f"diamond_{piece}.png"), scale_period=3, **fish).save(ITEM_TEX / f"fish_scale_{piece}.png")
    # her veil, worn: a hood of wet, dark weed-green cloth
    recolour(Image.open(VTEX / "diamond_layer_1.png"), dark=(14, 22, 18), light=(70, 92, 76)).save(
        ARMOUR_TEX / "tide_veil_layer_1.png")

    # the soul lamp: the lantern's frame turned to stretched pale skin, its flame left cold and blue
    lamp = Image.open(VTEX / "soul_lantern.png").convert("RGBA")
    px = lamp.load()
    for y in range(lamp.height):
        for x in range(lamp.width):
            r, g, b, a = px[x, y]
            if a and not (b > r + 25 and g > r):
                lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
                px[x, y] = tuple(int(lo + (hi - lo) * lum) for lo, hi in zip((86, 70, 62), (232, 216, 198))) + (a,)
    lamp.save(BLOCK_TEX / "soul_lamp.png")

    # a scatter of salt grains, thickest down the middle
    salt = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    spx = salt.load()
    for y in range(16):
        for x in range(16):
            if rng.random() < 0.55 - abs(y - 7.5) * 0.045:
                v = rng.randint(200, 245)
                spx[x, y] = (v, v, v - rng.randint(0, 12), 255)
    salt.save(BLOCK_TEX / "salt_line.png")

    # tide bone: the reference for the side; the end shows the porous marrow in rings
    tile_from(REFS / "ref_tide_bone.png", BLOCK_TEX / "tide_bone.png")
    side = Image.open(BLOCK_TEX / "tide_bone.png").convert("RGBA")
    top = side.copy()
    tpx = top.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            r, g, b, a = tpx[x, y]
            if d < 3:
                f = 0.45
            elif d < 4.5:
                f = 1.1
            elif d < 6.5:
                f = 0.85 if rng.random() < 0.4 else 1.0
            else:
                f = 0.75
            tpx[x, y] = (min(255, int(r * f)), min(255, int(g * f)), min(255, int(b * f)), 255)
    top.save(BLOCK_TEX / "tide_bone_top.png")


# ---------------------------------------------------------------- models and blockstates

def models():
    generated = ["water_sword", "tide_veil", "salt_line", "soul_lamp", "tide_glass", "soul_talisman",
                 "fish_scale_helmet", "fish_scale_chestplate", "fish_scale_leggings", "fish_scale_boots"]
    for name in generated:
        write(ROOT / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}:item/{name}"}})
    for name in ("tide_staff", "coin_sword"):
        write(ROOT / f"models/item/{name}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{M}:item/{name}"}})

    write(ROOT / "models/block/tide_bone.json", {"parent": "minecraft:block/cube_column", "textures": {
        "end": f"{M}:block/tide_bone_top", "side": f"{M}:block/tide_bone"}})
    write(ROOT / "models/block/tide_bone_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
                                                            "textures": {"end": f"{M}:block/tide_bone_top", "side": f"{M}:block/tide_bone"}})
    write(ROOT / "models/item/tide_bone.json", {"parent": f"{M}:block/tide_bone"})
    write(ROOT / "blockstates/tide_bone.json", {"variants": {
        "axis=y": {"model": f"{M}:block/tide_bone"},
        "axis=z": {"model": f"{M}:block/tide_bone_horizontal", "x": 90},
        "axis=x": {"model": f"{M}:block/tide_bone_horizontal", "x": 90, "y": 90}}})

    write(ROOT / "models/block/salt_line.json", {"render_type": "minecraft:cutout", "ambientocclusion": False,
                                                 "textures": {"particle": f"{M}:block/salt_line", "salt": f"{M}:block/salt_line"},
                                                 "elements": [{"from": [0, 0.25, 0], "to": [16, 0.25, 16], "shade": False,
                                                               "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#salt"},
                                                                         "down": {"uv": [0, 0, 16, 16], "texture": "#salt"}}}]})
    write(ROOT / "blockstates/salt_line.json", {"variants": {"": [{"model": f"{M}:block/salt_line", "y": y} for y in (0, 90, 180, 270)]}})

    for hanging in (False, True):
        name = "soul_lamp_hanging" if hanging else "soul_lamp"
        parent = "minecraft:block/template_hanging_lantern" if hanging else "minecraft:block/template_lantern"
        write(ROOT / f"models/block/{name}.json", {"parent": parent, "render_type": "minecraft:cutout",
                                                   "textures": {"lantern": f"{M}:block/soul_lamp"}})
    write(ROOT / "blockstates/soul_lamp.json", {"variants": {
        "hanging=false": {"model": f"{M}:block/soul_lamp"}, "hanging=true": {"model": f"{M}:block/soul_lamp_hanging"}}})


# ---------------------------------------------------------------- loot, tags, recipes

def item(name):
    return {"item": name if ":" in name else f"{M}:{name}"}


def shaped(name, pattern, key, count=1, category="equipment"):
    write(DATA / M / f"recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern,
                                             "key": {k: item(v) for k, v in key.items()},
                                             "result": {"id": f"{M}:{name}", "count": count}})


def shapeless(name, ingredients, count=1, category="misc", result=None):
    write(DATA / M / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                             "ingredients": [item(i) for i in ingredients],
                                             "result": {"id": f"{M}:{result or name}", "count": count}})


def drops_itself(name):
    write(DATA / M / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{M}:{name}"}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


SILK_TOUCH = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def data():
    for name in ("tide_bone", "salt_line", "soul_lamp"):
        drops_itself(name)
    # the salt crust on the flats breaks up into coarse salt (or comes away whole with silk touch)
    write(DATA / M / "loot_table/blocks/salt_crust.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": f"{M}:salt_crust", "conditions": [SILK_TOUCH]},
            {"type": "minecraft:item", "name": f"{M}:coarse_salt", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
                {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:uniform_bonus_count",
                 "parameters": {"bonusMultiplier": 1}},
                {"function": "minecraft:explosion_decay"}]}]}]}]})
    write(DATA / "minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"{M}:tide_bone", f"{M}:soul_lamp"]})
    write(DATA / M / "tags/entity_type/yin.json", {"replace": False, "values": [
        f"{M}:paper_effigy", f"{M}:hanged_one", f"{M}:drowned_one", f"{M}:salt_husk", "#minecraft:undead"]})

    shaped("tide_staff", ["SES", " B ", " B "], {"S": "mother_scale", "E": "tide_heart", "B": "tide_bone"})
    shaped("tide_veil", ["SSS", "S S"], {"S": "mother_scale"})
    shapeless("tide_offering_from_bone", ["tide_bone", "tide_bone", "tide_bone", "murky_eye", "paper_charm"],
              result="tide_offering")
    shaped("coin_sword", ["C", "C", "R"], {"C": "copper_coin", "R": "hanging_rope"})
    shapeless("soul_talisman", ["paper_charm", "murky_eye"], count=2, category="equipment")
    shapeless("salt_line", ["coarse_salt", "coarse_salt"], count=6)
    shaped("soul_lamp", [" T ", "HCH", " H "], {"T": "human_tooth", "H": "pale_hide", "C": "minecraft:candle"})
    shaped("tide_glass", ["E", "C", "C"], {"E": "murky_eye", "C": "minecraft:copper_ingot"})
    for piece, pattern in (("helmet", ["SHS", "S S"]), ("chestplate", ["S S", "SHS", "SSS"]),
                           ("leggings", ["SHS", "S S", "S S"]), ("boots", ["H H", "S S"])):
        shaped(f"fish_scale_{piece}", pattern, {"S": "fish_scale", "H": "pale_hide"})


NAMES = {
    "item.horror_town.tide_staff": ("潮母法杖", "Tide Mother's Staff"),
    "item.horror_town.water_sword": ("水剑", "Water Sword"),
    "entity.horror_town.water_sword": ("水剑", "Water Sword"),
    "item.horror_town.tide_veil": ("潮母面纱", "Tide Mother's Veil"),
    "block.horror_town.tide_bone": ("潮骨", "Tide Bone"),
    "item.horror_town.fish_scale_helmet": ("鱼鳞盔", "Fish-Scale Cap"),
    "item.horror_town.fish_scale_chestplate": ("鱼鳞甲", "Fish-Scale Vest"),
    "item.horror_town.fish_scale_leggings": ("鱼鳞护腿", "Fish-Scale Leggings"),
    "item.horror_town.fish_scale_boots": ("鱼鳞靴", "Fish-Scale Boots"),
    "item.horror_town.coin_sword": ("铜钱剑", "Coin Sword"),
    "item.horror_town.soul_talisman": ("镇魂符", "Binding Talisman"),
    "entity.horror_town.soul_talisman": ("镇魂符", "Binding Talisman"),
    "block.horror_town.salt_line": ("盐线", "Salt Line"),
    "block.horror_town.soul_lamp": ("引魂灯", "Soul Lamp"),
    "item.horror_town.tide_glass": ("窥潮镜", "Tide Glass"),
    "effect.horror_town.bound": ("镇住", "Bound"),
}
GONE = ["item.horror_town.tide_scale_helmet", "item.horror_town.tide_scale_chestplate",
        "item.horror_town.tide_scale_leggings", "item.horror_town.tide_scale_boots"]


def lang():
    for code, idx in (("zh_cn", 0), ("en_us", 1)):
        path = ROOT / f"lang/{code}.json"
        d = json.loads(path.read_text(encoding="utf-8"))
        for key in GONE:
            d.pop(key, None)
        d.update({k: v[idx] for k, v in NAMES.items()})
        write(path, d)


textures()
models()
data()
lang()
print("ok")
