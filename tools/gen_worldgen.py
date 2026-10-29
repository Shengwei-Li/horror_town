"""Generates the Fog Harbor worldgen JSON (biomes, terrain, features, world preset, biome tags).

Usage: python tools/gen_worldgen.py <vanilla data dir>
where <vanilla data dir> is the `data/minecraft` folder dumped by the vanilla data generator
(java -DbundlerMainClass=net.minecraft.data.Main -jar server.jar --server --output gen).
"""
import copy
import json
import sys
from pathlib import Path

MOD = "horror_town"
ROOT = Path(__file__).resolve().parent.parent / "src" / "main" / "resources" / "data"
VANILLA = Path(sys.argv[1])

SEA_LEVEL = 66  # vanilla is 63: the extra water floods the low coast into marsh
ISLAND_RADIUS = 2000.0  # the main landmass; the coast wanders roughly 1600-2300 blocks out

# ---------------------------------------------------------------- our own features

def placed(feature, *placement):
    return {"feature": feature, "placement": list(placement)}


def count(n):
    return {"type": "minecraft:count", "count": n}


def uniform(lo, hi):
    return {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}


RARITY = lambda chance: {"type": "minecraft:rarity_filter", "chance": chance}
IN_SQUARE = {"type": "minecraft:in_square"}
SEA_FLOOR = {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}
SURFACE = {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}
IN_BIOME = {"type": "minecraft:biome"}

CONFIGURED = {
    "tide_film": {"type": f"{MOD}:tide_film", "config": {}},
    "drowned_cypress": {"type": f"{MOD}:drowned_cypress", "config": {}},
    "dead_tree": {"type": f"{MOD}:dead_tree", "config": {}},
    "tide_bones": {"type": f"{MOD}:tide_bones", "config": {}},
    "barnacle_trunks": {"type": f"{MOD}:barnacle_trunks", "config": {}},
    "barnacle_rocks": {"type": f"{MOD}:barnacle_rocks", "config": {}},
    "wrecked_boat": {"type": f"{MOD}:wrecked_boat", "config": {}},
    "sea_stela": {"type": f"{MOD}:sea_stela", "config": {}},
    "rotting_hay": {"type": f"{MOD}:rotting_hay", "config": {}},
    "cave_seep": {"type": f"{MOD}:cave_seep", "config": {}},
    # 死水塘: stagnant pools in the low ground inland, banked with mud
    "standing_pool": {"type": f"{MOD}:standing_pool", "config": {}},
}
PLACED = {
    "drowned_cypress_sparse": placed(f"{MOD}:drowned_cypress", RARITY(2), IN_SQUARE, SEA_FLOOR, IN_BIOME),
    "drowned_cypress_dense": placed(f"{MOD}:drowned_cypress", count(uniform(2, 4)), IN_SQUARE, SEA_FLOOR, IN_BIOME),
    "dead_tree_common": placed(f"{MOD}:dead_tree", count(uniform(0, 2)), IN_SQUARE, SEA_FLOOR, IN_BIOME),
    "dead_tree_rare": placed(f"{MOD}:dead_tree", RARITY(3), IN_SQUARE, SEA_FLOOR, IN_BIOME),
    # 潮骨: now and then the skeleton of something huge on the sea floor
    "tide_bones": placed(f"{MOD}:tide_bones", RARITY(10), IN_SQUARE, SEA_FLOOR, IN_BIOME),
    # inland: barnacles on the trunks and the bare rock, once per chunk from its corner, after the trees
    "barnacle_trunks": placed(f"{MOD}:barnacle_trunks"),
    "barnacle_rocks": placed(f"{MOD}:barnacle_rocks"),
    "wrecked_boat": placed(f"{MOD}:wrecked_boat", RARITY(14), IN_SQUARE, SURFACE, IN_BIOME),
    "sea_stela_common": placed(f"{MOD}:sea_stela", RARITY(10), IN_SQUARE, SURFACE, IN_BIOME),
    "sea_stela_rare": placed(f"{MOD}:sea_stela", RARITY(40), IN_SQUARE, SURFACE, IN_BIOME),
    "rotting_hay": placed(f"{MOD}:rotting_hay", RARITY(5), IN_SQUARE, SURFACE, IN_BIOME),
    "standing_pool": placed(f"{MOD}:standing_pool", RARITY(6), IN_SQUARE, SURFACE, IN_BIOME),
    # the sea in the caves: spots in cave air, anywhere from deep down to just under the ground
    "cave_seep": placed(f"{MOD}:cave_seep", count(40), IN_SQUARE,
                        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                                      "min_inclusive": {"above_bottom": 8},
                                                                      "max_inclusive": {"absolute": 110}}},
                        {"type": "minecraft:environment_scan", "direction_of_search": "up", "max_steps": 12,
                         "target_condition": {"type": "minecraft:solid"},
                         "allowed_search_condition": {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"}},
                        {"type": "minecraft:random_offset", "xz_spread": 0, "y_spread": -1}, IN_BIOME),
    # 泣岩: sea water seeping out of the crags, far above any sea
    "weeping_springs": placed("minecraft:spring_water", count(50), IN_SQUARE,
                              {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                                            "min_inclusive": {"absolute": 80},
                                                                            "max_inclusive": {"absolute": 240}}}, IN_BIOME),
    # once per chunk, from the chunk corner
    "tide_film": placed(f"{MOD}:tide_film"),
}

