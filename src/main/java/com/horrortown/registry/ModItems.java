package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(HorrorTownMod.MODID);

    public static final DeferredItem<BlockItem> ROT_MUD = ITEMS.registerSimpleBlockItem(ModBlocks.ROT_MUD);
    public static final DeferredItem<BlockItem> SALT_CRUST = ITEMS.registerSimpleBlockItem(ModBlocks.SALT_CRUST);
    public static final DeferredItem<BlockItem> ROTWOOD_LOG = ITEMS.registerSimpleBlockItem(ModBlocks.ROTWOOD_LOG);

    private ModItems() {
    }
}
