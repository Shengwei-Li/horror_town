package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.block.SaltLineBlock;
import com.horrortown.entity.ThrownTalisman;
import com.horrortown.entity.WaterSword;
import com.horrortown.item.TalismanItem;
import com.horrortown.item.TideGlassItem;
import com.horrortown.item.TideHarpoonItem;
import com.horrortown.item.TideOfferingItem;
import com.horrortown.item.TideStaffItem;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * What is made from the Tide Mother and from the things in the fog: her staff and veil, the offering that calls
 * her up, and the folk remedies and weapons of the town (coin sword, talismans, salt lines, lamps of skin).
 */
public final class ModGear {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, HorrorTownMod.MODID);

    // ------------------------------------------------------------------ the Tide Mother

    /** 潮母鳞 */
    public static final DeferredItem<Item> MOTHER_SCALE = ModItems.ITEMS.registerSimpleItem("mother_scale",
            new Item.Properties().rarity(Rarity.UNCOMMON));
    /** 潮母之心 */
    public static final DeferredItem<Item> TIDE_HEART = ModItems.ITEMS.registerSimpleItem("tide_heart",
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(1).fireResistant());
    /** 潮母祭品: offered at the idol by night, it calls her up out of the sea */
    public static final DeferredItem<TideOfferingItem> TIDE_OFFERING = ModItems.ITEMS.register("tide_offering",
            () -> new TideOfferingItem(new Item.Properties().rarity(Rarity.RARE).stacksTo(1)));
    /** 潮心鱼叉 */
    public static final DeferredItem<SwordItem> TIDE_HARPOON = ModItems.ITEMS.register("tide_harpoon",
            () -> new TideHarpoonItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant()
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.6F))));
    /** 潮母法杖 */
    public static final DeferredItem<TideStaffItem> TIDE_STAFF = ModItems.ITEMS.register("tide_staff",
            () -> new TideStaffItem(new Item.Properties().rarity(Rarity.EPIC).fireResistant().durability(400)));
    /** The blade the staff throws, only ever seen in flight (it is what the renderer draws). */
    public static final DeferredItem<Item> WATER_SWORD = ModItems.ITEMS.registerSimpleItem("water_sword");
    public static final DeferredHolder<EntityType<?>, EntityType<WaterSword>> WATER_SWORD_ENTITY =
            ModEntities.ENTITIES.register("water_sword", () -> EntityType.Builder.<WaterSword>of(WaterSword::new,
                    MobCategory.MISC).sized(0.5F, 0.5F).clientTrackingRange(8).updateInterval(1).noSave()
                    .build("water_sword"));

    /** 潮母面纱: the fog thins in front of you, and the Watcher's eye slides off you */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TIDE_VEIL_MATERIAL = ARMOR_MATERIALS.register(
            "tide_veil", () -> material(2, 15, SoundEvents.ARMOR_EQUIP_TURTLE, MOTHER_SCALE, "tide_veil", 1.0F));
    public static final DeferredItem<ArmorItem> TIDE_VEIL = ModItems.ITEMS.register("tide_veil",
            () -> new ArmorItem(TIDE_VEIL_MATERIAL, ArmorItem.Type.HELMET, new Item.Properties().rarity(Rarity.EPIC)
                    .durability(ArmorItem.Type.HELMET.getDurability(33))));

    // ------------------------------------------------------------------ the world

    /** 潮骨: the bones of something huge on the sea floor */
    public static final DeferredBlock<RotatedPillarBlock> TIDE_BONE = ModBlocks.BLOCKS.registerBlock("tide_bone",
            RotatedPillarBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SAND).requiresCorrectToolForDrops()
                    .strength(2.5F).sound(SoundType.BONE_BLOCK));
    public static final DeferredItem<BlockItem> TIDE_BONE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(TIDE_BONE);

    // ------------------------------------------------------------------ the town's remedies

    /** 泥靴: caked, patched waders the townsfolk keep for the mud flats; no protection to speak of */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MUD = ARMOR_MATERIALS.register("mud",
            () -> material(1, 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Items.LEATHER, "mud", 0.0F));
    public static final DeferredItem<ArmorItem> MUD_BOOTS = ModItems.ITEMS.register("mud_boots",
            () -> new ArmorItem(MUD, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(8))));
    /** 泥上行者: the enchantment that does for any boots what the mud boots do */
    public static final ResourceKey<Enchantment> MUD_WALKER = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "mud_walker"));

    /** 鱼鳞甲: scales stitched onto pale hide; worse than iron, but you swim like a fish in it */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> FISH_SCALE = ARMOR_MATERIALS.register("fish_scale",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.LEGGINGS, 3);
                map.put(ArmorItem.Type.CHESTPLATE, 4);
                map.put(ArmorItem.Type.HELMET, 2);
                map.put(ArmorItem.Type.BODY, 4);
            }), 12, SoundEvents.ARMOR_EQUIP_TURTLE, () -> Ingredient.of(ModDrops.FISH_SCALE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "fish_scale"))),
                    0.0F, 0.0F));
    public static final DeferredItem<ArmorItem> FISH_SCALE_HELMET = fishScale("fish_scale_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> FISH_SCALE_CHESTPLATE = fishScale("fish_scale_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> FISH_SCALE_LEGGINGS = fishScale("fish_scale_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> FISH_SCALE_BOOTS = fishScale("fish_scale_boots", ArmorItem.Type.BOOTS);

    /** 铜钱剑: old coins strung on a red cord, the Taoist's sword against the dead: it cuts what is 阴 twice as deep */
    public static final DeferredItem<SwordItem> COIN_SWORD = ModItems.ITEMS.register("coin_sword",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().durability(300)
                    .attributes(SwordItem.createAttributes(Tiers.IRON, 2, -2.2F))));
    /** What the coin sword cuts twice as deep: the restless dead and things of paper */
    public static final TagKey<EntityType<?>> YIN = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "yin"));

    /** 镇魂符 */
    public static final DeferredItem<TalismanItem> SOUL_TALISMAN = ModItems.ITEMS.register("soul_talisman",
            () -> new TalismanItem(new Item.Properties().stacksTo(16)));
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownTalisman>> TALISMAN_ENTITY =
            ModEntities.ENTITIES.register("soul_talisman", () -> EntityType.Builder.<ThrownTalisman>of(ThrownTalisman::new,
                    MobCategory.MISC).sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10).build("soul_talisman"));

    /** 盐线 */
    public static final DeferredBlock<SaltLineBlock> SALT_LINE = ModBlocks.BLOCKS.registerBlock("salt_line",
            SaltLineBlock::new, BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).instabreak().noOcclusion()
                    .sound(SoundType.SAND).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> SALT_LINE_ITEM = ModItems.ITEMS.registerSimpleBlockItem(SALT_LINE);

    /** 引魂灯: a lamp of stretched pale skin, set with teeth; carried at night, it keeps the dark off your mind */
    public static final DeferredBlock<LanternBlock> SOUL_LAMP = ModBlocks.BLOCKS.registerBlock("soul_lamp",
            LanternBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_LANTERN).lightLevel(state -> 12));
    public static final DeferredItem<BlockItem> SOUL_LAMP_ITEM = ModItems.ITEMS.registerSimpleBlockItem(SOUL_LAMP);

    /** 窥潮镜 */
    public static final DeferredItem<TideGlassItem> TIDE_GLASS = ModItems.ITEMS.register("tide_glass",
            () -> new TideGlassItem(new Item.Properties().stacksTo(1)));

    public static final List<DeferredItem<? extends Item>> ALL = List.of(MOTHER_SCALE, TIDE_HEART, TIDE_OFFERING,
            TIDE_HARPOON, TIDE_STAFF, TIDE_VEIL, TIDE_BONE_ITEM, MUD_BOOTS, FISH_SCALE_HELMET, FISH_SCALE_CHESTPLATE,
            FISH_SCALE_LEGGINGS, FISH_SCALE_BOOTS, COIN_SWORD, SOUL_TALISMAN, SALT_LINE_ITEM, SOUL_LAMP_ITEM, TIDE_GLASS);

    private static ArmorMaterial material(int defence, int enchantability, Holder<SoundEvent> sound,
                                          Supplier<? extends Item> repair, String layer, float toughness) {
        return new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
            for (ArmorItem.Type type : ArmorItem.Type.values()) {
                map.put(type, defence);
            }
        }), enchantability, sound, () -> Ingredient.of(repair.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, layer))),
                toughness, 0.0F);
    }

    private static DeferredItem<ArmorItem> fishScale(String name, ArmorItem.Type type) {
        return ModItems.ITEMS.register(name, () -> new ArmorItem(FISH_SCALE, type,
                new Item.Properties().durability(type.getDurability(12))));
    }

    /** Whether this one walks through rot mud as if it were firm ground: mud boots, or boots with Mud Walker. */
    public static boolean walksOnMud(LivingEntity entity) {
        ItemStack boots = entity.getItemBySlot(EquipmentSlot.FEET);
        if (boots.isEmpty()) {
            return false;
        }
        if (boots.is(MUD_BOOTS.get())) {
            return true;
        }
        return entity.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(MUD_WALKER)
                .map(walker -> EnchantmentHelper.getItemEnchantmentLevel(walker, boots) > 0).orElse(false);
    }

    /** Whether the player has the Tide Mother's veil on. */
    public static boolean veiled(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(TIDE_VEIL.get());
    }

    /** How many pieces of fish-scale armour this one has on. */
    public static int fishScalePieces(LivingEntity entity) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (entity.getItemBySlot(slot).getItem() instanceof ArmorItem armour && armour.getMaterial().is(FISH_SCALE)) {
                pieces++;
            }
        }
        return pieces;
    }

    /** Whether this one has a soul lamp in either hand. */
    public static boolean carriesLamp(LivingEntity entity) {
        return entity.getMainHandItem().is(SOUL_LAMP_ITEM.get()) || entity.getOffhandItem().is(SOUL_LAMP_ITEM.get());
    }

    /** Loads the class, so its items are registered with the rest. */
    public static void init() {
    }

    private ModGear() {
    }
}