# One global order for the vegetation step; every biome takes a subsequence so there is never an order cycle.
VEGETATION_ORDER = [
    f"{MOD}:tide_bones",
    f"{MOD}:wrecked_boat",
    f"{MOD}:sea_stela_common",
    f"{MOD}:sea_stela_rare",
    f"{MOD}:rotting_hay",
    "minecraft:forest_rock",
    "minecraft:glow_lichen",
    "minecraft:patch_large_fern",
    f"{MOD}:drowned_cypress_dense",
    f"{MOD}:drowned_cypress_sparse",
    f"{MOD}:dead_tree_common",
    f"{MOD}:dead_tree_rare",
    "minecraft:trees_birch_and_oak",
    "minecraft:trees_taiga",
    "minecraft:trees_plains",
    "minecraft:flower_plains",
    "minecraft:flower_default",
    "minecraft:patch_berry_common",
    "minecraft:patch_tall_grass",
    "minecraft:patch_grass_taiga",
    "minecraft:patch_grass_normal",
    "minecraft:patch_grass_plain",
    "minecraft:patch_grass_forest",
    "minecraft:patch_dead_bush",
    "minecraft:patch_waterlily",
    "minecraft:brown_mushroom_swamp",
    "minecraft:red_mushroom_swamp",
    "minecraft:brown_mushroom_normal",
    "minecraft:red_mushroom_normal",
    "minecraft:patch_sugar_cane_swamp",
    "minecraft:patch_sugar_cane",
    "minecraft:patch_pumpkin",
    "minecraft:seagrass_swamp",
    "minecraft:seagrass_normal",
    "minecraft:seagrass_deep",
    "minecraft:seagrass_simple",
    "minecraft:kelp_cold",
    f"{MOD}:barnacle_trunks",
    f"{MOD}:barnacle_rocks",
]


def ordered(names):
    unknown = set(names) - set(VEGETATION_ORDER)
    assert not unknown, unknown
    return [n for n in VEGETATION_ORDER if n in names]


def features(vegetation, fossils=False, film=False, pools=False, seeps=False, springs=False):
    mangrove = json.loads((VANILLA / "worldgen/biome/mangrove_swamp.json").read_text())
    steps = copy.deepcopy(mangrove["features"])
    if pools:
        steps[1] = [f"{MOD}:standing_pool"] + steps[1]
    if seeps:
        steps[7] = [f"{MOD}:cave_seep"] + steps[7]
    if springs:
        steps[8] = [f"{MOD}:weeping_springs"] + steps[8]
    if film:
        steps[10] = [f"{MOD}:tide_film"] + steps[10]
    steps[3] = (["minecraft:fossil_upper", "minecraft:fossil_lower"] if fossils else []) + [
        "minecraft:monster_room", "minecraft:monster_room_deep"]
    steps[9] = ordered(vegetation)
    return steps


# ---------------------------------------------------------------- spawns

def spawn(mob, weight, lo, hi):
    return {"type": mob, "weight": weight, "minCount": lo, "maxCount": hi}


# vanilla monsters stay, but thinned out: the Fog Harbor creatures (added per biome) are the main threat
LAND_MONSTERS = [
    spawn("minecraft:spider", 40, 2, 4),
    spawn("minecraft:zombie", 30, 2, 4),
    spawn("minecraft:skeleton", 40, 2, 4),
    spawn("minecraft:creeper", 60, 2, 4),
    spawn("minecraft:enderman", 10, 1, 4),
    spawn("minecraft:witch", 5, 1, 1),
    spawn("minecraft:drowned", 10, 1, 2),
]
# inland, away from the sea, the ordinary monsters at their ordinary numbers; the island's own things are rarer
INLAND_MONSTERS = [
    spawn("minecraft:spider", 100, 4, 4),
    spawn("minecraft:zombie", 95, 4, 4),
    spawn("minecraft:zombie_villager", 10, 1, 1),
    spawn("minecraft:skeleton", 100, 4, 4),
    spawn("minecraft:creeper", 100, 4, 4),
    spawn("minecraft:slime", 100, 4, 4),
    spawn("minecraft:enderman", 10, 1, 4),
    spawn("minecraft:witch", 5, 1, 1),
]
FARM_ANIMALS = [
    spawn("minecraft:sheep", 12, 4, 4),
    spawn("minecraft:pig", 10, 4, 4),
    spawn("minecraft:chicken", 10, 4, 4),
    spawn("minecraft:cow", 8, 4, 4),
]


def spawners(creature=(), monster=(), water_creature=(), water_ambient=()):
    return {
        "ambient": [spawn("minecraft:bat", 10, 8, 8)],
        "axolotls": [],
        "creature": list(creature),
        "misc": [],
        "monster": list(monster),
        "underground_water_creature": [spawn("minecraft:glow_squid", 10, 4, 6)],
        "water_ambient": list(water_ambient),
        "water_creature": list(water_creature),
    }


# ---------------------------------------------------------------- biomes

# Colours: everything leans grey-green and dull, and the water is dark. Fog/sky colours are mostly replaced
# client side, but they still tint the map and anything that reads them.
MUSIC = {"sound": "minecraft:music.overworld.deep_dark", "min_delay": 12000, "max_delay": 24000,
         "replace_current_music": False}
