package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.item.TideHarpoonItem;
import com.horrortown.item.TideOfferingItem;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** What the Tide Mother's true body leaves behind, the offering that calls her up, and what is made of her. */
public final class ModGear {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, HorrorTownMod.MODID);

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

    /** 潮鳞甲: as good as diamond, and it keeps your head when the sea is looking at you */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TIDE_SCALE = ARMOR_MATERIALS.register("tide_scale",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 8);
            }), 15, SoundEvents.ARMOR_EQUIP_TURTLE, () -> Ingredient.of(MOTHER_SCALE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "tide_scale"))),
                    2.0F, 0.05F));
    public static final DeferredItem<ArmorItem> TIDE_SCALE_HELMET = armour("tide_scale_helmet", ArmorItem.Type.HELMET);
    public static final DeferredItem<ArmorItem> TIDE_SCALE_CHESTPLATE = armour("tide_scale_chestplate", ArmorItem.Type.CHESTPLATE);
    public static final DeferredItem<ArmorItem> TIDE_SCALE_LEGGINGS = armour("tide_scale_leggings", ArmorItem.Type.LEGGINGS);
    public static final DeferredItem<ArmorItem> TIDE_SCALE_BOOTS = armour("tide_scale_boots", ArmorItem.Type.BOOTS);

    /** 泥靴: caked, patched waders the townsfolk keep for the mud flats; no protection to speak of */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> MUD = ARMOR_MATERIALS.register("mud",
            () -> new ArmorMaterial(Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 1);
                map.put(ArmorItem.Type.LEGGINGS, 1);
                map.put(ArmorItem.Type.CHESTPLATE, 1);
                map.put(ArmorItem.Type.HELMET, 1);
                map.put(ArmorItem.Type.BODY, 1);
            }), 12, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(Items.LEATHER),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "mud"))),
                    0.0F, 0.0F));
    public static final DeferredItem<ArmorItem> MUD_BOOTS = ModItems.ITEMS.register("mud_boots",
            () -> new ArmorItem(MUD, ArmorItem.Type.BOOTS, new Item.Properties().durability(ArmorItem.Type.BOOTS.getDurability(8))));
    /** 泥上行者: the enchantment that does for any boots what the mud boots do */
    public static final ResourceKey<Enchantment> MUD_WALKER = ResourceKey.create(Registries.ENCHANTMENT,
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "mud_walker"));

    public static final List<DeferredItem<? extends Item>> ALL = List.of(MOTHER_SCALE, TIDE_HEART, TIDE_OFFERING,
            TIDE_HARPOON, TIDE_SCALE_HELMET, TIDE_SCALE_CHESTPLATE, TIDE_SCALE_LEGGINGS, TIDE_SCALE_BOOTS, MUD_BOOTS);

    private static DeferredItem<ArmorItem> armour(String name, ArmorItem.Type type) {
        return ModItems.ITEMS.register(name, () -> new ArmorItem(TIDE_SCALE, type,
                new Item.Properties().durability(type.getDurability(33))));
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

    /** How many pieces of tide-scale armour the player has on. */
    public static int tideScalePieces(Player player) {
        int pieces = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS,
                EquipmentSlot.FEET}) {
            if (player.getItemBySlot(slot).getItem() instanceof ArmorItem armour && armour.getMaterial().is(TIDE_SCALE)) {
                pieces++;
            }
        }
        return pieces;
    }

    /** Loads the class, so its items are registered with the rest. */
    public static void init() {
    }

    private ModGear() {
    }
}
