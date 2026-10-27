package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.registry.ModEntities;
import com.horrortown.world.FogHarbor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.horrortown.registry.ModBlocks;
import net.minecraft.client.renderer.BiomeColors;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(FogHarbor.ID, new FogHarborEffects());
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.TOWNSFOLK.get(), TownsfolkRenderer::new);
    }

    /** The shallow-water film takes the colour of the water around it. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageWaterColor(level, pos) : 0x2B2E25, ModBlocks.TIDE_FILM.get());
    }
}