MOOD = {"sound": "minecraft:ambient.cave", "tick_delay": 6000, "block_search_extent": 8, "offset": 2.0}


def biome(temperature, effects, steps, spawns, downfall=0.9):
    base = {"fog_color": 0x8A938C, "sky_color": 0x7D8580, "mood_sound": MOOD, "music": MUSIC}
    base.update(effects)
    return {
        "has_precipitation": True,
        "temperature": temperature,
        "downfall": downfall,
        "effects": base,
        "spawners": spawns,
        "spawn_costs": {},
        "carvers": {"air": ["minecraft:cave", "minecraft:cave_extra_underground", "minecraft:canyon"]},
        "features": steps,
    }


BIOMES = {
    # 雾沼: the drowned coast — rot mud, reeds, lilies, standing water, grey trunks and dead trees
    "fog_marsh": biome(0.6, {
        "water_color": 0x2E3328, "water_fog_color": 0x1A1E16,
        "grass_color": 0x6B6A50, "foliage_color": 0x5A5A44,
    }, features([
        "minecraft:glow_lichen", f"{MOD}:drowned_cypress_sparse", f"{MOD}:dead_tree_common",
        "minecraft:patch_grass_normal", "minecraft:patch_dead_bush",
        "minecraft:brown_mushroom_swamp", "minecraft:patch_pumpkin",
    ], fossils=True), spawners(
        creature=FARM_ANIMALS + [spawn("minecraft:frog", 10, 2, 5), spawn(f"{MOD}:tooth_snail", 6, 1, 2)],
        monster=LAND_MONSTERS + [spawn("minecraft:slime", 40, 2, 4), spawn(f"{MOD}:drowned_one", 60, 1, 3),
                                 spawn(f"{MOD}:mud_lurker", 6, 1, 1), spawn(f"{MOD}:paper_effigy", 20, 1, 1),
                                 spawn(f"{MOD}:leech_eel", 30, 2, 3), spawn(f"{MOD}:hairless_dog", 10, 1, 1)],
    )),
    # 泥滩: the drowned outer coast — flat black mud as far as the fog lets you see, pools of standing water every
    # few steps, nothing green, only snags and the bones of trees
    "mud_flats": biome(0.6, {
        "water_color": 0x2B2E25, "water_fog_color": 0x16180F,
        "grass_color": 0x6E6A52, "foliage_color": 0x5C5946,
    }, features([
        "minecraft:glow_lichen", f"{MOD}:dead_tree_rare", "minecraft:patch_dead_bush",
    ], fossils=True, film=True), spawners(
        creature=[spawn("minecraft:frog", 4, 1, 3), spawn(f"{MOD}:tooth_snail", 10, 1, 2),
                  spawn(f"{MOD}:eye_crab", 12, 3, 6)],
        monster=LAND_MONSTERS + [spawn(f"{MOD}:drowned_one", 80, 2, 3), spawn(f"{MOD}:mud_lurker", 10, 1, 1),
                                 spawn(f"{MOD}:leech_eel", 60, 2, 4), spawn(f"{MOD}:fishman", 30, 3, 4),
                                 spawn(f"{MOD}:hairless_dog", 8, 1, 1)],
    )),
    # 盐碱滩: dead white flats where the sea dried out — salt crust, brine pools, bones of trees
    "salt_flats": biome(0.9, {
        "water_color": 0x4A5A55, "water_fog_color": 0x2C3633,
        "grass_color": 0x7E7A62, "foliage_color": 0x6A6652,
    }, features([
        "minecraft:glow_lichen", f"{MOD}:dead_tree_rare", "minecraft:patch_dead_bush",
    ], fossils=True), spawners(
        creature=[spawn("minecraft:rabbit", 4, 2, 3), spawn(f"{MOD}:eye_crab", 6, 2, 4)],
        monster=LAND_MONSTERS + [spawn(f"{MOD}:salt_husk", 100, 2, 4), spawn(f"{MOD}:paper_effigy", 10, 1, 1),
                                 spawn(f"{MOD}:hairless_dog", 10, 1, 1)],
    ), downfall=0.2),
    # 吊杉林: tall grey trunks standing in black water, hardly a leaf on them
    "drowned_grove": biome(0.5, {
        "water_color": 0x262B1F, "water_fog_color": 0x12160F,
        "grass_color": 0x5E6248, "foliage_color": 0x4C5040,
        "particle": {"options": {"type": "minecraft:white_ash"}, "probability": 0.004},
    }, features([
        "minecraft:glow_lichen", "minecraft:patch_large_fern", f"{MOD}:drowned_cypress_dense",
        f"{MOD}:dead_tree_common", "minecraft:patch_grass_taiga", "minecraft:patch_dead_bush",
        "minecraft:patch_waterlily", "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal",
        "minecraft:seagrass_swamp",
    ]), spawners(
        creature=FARM_ANIMALS + [spawn("minecraft:fox", 8, 2, 4)],
        monster=LAND_MONSTERS + [spawn(f"{MOD}:hanged_one", 80, 1, 2), spawn(f"{MOD}:paper_effigy", 20, 1, 1),
                                 spawn(f"{MOD}:drowned_one", 30, 1, 2)],
    )),
    # ------------------------------------------------ the transition belt and the interior
    # 盐碱荒原: behind the marsh the land dries out: cracked mud and salt, dead reeds, boats left where the sea
    # went out from under them, and old steles put up to keep it out
    "brine_waste": biome(0.8, {
        "water_color": 0x46544C, "water_fog_color": 0x283230,
        "grass_color": 0x857B5C, "foliage_color": 0x766C50,
    }, features([
        f"{MOD}:wrecked_boat", f"{MOD}:sea_stela_common", "minecraft:glow_lichen", f"{MOD}:dead_tree_rare",
        "minecraft:patch_grass_plain", "minecraft:patch_dead_bush",
    ], fossils=True, seeps=True), spawners(
        creature=[spawn("minecraft:rabbit", 6, 2, 3), spawn(f"{MOD}:eye_crab", 4, 1, 3)],
        monster=LAND_MONSTERS + [spawn(f"{MOD}:salt_husk", 60, 2, 3), spawn(f"{MOD}:hairless_dog", 20, 1, 2),
                                 spawn(f"{MOD}:paper_effigy", 15, 1, 1)],
    ), downfall=0.3),
    # 藤壶林: oak and birch woods far inland, barnacles crusted round the feet of every trunk, standing water
    "barnacle_wood": biome(0.6, {
        "water_color": 0x2C3A30, "water_fog_color": 0x18201A,
        "grass_color": 0x66704C, "foliage_color": 0x56603F,
        "particle": {"options": {"type": "minecraft:white_ash"}, "probability": 0.002},
    }, features([
        "minecraft:forest_rock", "minecraft:glow_lichen", f"{MOD}:dead_tree_rare", "minecraft:trees_birch_and_oak",
        "minecraft:flower_default", "minecraft:patch_grass_forest", "minecraft:brown_mushroom_normal",
        "minecraft:red_mushroom_normal",
        f"{MOD}:barnacle_trunks",
    ], pools=True, seeps=True), spawners(
        creature=FARM_ANIMALS + [spawn("minecraft:fox", 6, 2, 3), spawn("minecraft:wolf", 4, 2, 4)],
        monster=INLAND_MONSTERS + [spawn(f"{MOD}:hanged_one", 20, 1, 1), spawn(f"{MOD}:hairless_dog", 10, 1, 2),
                                   spawn(f"{MOD}:paper_effigy", 5, 1, 1)],
    )),
    # 霉原: open farmland nobody works any more: wet grass, mould, stagnant pools, haystacks gone black
    "mildew_plains": biome(0.7, {
        "water_color": 0x36402E, "water_fog_color": 0x1E241A,
        "grass_color": 0x76784E, "foliage_color": 0x646640,
    }, features([
        f"{MOD}:sea_stela_rare", f"{MOD}:rotting_hay", "minecraft:glow_lichen", f"{MOD}:dead_tree_rare",
        "minecraft:trees_plains", "minecraft:flower_plains", "minecraft:patch_tall_grass",
        "minecraft:patch_grass_plain", "minecraft:brown_mushroom_normal", "minecraft:patch_pumpkin",
        "minecraft:patch_sugar_cane",
        f"{MOD}:barnacle_trunks",
    ], pools=True, seeps=True), spawners(
        creature=FARM_ANIMALS + [spawn("minecraft:horse", 2, 2, 4), spawn(f"{MOD}:tooth_snail", 6, 1, 2)],
        monster=INLAND_MONSTERS + [spawn(f"{MOD}:paper_effigy", 20, 1, 1), spawn(f"{MOD}:hairless_dog", 10, 1, 2)],
    )),
    # 湿杉林: dark spruce dripping with lichen, the ground soft with needles and moss; things hang from the branches
    "sodden_taiga": biome(0.3, {
        "water_color": 0x1F2A26, "water_fog_color": 0x101614,
        "grass_color": 0x58644A, "foliage_color": 0x48543E,
    }, features([
        "minecraft:forest_rock", "minecraft:glow_lichen", "minecraft:patch_large_fern", f"{MOD}:dead_tree_common",
        "minecraft:trees_taiga", "minecraft:patch_berry_common", "minecraft:patch_grass_taiga",
        "minecraft:brown_mushroom_normal", "minecraft:red_mushroom_normal", f"{MOD}:barnacle_trunks",
    ], pools=True, seeps=True), spawners(
        creature=FARM_ANIMALS + [spawn("minecraft:wolf", 8, 4, 4), spawn("minecraft:fox", 8, 2, 4)],
        monster=INLAND_MONSTERS + [spawn(f"{MOD}:hanged_one", 25, 1, 1), spawn(f"{MOD}:hairless_dog", 12, 1, 2)],
    )),
    # 泣岩山: the island's mountains, grey rock that weeps sea water from every crack, barnacles high on the crags
    "weeping_crags": biome(0.4, {
        "water_color": 0x2A3A3C, "water_fog_color": 0x141E20,
        "grass_color": 0x606A50, "foliage_color": 0x505A42,
    }, features([
        "minecraft:glow_lichen", f"{MOD}:dead_tree_rare", "minecraft:patch_grass_taiga", f"{MOD}:barnacle_rocks",
    ], seeps=True, springs=True), spawners(
        creature=[spawn("minecraft:goat", 5, 1, 3), spawn("minecraft:rabbit", 4, 2, 3)],
        monster=INLAND_MONSTERS + [spawn(f"{MOD}:salt_husk", 10, 1, 2), spawn(f"{MOD}:hanged_one", 8, 1, 1),
                                   spawn(f"{MOD}:hairless_dog", 8, 1, 2)],
    ), downfall=0.6),
    # 浊海: the grey sea
    "murky_sea": biome(0.5, {
        "water_color": 0x1F2A2B, "water_fog_color": 0x10181A,
        "grass_color": 0x5A6447, "foliage_color": 0x4A5238,
    }, features([
        f"{MOD}:tide_bones", "minecraft:glow_lichen", "minecraft:seagrass_normal", "minecraft:seagrass_simple",
        "minecraft:kelp_cold",
    ]), spawners(
        monster=LAND_MONSTERS + [spawn("minecraft:drowned", 70, 1, 2)],
        water_creature=[spawn("minecraft:squid", 3, 1, 4)],
        water_ambient=[spawn("minecraft:cod", 15, 3, 6), spawn(f"{MOD}:face_fish", 20, 2, 4)],
    ), downfall=0.5),
    # 渊沟: where the sea goes black. Something lives down here.
    "abyssal_trench": biome(0.5, {
        "water_color": 0x0E1517, "water_fog_color": 0x05090A,
        "grass_color": 0x5A6447, "foliage_color": 0x4A5238,
    }, features([
        f"{MOD}:tide_bones", "minecraft:glow_lichen", "minecraft:seagrass_deep", "minecraft:seagrass_simple",
        "minecraft:kelp_cold",
    ]), spawners(
        monster=LAND_MONSTERS + [spawn("minecraft:drowned", 100, 1, 3)],
        water_creature=[spawn("minecraft:squid", 1, 1, 2)],
        water_ambient=[spawn("minecraft:cod", 5, 1, 3), spawn(f"{MOD}:face_fish", 5, 1, 2)],
    ), downfall=0.5),
}

