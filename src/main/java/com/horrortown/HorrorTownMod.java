package com.horrortown;

import com.horrortown.entity.Townsfolk;
import com.horrortown.network.ModNetwork;
import com.horrortown.registry.ModAttachments;
import com.horrortown.registry.ModBlocks;
import com.horrortown.registry.ModDrops;
import com.horrortown.registry.ModEffects;
import com.horrortown.registry.ModEntities;
import com.horrortown.registry.ModItems;
import com.horrortown.registry.ModMobs;
import com.horrortown.registry.ModStructures;
import com.horrortown.registry.ModWorldgen;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;

@Mod(HorrorTownMod.MODID)
public class HorrorTownMod {
    public static final String MODID = "horror_town";

    public HorrorTownMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModWorldgen.FEATURES.register(modBus);
        ModWorldgen.DENSITY_FUNCTION_TYPES.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModMobs.ENTITIES.register(modBus);
        ModDrops.init();
        modBus.addListener(ModMobs::registerAttributes);
        modBus.addListener(ModMobs::registerSpawnPlacements);
        ModStructures.TYPES.register(modBus);
        ModStructures.PIECES.register(modBus);
        modBus.addListener(this::registerAttributes);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        modBus.addListener(ModNetwork::register);
        modBus.addListener(this::addCreativeTabItems);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.TOWNSFOLK.get(), Townsfolk.createAttributes().build());
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.ROT_MUD.get());
            event.accept(ModItems.SALT_CRUST.get());
            event.accept(ModItems.ROTWOOD_LOG.get());
        } else if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(ModItems.ROTTEN_PLANKS.get());
            event.accept(ModItems.SOAKED_PLANKS.get());
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.COPPER_COIN.get());
            ModDrops.ALL.forEach(item -> event.accept(item.get()));
        } else if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.TOWNSFOLK_SPAWN_EGG.get());
            ModMobs.SPAWN_EGGS.forEach(egg -> event.accept(egg.get()));
        }
    }
}
