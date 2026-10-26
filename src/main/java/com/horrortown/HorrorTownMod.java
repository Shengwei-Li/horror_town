package com.horrortown;

import com.horrortown.network.FogPayload;
import com.horrortown.registry.ModBlocks;
import com.horrortown.registry.ModItems;
import com.horrortown.registry.ModWorldgen;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(HorrorTownMod.MODID)
public class HorrorTownMod {
    public static final String MODID = "horror_town";

    public HorrorTownMod(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModWorldgen.FEATURES.register(modBus);
        ModWorldgen.DENSITY_FUNCTION_TYPES.register(modBus);
        modBus.addListener(FogPayload::register);
        modBus.addListener(this::addCreativeTabItems);
    }

    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(ModItems.ROT_MUD.get());
            event.accept(ModItems.SALT_CRUST.get());
            event.accept(ModItems.ROTWOOD_LOG.get());
        }
    }
}