# ---------------------------------------------------------------- biome source

FULL = [-2.0, 2.0]


def point(name, humidity=FULL, continentalness=FULL, temperature=FULL, erosion=FULL):
    return {"biome": f"{MOD}:{name}", "parameters": {
        "temperature": temperature, "humidity": humidity, "continentalness": continentalness,
        "erosion": erosion, "weirdness": FULL, "depth": [0.0, 1.0], "offset": 0.0}}


OUTER_COAST = [-0.19, 0.0]
NEAR_INLAND = [0.0, 0.12]
TRANSITION = [0.12, 0.22]
INLAND = [0.22, 2.0]
MOUNTAINS = [-2.0, -0.5]
LOWLAND = [-0.5, 2.0]
BIOME_SOURCE = {"type": "minecraft:multi_noise", "biomes": [
    point("abyssal_trench", continentalness=[-2.0, -0.75]),
    point("murky_sea", continentalness=[-0.75, -0.19]),
    # the outer coast (and the islets out at sea): a wide ring of mud flats
    point("mud_flats", continentalness=OUTER_COAST),
    # behind them the marsh, with patches of dry salt flats
    point("salt_flats", humidity=[-2.0, -0.4], continentalness=NEAR_INLAND),
    point("fog_marsh", humidity=[-0.4, 2.0], continentalness=NEAR_INLAND),
    # further in, the transition belt: the drowned groves where it is still wet, the land drying out elsewhere
    point("salt_flats", humidity=[-2.0, -0.35], continentalness=TRANSITION),
    point("brine_waste", humidity=[-0.35, 0.1], continentalness=TRANSITION),
    point("drowned_grove", humidity=[0.1, 2.0], continentalness=TRANSITION),
    # the interior: crags where vanilla would raise mountains, and woods, taiga and old fields between them
    point("weeping_crags", continentalness=INLAND, erosion=MOUNTAINS),
    point("brine_waste", humidity=[-2.0, -0.45], continentalness=INLAND, erosion=LOWLAND),
    point("mildew_plains", humidity=[-0.45, 0.05], continentalness=INLAND, erosion=LOWLAND),
    point("barnacle_wood", humidity=[0.05, 2.0], temperature=[-0.15, 2.0], continentalness=INLAND, erosion=LOWLAND),
    point("sodden_taiga", humidity=[0.05, 2.0], temperature=[-2.0, -0.15], continentalness=INLAND, erosion=LOWLAND),
]}

