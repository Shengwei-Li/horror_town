"""Generates the boilerplate for the Fog Harbor creatures: ModMobs.java (entity types, attributes, spawn rules,
renderers, spawn eggs), their drop items, loot tables, item models and names.

Usage: python tools/gen_mob_registry.py
Edit MOBS / ITEMS below and run again; the generated files are overwritten.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/horrortown"
RES = ROOT / "src/main/resources"
M = "horror_town"

# hand-written renderers (client/), for creatures that need more than the generic one
CUSTOM_RENDERERS = {"mud_lurker": "MudLurkerRenderer"}

# name, class, category, width, height, egg colours, spawn rule, glows, zh, en
MOBS = [
    ("drowned_one", "DrownedOne", "MONSTER", 1.4, 1.6, (0x6E8490, 0x1E2A26), "dark", False, "水鬼", "Drowned One"),
    ("mud_lurker", "MudLurker", "MONSTER", 2.0, 5.8, (0x3A2E22, 0xE2DC96), "any", True, "泥人", "Mud Lurker"),
    ("paper_effigy", "PaperEffigy", "MONSTER", 0.7, 1.95, (0xE4E0D2, 0xA82828), "dark", False, "纸人", "Paper Effigy"),
    ("fishman", "Fishman", "MONSTER", 0.8, 1.95, (0x54685C, 0xDCD696), "dark", True, "鱼人渔夫", "Fishman"),
    ("hanged_one", "HangedOne", "MONSTER", 0.7, 1.95, (0x96968E, 0x78603C), "dark", False, "吊者", "Hanged One"),
    ("salt_husk", "SaltHusk", "MONSTER", 0.8, 1.95, (0x786C5C, 0xE6E2D6), "dark", False, "盐尸", "Salt Husk"),
    ("tooth_snail", "ToothSnail", "CREATURE", 0.7, 1.2, (0x968078, 0xDED6BA), "mob", False, "人牙螺", "Tooth Snail"),
    ("eye_crab", "EyeCrab", "CREATURE", 0.7, 1.0, (0x703428, 0xE2DCCC), "mob", False, "独眼蟹", "Eye Crab"),
    ("leech_eel", "LeechEel", "MONSTER", 0.5, 0.4, (0x18221C, 0x3C0A0E), "any", False, "水蛭鳗", "Leech Eel"),
    ("face_fish", "FaceFish", "WATER_AMBIENT", 0.5, 0.5, (0x96A0A0, 0xC4AA96), "water", False, "人面鱼", "Face Fish"),
    ("hairless_dog", "HairlessDog", "MONSTER", 0.6, 0.9, (0xCEB2AC, 0x10100C), "any", False, "无毛犬", "Hairless Dog"),
]

# item, zh, en, kind (plain / food:<nutrition>:<saturation>:<effect>:<seconds>:<chance> / harpoon)
ITEMS = [
    ("fish_scale", "鳞片", "Fish Scale", "plain"),
    ("murky_eye", "浊眼", "Murky Eye", "plain"),
    ("coarse_salt", "粗盐", "Coarse Salt", "plain"),
    ("paper_charm", "符纸", "Paper Charm", "plain"),
    ("bog_heart", "泥心", "Bog Heart", "plain"),
    ("human_tooth", "人牙", "Human Tooth", "plain"),
    ("pale_hide", "白皮", "Pale Hide", "plain"),
    ("hanging_rope", "吊绳", "Hanging Rope", "plain"),
    ("leech", "水蛭", "Leech", "food:2:0.1:HUNGER:15:0.8"),
    ("snail_flesh", "螺肉", "Snail Flesh", "food:4:0.4:CONFUSION:10:0.5"),
    ("face_fish", "人面鱼", "Face Fish", "food:3:0.3:CONFUSION:15:1.0"),
    ("harpoon", "鱼叉", "Harpoon", "harpoon"),
]

# what each drops: (item, min, max, chance)
LOOT = {
    "drowned_one": [("minecraft:rotten_flesh", 0, 2, 1.0), (f"{M}:fish_scale", 0, 1, 1.0), (f"{M}:copper_coin", 1, 1, 0.3)],
    "mud_lurker": [("minecraft:mud", 1, 3, 1.0), (f"{M}:bog_heart", 1, 1, 0.5)],
    "paper_effigy": [("minecraft:paper", 1, 3, 1.0), (f"{M}:paper_charm", 1, 1, 1.0)],
    "fishman": [(f"{M}:fish_scale", 1, 3, 1.0), ("minecraft:cod", 0, 2, 1.0), (f"{M}:harpoon", 1, 1, 0.08)],
    "hanged_one": [(f"{M}:hanging_rope", 1, 1, 1.0), ("minecraft:bone", 0, 2, 1.0)],
    "salt_husk": [(f"{M}:coarse_salt", 1, 3, 1.0), ("minecraft:bone", 0, 1, 1.0)],
    "tooth_snail": [(f"{M}:snail_flesh", 1, 1, 1.0), (f"{M}:human_tooth", 0, 2, 1.0)],
    "eye_crab": [(f"{M}:murky_eye", 1, 1, 0.4), ("minecraft:bone_meal", 0, 1, 1.0)],
    "leech_eel": [(f"{M}:leech", 1, 1, 1.0)],
    "face_fish": [(f"{M}:face_fish", 1, 1, 1.0)],
    "hairless_dog": [(f"{M}:pale_hide", 1, 2, 1.0), ("minecraft:bone", 0, 1, 1.0)],
}

SPAWN_RULE = {
    "dark": ("ON_GROUND", "Monster::checkMonsterSpawnRules"),
    "any": ("ON_GROUND", "Monster::checkAnyLightMonsterSpawnRules"),
    "mob": ("ON_GROUND", "Mob::checkMobSpawnRules"),
    "water": ("IN_WATER", "WaterAnimal::checkSurfaceWaterAnimalSpawnRules"),
}


def java():
    up = lambda n: n.upper()
    lines = []
    lines.append("package com.horrortown.registry;\n")
    lines.append("import com.horrortown.HorrorTownMod;")
    lines.append("import com.horrortown.client.HorrorMobRenderer;")
    lines.append("import com.horrortown.entity.mob.*;")
    for imp in ["net.minecraft.core.registries.Registries", "net.minecraft.world.entity.EntityType",
                "net.minecraft.world.entity.Mob", "net.minecraft.world.entity.MobCategory",
                "net.minecraft.world.entity.SpawnPlacementTypes", "net.minecraft.world.entity.animal.AbstractFish",
                "net.minecraft.world.entity.animal.WaterAnimal", "net.minecraft.world.entity.monster.Monster",
                "net.minecraft.world.item.Item", "net.minecraft.world.level.levelgen.Heightmap",
                "net.neoforged.neoforge.client.event.EntityRenderersEvent",
                "net.neoforged.neoforge.common.DeferredSpawnEggItem",
                "net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent",
                "net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent",
                "net.neoforged.neoforge.registries.DeferredHolder", "net.neoforged.neoforge.registries.DeferredItem",
                "net.neoforged.neoforge.registries.DeferredRegister", "java.util.List"]:
        lines.append(f"import {imp};")
    lines.append("")
    lines.append("/** The Fog Harbor creatures. Generated by tools/gen_mob_registry.py — edit that, not this. */")
    lines.append("public final class ModMobs {")
    lines.append("    public static final DeferredRegister<EntityType<?>> ENTITIES =")
    lines.append("            DeferredRegister.create(Registries.ENTITY_TYPE, HorrorTownMod.MODID);\n")
    for name, cls, cat, w, h, egg, rule, glow, zh, en in MOBS:
        lines.append(f"    /** {zh} */")
        lines.append(f"    public static final DeferredHolder<EntityType<?>, EntityType<{cls}>> {up(name)} = ENTITIES.register(\"{name}\",")
        lines.append(f"            () -> EntityType.Builder.<{cls}>of({cls}::new, MobCategory.{cat}).sized({w}F, {h}F)")
        lines.append(f"                    .clientTrackingRange(8).build(\"{name}\"));")
    lines.append("")
    for name, cls, cat, w, h, egg, rule, glow, zh, en in MOBS:
        lines.append(f"    public static final DeferredItem<DeferredSpawnEggItem> {up(name)}_SPAWN_EGG = ModItems.ITEMS.register(\"{name}_spawn_egg\",")
        lines.append(f"            () -> new DeferredSpawnEggItem({up(name)}, 0x{egg[0]:06X}, 0x{egg[1]:06X}, new Item.Properties()));")
    lines.append("")
    lines.append(f"    public static final List<DeferredItem<DeferredSpawnEggItem>> SPAWN_EGGS = List.of(")
    lines.append(",\n".join(f"            {up(n[0])}_SPAWN_EGG" for n in MOBS) + ");\n")
    lines.append("    private ModMobs() {\n    }\n")
    lines.append("    public static void registerAttributes(EntityAttributeCreationEvent event) {")
    for name, cls, cat, *_ in MOBS:
        attrs = "AbstractFish.createAttributes()" if cls == "FaceFish" else f"{cls}.createAttributes()"
        lines.append(f"        event.put({up(name)}.get(), {attrs}.build());")
    lines.append("    }\n")
    lines.append("    public static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {")
    for name, cls, cat, w, h, egg, rule, *_ in MOBS:
        placement, predicate = SPAWN_RULE[rule]
        lines.append(f"        event.register({up(name)}.get(), SpawnPlacementTypes.{placement}, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,")
        lines.append(f"                {predicate}, RegisterSpawnPlacementsEvent.Operation.REPLACE);")
    lines.append("    }")
    lines.append("}")
    text = "\n".join(lines) + "\n"
    text = text.replace("import com.horrortown.client.HorrorMobRenderer;\n", "")
    text = text.replace("import net.neoforged.neoforge.client.event.EntityRenderersEvent;\n", "")
    (JAVA / "registry/ModMobs.java").write_text(text, encoding="utf-8")

    # the renderers live on the client side only
    client = ["package com.horrortown.client;\n", "import com.horrortown.registry.ModMobs;",
              "import net.neoforged.neoforge.client.event.EntityRenderersEvent;", "",
              "/** Renderers for the Fog Harbor creatures. Generated by tools/gen_mob_registry.py — edit that, not this. */",
              "public final class ModMobRenderers {", "    private ModMobRenderers() {\n    }\n",
              "    public static void register(EntityRenderersEvent.RegisterRenderers event) {"]
    for name, cls, cat, w, h, egg, rule, glow, *_ in MOBS:
        humanoid = "true" if cat == "MONSTER" and name not in ("leech_eel", "hairless_dog") else "false"
        client.append(f"        event.registerEntityRenderer(ModMobs.{up(name)}.get(),")
        if name in CUSTOM_RENDERERS:
            client.append(f"                {CUSTOM_RENDERERS[name]}::new);")
            continue
        client.append(f"                context -> new HorrorMobRenderer<>(context, \"{name}\", {str(glow).lower()}, {humanoid}, {w / 1.5:.2f}F));")
    client += ["    }", "}"]
    (JAVA / "client/ModMobRenderers.java").write_text("\n".join(client) + "\n", encoding="utf-8")


def items_java():
    lines = ["package com.horrortown.registry;\n", "import net.minecraft.world.effect.MobEffectInstance;",
             "import net.minecraft.world.effect.MobEffects;", "import net.minecraft.world.food.FoodProperties;",
             "import net.minecraft.world.item.Item;", "import net.minecraft.world.item.SwordItem;",
             "import net.minecraft.world.item.Tiers;", "import net.neoforged.neoforge.registries.DeferredItem;", "",
             "import java.util.List;", "",
             "/** What the Fog Harbor creatures drop. Generated by tools/gen_mob_registry.py — edit that, not this. */",
             "public final class ModDrops {"]
    for item, zh, en, kind in ITEMS:
        const = item.upper()
        lines.append(f"    /** {zh} */")
        if kind == "plain":
            lines.append(f"    public static final DeferredItem<Item> {const} = ModItems.ITEMS.registerSimpleItem(\"{item}\");")
        elif kind == "harpoon":
            lines.append(f"    public static final DeferredItem<SwordItem> {const} = ModItems.ITEMS.register(\"{item}\",")
            lines.append("            () -> new SwordItem(Tiers.IRON, new Item.Properties()")
            lines.append("                    .attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.6F))));")
        else:
            _, nutrition, saturation, effect, seconds, chance = kind.split(":")
            lines.append(f"    public static final DeferredItem<Item> {const} = ModItems.ITEMS.registerSimpleItem(\"{item}\",")
            lines.append(f"            new Item.Properties().food(new FoodProperties.Builder().nutrition({nutrition})")
            lines.append(f"                    .saturationModifier({saturation}F)")
            lines.append(f"                    .effect(() -> new MobEffectInstance(MobEffects.{effect}, {int(seconds) * 20}), {chance}F).build()));")
    lines.append("")
    lines.append("    public static final List<DeferredItem<? extends Item>> ALL = List.of(" +
                 ", ".join(i[0].upper() for i in ITEMS) + ");\n")
    lines.append("    private ModDrops() {\n    }\n")
    lines.append("    /** Loads the class (and so registers the items). */")
    lines.append("    public static void init() {\n    }")
    lines.append("}")
    (JAVA / "registry/ModDrops.java").write_text("\n".join(lines) + "\n", encoding="utf-8")


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def resources():
    for name, entries in LOOT.items():
        pools = []
        for item, lo, hi, chance in entries:
            entry = {"type": "minecraft:item", "name": item, "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
                {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                 "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}
            pool = {"rolls": 1, "bonus_rolls": 0, "entries": [entry]}
            if chance < 1.0:
                pool["conditions"] = [{"condition": "minecraft:random_chance_with_enchanted_bonus",
                                       "enchantment": "minecraft:looting", "unenchanted_chance": chance,
                                       "enchanted_chance": {"type": "minecraft:linear", "base": chance + 0.02,
                                                            "per_level_above_first": 0.02}}]
            pools.append(pool)
        write(RES / f"data/{M}/loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": pools})
    assets = RES / f"assets/{M}"
    for item, *_ in ITEMS:
        parent = "minecraft:item/handheld" if item == "harpoon" else "minecraft:item/generated"
        write(assets / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"{M}:item/{item}"}})
    for name, *_ in MOBS:
        write(assets / f"models/item/{name}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    for lang, idx in (("zh_cn", 0), ("en_us", 1)):
        path = assets / f"lang/{lang}.json"
        d = json.loads(path.read_text(encoding="utf-8"))
        for name, cls, cat, w, h, egg, rule, glow, zh, en in MOBS:
            d[f"entity.{M}.{name}"] = (zh, en)[idx]
            d[f"item.{M}.{name}_spawn_egg"] = (f"{zh}刷怪蛋", f"{en} Spawn Egg")[idx]
        for item, zh, en, kind in ITEMS:
            d[f"item.{M}.{item}"] = (zh, en)[idx]
        write(path, d)


java()
items_java()
resources()
print("ok")
