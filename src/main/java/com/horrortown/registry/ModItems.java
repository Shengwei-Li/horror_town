package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HorrorTownMod.MODID);

    public static final DeferredItem<BlockItem> ROT_MUD = ITEMS.registerSimpleBlockItem(ModBlocks.ROT_MUD);
    public static final DeferredItem<BlockItem> SALT_CRUST = ITEMS.registerSimpleBlockItem(ModBlocks.SALT_CRUST);
    public static final DeferredItem<BlockItem> ROTWOOD_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.ROTWOOD_LOG);

    public static final DeferredItem<BlockItem> ROTTEN_PLANKS = ITEMS.registerSimpleBlockItem(ModBlocks.ROTTEN_PLANKS);
    public static final DeferredItem<BlockItem> SOAKED_PLANKS = ITEMS.registerSimpleBlockItem(ModBlocks.SOAKED_PLANKS);

    /** 铜钱: the town's money */
    public static final DeferredItem<Item> COPPER_COIN = ITEMS.registerSimpleItem("copper_coin");
    public static final DeferredItem<DeferredSpawnEggItem> TOWNSFOLK_SPAWN_EGG = ITEMS.register("townsfolk_spawn_egg",
            () -> new DeferredSpawnEggItem(ModEntities.TOWNSFOLK, 0x4A5238, 0x9AA39A, new Item.Properties()));

    private ModItems() {
    }
}