# ---------------------------------------------------------------- surface rules


def block(name):
    return {"type": "minecraft:block", "result_state": {"Name": name}}


def cond(test, then):
    return {"type": "minecraft:condition", "if_true": test, "then_run": then}


def seq(*rules):
    return {"type": "minecraft:sequence", "sequence": list(rules)}


def biome_is(*names):
    return {"type": "minecraft:biome", "biome_is": [f"{MOD}:{n}" for n in names]}


def noise(name, lo, hi=1.7976931348623157e308):
    return {"type": "minecraft:noise_threshold", "noise": name, "min_threshold": lo, "max_threshold": hi}


def y_above(y):
    return {"type": "minecraft:y_above", "anchor": {"absolute": y}, "surface_depth_multiplier": 0,
            "add_stone_depth": False}


def not_(test):
    return {"type": "minecraft:not", "invert": test}


FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "surface_type": "floor", "add_surface_depth": False,
         "secondary_depth_range": 0}
UNDER_FLOOR = {"type": "minecraft:stone_depth", "offset": 0, "surface_type": "floor", "add_surface_depth": True,
               "secondary_depth_range": 0}
ABOVE_WATER = {"type": "minecraft:water", "offset": -1, "surface_depth_multiplier": 0, "add_stone_depth": False}
# true on the low band around sea level, where the swamp-style puddles go
AT_SEA_LEVEL = [y_above(SEA_LEVEL - 1), not_(y_above(SEA_LEVEL + 2))]
ROT_MUD = f"{MOD}:rot_mud"
SALT = f"{MOD}:salt_crust"


