package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.registry.ModEntities;
import com.horrortown.registry.ModGear;
import com.horrortown.world.FogHarbor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.horrortown.registry.ModBlocks;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
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
        event.registerEntityRenderer(ModEntities.TIDE_MOTHER.get(), TideMotherRenderer::new);
        event.registerEntityRenderer(ModEntities.WATCHER.get(), WatcherRenderer::new);
        event.registerEntityRenderer(ModEntities.TIDE_MOTHER_TRUE.get(), TideMotherTrueRenderer::new);
        event.registerEntityRenderer(ModGear.WATER_SWORD_ENTITY.get(), WaterSwordRenderer::new);
        event.registerEntityRenderer(ModGear.TALISMAN_ENTITY.get(), ThrownItemRenderer::new);
        ModMobRenderers.register(event);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        ClientSanity.registerLayer(event);
    }

    /** The shallow-water film takes the colour of the water around it. */
    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> level != null && pos != null
                ? BiomeColors.getAverageWaterColor(level, pos) : 0x2B2E25, ModBlocks.TIDE_FILM.get());
    }
}
