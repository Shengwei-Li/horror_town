"""泥靴 and 泥上行者: the mud boots found in the town's houses and the enchantment that does the same for any boots.

Usage: python tools/gen_mud_gear.py <vanilla texture dir with leather_boots.png and leather_layer_1.png>
Both let you walk through rot mud at a normal pace instead of wading.
"""
import json
import sys
from pathlib import Path

from PIL import Image

from gen_mobs import ROOT, rng

VTEX = Path(sys.argv[1])
M = "horror_town"
DATA = ROOT.parent.parent / "data"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + chr(10), encoding="utf-8")


def mud_palette(img, dark=(34, 28, 22), light=(118, 100, 74)):
    """Recolours a leather texture into caked, cracked mud, keeping its shading and its alpha."""
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            if rng.random() < 0.12:
                lum *= 0.6  # a crack, or a clot of darker mud
            c = [dark[k] + (light[k] - dark[k]) * lum + rng.randint(-6, 6) for k in range(3)]
            px[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (a,)
    return img


def textures():
    mud_palette(Image.open(VTEX / "leather_layer_1.png")).save(ROOT / "textures/models/armor/mud_layer_1.png")
    mud_palette(Image.open(VTEX / "leather_boots.png")).save(ROOT / "textures/item/mud_boots.png")


NAMES = {
    "item.horror_town.mud_boots": ("泥靴", "Mud Boots"),
    "enchantment.horror_town.mud_walker": ("泥上行者", "Mud Walker"),
}


def data():
    write(ROOT / "models/item/mud_boots.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}:item/mud_boots"}})
    write(DATA / M / "enchantment/mud_walker.json", {
        "description": {"translate": f"enchantment.{M}.mud_walker"},
        "supported_items": "#minecraft:enchantable/foot_armor",
        "weight": 2,
        "max_level": 1,
        "min_cost": {"base": 10, "per_level_above_first": 10},
        "max_cost": {"base": 40, "per_level_above_first": 10},
        "anvil_cost": 4,
        "slots": ["feet"]})
    # an ordinary enchantment: on the table, from librarians, in loot
    write(DATA / "minecraft/tags/enchantment/non_treasure.json", {"replace": False, "values": [f"{M}:mud_walker"]})
    # a pair of mud boots in about a third of the houses
    path = DATA / M / "loot_table/chests/town_house.json"
    table = json.loads(path.read_text(encoding="utf-8"))
    table["pools"] = [p for p in table["pools"] if not any(e.get("name") == f"{M}:mud_boots" for e in p["entries"])]
    table["pools"].append({"rolls": 1, "bonus_rolls": 0, "entries": [
        {"type": "minecraft:item", "name": f"{M}:mud_boots", "weight": 35,
         "functions": [{"function": "minecraft:set_damage", "damage": {"type": "minecraft:uniform", "min": 0.5, "max": 1.0}}]},
        {"type": "minecraft:empty", "weight": 65}]})
    write(path, table)
    for lang, idx in (("zh_cn", 0), ("en_us", 1)):
        path = ROOT / f"lang/{lang}.json"
        d = json.loads(path.read_text(encoding="utf-8"))
        d.update({k: v[idx] for k, v in NAMES.items()})
        write(path, d)


textures()
data()
print("ok")