def all_of(tests, then):
    for test in reversed(tests):
        then = cond(test, then)
    return then


def puddles(threshold):
    return all_of(AT_SEA_LEVEL + [noise("minecraft:surface_swamp", threshold)], block("minecraft:water"))


SEA_BED = seq(
    cond(noise("minecraft:surface", 0.12), block("minecraft:gravel")),
    cond(noise("minecraft:surface", -0.15, 0.12), block("minecraft:mud")),
    block("minecraft:clay"),
)

MOD_SURFACE = seq(
    cond(biome_is("mud_flats"), seq(
        cond(FLOOR, seq(
            puddles(-0.45),
            cond(ABOVE_WATER, seq(
                cond(noise("minecraft:surface", 0.6), block("minecraft:coarse_dirt")),
                cond(noise("minecraft:surface", -0.1, 0.12), block("minecraft:mud")),
                block(ROT_MUD),
            )),
            block(ROT_MUD),
        )),
        cond(UNDER_FLOOR, block("minecraft:mud")),
    )),
    cond(biome_is("fog_marsh"), seq(
        cond(FLOOR, seq(
            puddles(-0.25),
            cond(ABOVE_WATER, seq(
                cond(noise("minecraft:surface", -0.2, 0.1), block(ROT_MUD)),
                cond(noise("minecraft:surface", 0.1, 0.3), block("minecraft:mud")),
                cond(noise("minecraft:surface", 0.3, 0.4), block("minecraft:coarse_dirt")),
                block("minecraft:grass_block"),
            )),
            block(ROT_MUD),
        )),
        cond(UNDER_FLOOR, block("minecraft:mud")),
    )),
    cond(biome_is("salt_flats"), seq(
        cond(FLOOR, seq(
            puddles(0.15),
            cond(ABOVE_WATER, seq(
                cond(noise("minecraft:surface", 0.25), block("minecraft:coarse_dirt")),
                cond(noise("minecraft:surface", -0.3, -0.18), block(ROT_MUD)),
                block(SALT),
            )),
            block(SALT),
        )),
        cond(UNDER_FLOOR, block("minecraft:sand")),
    )),
    cond(biome_is("drowned_grove"), seq(
        cond(FLOOR, seq(
            puddles(-0.3),
            cond(ABOVE_WATER, seq(
                cond(noise("minecraft:surface", 0.3), block(ROT_MUD)),
                cond(noise("minecraft:surface", -0.25, -0.05), block("minecraft:coarse_dirt")),
                block("minecraft:grass_block"),
            )),
            block(ROT_MUD),
        )),
        cond(UNDER_FLOOR, block("minecraft:mud")),
    )),
    cond(biome_is("brine_waste"), seq(
        cond(FLOOR, seq(
            cond(noise("minecraft:surface", 0.35), block("minecraft:coarse_dirt")),
            cond(noise("minecraft:surface", -0.05, 0.12), block(SALT)),
            cond(noise("minecraft:surface", -0.45, -0.3), block("minecraft:tuff")),
            block("minecraft:packed_mud"),
        )),
        cond(UNDER_FLOOR, block("minecraft:dirt")),
    )),
    cond(biome_is("barnacle_wood"), cond(FLOOR, seq(
        cond(noise("minecraft:surface", 0.3), block("minecraft:moss_block")),
        cond(noise("minecraft:surface", -0.3, -0.12), block("minecraft:podzol")),
        cond(noise("minecraft:surface", -0.5, -0.42), block("minecraft:mud")),
        block("minecraft:grass_block"),
    ))),
    cond(biome_is("mildew_plains"), cond(FLOOR, seq(
        cond(noise("minecraft:surface", 0.42), block("minecraft:moss_block")),
        cond(noise("minecraft:surface", -0.2, -0.1), block("minecraft:coarse_dirt")),
        cond(noise("minecraft:surface", -0.5, -0.4), block("minecraft:mud")),
        block("minecraft:grass_block"),
    ))),
    cond(biome_is("sodden_taiga"), cond(FLOOR, seq(
        cond(noise("minecraft:surface", 0.25), block("minecraft:moss_block")),
        cond(noise("minecraft:surface", -0.4, -0.2), block("minecraft:coarse_dirt")),
        block("minecraft:podzol"),
    ))),
    # the crags: bare weathered rock up high, wet moss and gravel lower down
    cond(biome_is("weeping_crags"), cond(FLOOR, seq(
        cond(y_above(120), seq(
            cond(noise("minecraft:surface", 0.2), block("minecraft:tuff")),
            cond(noise("minecraft:surface", -0.1, 0.05), block("minecraft:calcite")),
            block("minecraft:stone"),
        )),
        cond(noise("minecraft:surface", 0.3), block("minecraft:gravel")),
        cond(noise("minecraft:surface", -0.2, 0.0), block("minecraft:moss_block")),
        block("minecraft:grass_block"),
    ))),
    cond(biome_is("murky_sea", "abyssal_trench"), seq(
        cond(FLOOR, seq(
            cond(biome_is("abyssal_trench"), seq(
                cond(noise("minecraft:surface", 0.0), block("minecraft:tuff")),
                block(ROT_MUD),
            )),
            SEA_BED,
        )),
        cond(UNDER_FLOOR, block("minecraft:gravel")),
    )),
)

# ---------------------------------------------------------------- terrain shape

# Continentalness comes from IslandContinents (one island + islets, see the Java class) instead of plain noise.
# The vanilla splines that turn continentalness into height are copied with that swapped in, then reshaped:
#  - land: the vanilla height is pulled towards the level of the mud flats, hard on the coast (a block or two of
#    rise and fall) and less and less inland, until the mountains in the middle are left as they are;
#  - sea: vanilla's ocean floor is replaced by a long, gently sloping shelf of shallow water that only drops into
#    the deep well out from the coast.
# Surface height is about (1.5 + offset) * 128 - 64 blocks.
OURS = f"{MOD}:fog_harbor"
FLATS_OFFSET = -0.482  # the mud flats sit right at sea level (y 66)
COAST_C = -0.19
SWAPS = {
    '"minecraft:overworld/continents"': f'"{OURS}/continents"',
    '"minecraft:overworld/offset"': f'"{OURS}/vanilla_offset"',
    '"minecraft:overworld/factor"': f'"{OURS}/factor"',
    '"minecraft:overworld/jaggedness"': f'"{OURS}/jaggedness"',
    '"minecraft:overworld/depth"': f'"{OURS}/depth"',
    '"minecraft:overworld/sloped_cheese"': f'"{OURS}/sloped_cheese"',
}


def swapped(path):
    text = path.read_text()
    for old, new in SWAPS.items():
        text = text.replace(old, new)
    return json.loads(text)


def spline(points):
    """A spline over our continentalness: [(continentalness, value), ...]."""
    return {"type": "minecraft:spline", "spline": {"coordinate": f"{OURS}/continents", "points": [
        {"location": c, "value": v, "derivative": 0.0} for c, v in points]}}


# how much of vanilla's land relief survives: almost none on the coast, all of it in the far interior
LAND_RELIEF = spline([(COAST_C, 0.05), (0.0, 0.12), (0.12, 0.3), (0.3, 0.75), (0.5, 1.0)])
# steeper density falloff on the coast so 3D noise cannot raise lumps there
FLATNESS = spline([(COAST_C, 4.0), (0.05, 2.5), (0.2, 1.0)])
# the sea floor: ~2 blocks of water at the shore, a few blocks over the long shelf, then down into the deep
SEABED = spline([(-1.1, -0.92), (-0.8, -0.82), (-0.62, -0.66), (-0.5, -0.535), (-0.3, -0.515), (COAST_C, -0.5)])


def density_functions():
    df = VANILLA / "worldgen/density_function/overworld"
    continents = {"type": "minecraft:flat_cache", "argument": {
        "type": f"{MOD}:island_continents",
        "wobble": "minecraft:overworld/continents",
        "islets": {"type": "minecraft:noise", "noise": f"{MOD}:islets", "xz_scale": 1.0, "y_scale": 0.0},
        "radius": ISLAND_RADIUS,
    }}
    land = {"type": "minecraft:add", "argument1": FLATS_OFFSET, "argument2": {
        "type": "minecraft:mul", "argument1": LAND_RELIEF,
        "argument2": {"type": "minecraft:add", "argument1": -FLATS_OFFSET, "argument2": f"{OURS}/vanilla_offset"}}}
    offset = {"type": "minecraft:flat_cache", "argument": {
        "type": "minecraft:range_choice", "input": f"{OURS}/continents",
        "min_inclusive": COAST_C, "max_exclusive": 10.0,
        "when_in_range": land, "when_out_of_range": SEABED}}
    depth = swapped(df / "depth.json")
    depth["argument2"] = f"{OURS}/offset"
    return {
        "continents": continents,
        "vanilla_offset": swapped(df / "offset.json"),
        "offset": offset,
        "factor": {"type": "minecraft:mul", "argument1": FLATNESS, "argument2": swapped(df / "factor.json")},
        "jaggedness": {"type": "minecraft:mul", "argument1": LAND_RELIEF, "argument2": swapped(df / "jaggedness.json")},
        "depth": depth,
        "sloped_cheese": swapped(df / "sloped_cheese.json"),
    }


# patches a few hundred blocks across; their peaks out at sea become islets
ISLETS_NOISE = {"firstOctave": -9, "amplitudes": [1.0, 1.0, 1.0, 0.5]}


def noise_settings():
    settings = swapped(VANILLA / "worldgen/noise_settings/overworld.json")
    settings["sea_level"] = SEA_LEVEL
    settings["surface_rule"]["sequence"][1]["then_run"]["sequence"].insert(0, MOD_SURFACE)
    # spawn on the coast, where the town will be
    for target in settings["spawn_target"]:
        target["continentalness"] = [-0.19, -0.05]
    return settings


# ---------------------------------------------------------------- dimension + preset


def dimension_type():
    dim = json.loads((VANILLA / "dimension_type/overworld.json").read_text())
    dim["effects"] = f"{MOD}:fog_harbor"
    return dim


WORLD_PRESET = {"dimensions": {
    "minecraft:overworld": {
        "type": f"{MOD}:fog_harbor",
        "generator": {"type": "minecraft:noise", "biome_source": BIOME_SOURCE, "settings": f"{MOD}:fog_harbor"},
    },
    "minecraft:the_nether": {
        "type": "minecraft:the_nether",
        "generator": {"type": "minecraft:noise", "settings": "minecraft:nether",
                      "biome_source": {"type": "minecraft:multi_noise", "preset": "minecraft:nether"}},
    },
    "minecraft:the_end": {
        "type": "minecraft:the_end",
        "generator": {"type": "minecraft:noise", "settings": "minecraft:end",
                      "biome_source": {"type": "minecraft:the_end"}},
    },
}}

# ---------------------------------------------------------------- tags

LAND = ["mud_flats", "fog_marsh", "salt_flats", "drowned_grove", "brine_waste", "barnacle_wood", "mildew_plains",
        "sodden_taiga", "weeping_crags"]
SEA = ["murky_sea", "abyssal_trench"]
ALL = LAND + SEA

BIOME_TAGS = {
    "is_overworld": ALL,
    "stronghold_biased_to": LAND,
    "is_ocean": SEA,
    "is_deep_ocean": ["abyssal_trench"],
    "is_forest": ["drowned_grove", "barnacle_wood", "sodden_taiga"],
    "is_taiga": ["sodden_taiga"],
    "is_mountain": ["weeping_crags"],
    "is_hill": ["weeping_crags"],
    "allows_surface_slime_spawns": ["fog_marsh", "mud_flats"],
    "water_on_map_outlines": ["fog_marsh", "mud_flats"],
    "has_structure/mineshaft": ALL,
    "has_structure/ruined_portal_swamp": LAND,
    "has_structure/ruined_portal_ocean": SEA,
    "has_structure/shipwreck": SEA,
    "has_structure/shipwreck_beached": ["mud_flats", "salt_flats", "brine_waste"],
    "has_structure/trail_ruins": ["barnacle_wood", "sodden_taiga"],
    "has_structure/village_plains": ["mildew_plains"],
    "has_structure/village_taiga": ["sodden_taiga"],
    "has_structure/ruined_portal_standard": ["barnacle_wood", "mildew_plains", "sodden_taiga"],
    "has_structure/ruined_portal_mountain": ["weeping_crags"],
    "has_structure/trial_chambers": ["barnacle_wood", "mildew_plains", "sodden_taiga", "weeping_crags", "brine_waste"],
    "has_structure/ocean_ruin_cold": SEA,
    "has_structure/buried_treasure": ["mud_flats", "salt_flats"],
    "has_structure/swamp_hut": ["fog_marsh"],
}


# ---------------------------------------------------------------- the town

TOWN_STRUCTURE = {
    "type": f"{MOD}:fog_town",
    "biomes": f"#{MOD}:has_fog_town",
    "step": "surface_structures",
    "spawn_overrides": {},
    "terrain_adaptation": "none",
}
# every chunk is a candidate; the structure itself only starts in the chunk of the town site
TOWN_STRUCTURE_SET = {
    "structures": [{"structure": f"{MOD}:fog_town", "weight": 1}],
    "placement": {"type": "minecraft:random_spread", "spacing": 1, "separation": 0, "salt": 1931},
}


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def main():
    ours = ROOT / MOD
    for name, data in BIOMES.items():
        write(ours / "worldgen/biome" / f"{name}.json", data)
    for name, data in CONFIGURED.items():
        write(ours / "worldgen/configured_feature" / f"{name}.json", data)
    for name, data in PLACED.items():
        write(ours / "worldgen/placed_feature" / f"{name}.json", data)
    write(ours / "worldgen/noise/islets.json", ISLETS_NOISE)
    write(ours / "worldgen/noise_settings/fog_harbor.json", noise_settings())
    for name, data in density_functions().items():
        write(ours / "worldgen/density_function/fog_harbor" / f"{name}.json", data)
    write(ours / "worldgen/world_preset/fog_harbor.json", WORLD_PRESET)
    write(ours / "worldgen/structure/fog_town.json", TOWN_STRUCTURE)
    write(ours / "worldgen/structure_set/fog_town.json", TOWN_STRUCTURE_SET)
    write(ours / "tags/worldgen/biome/has_fog_town.json", {"replace": False, "values": [f"{MOD}:{n}" for n in ALL]})
    write(ours / "dimension_type/fog_harbor.json", dimension_type())
    write(ROOT / "minecraft/tags/worldgen/world_preset/normal.json",
          {"replace": False, "values": [f"{MOD}:fog_harbor"]})
    for tag, names in BIOME_TAGS.items():
        write(ROOT / "minecraft/tags/worldgen/biome" / f"{tag}.json",
              {"replace": False, "values": [f"{MOD}:{n}" for n in names]})


if __name__ == "__main__":
    main()
