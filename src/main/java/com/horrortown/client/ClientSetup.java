package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.FogHarbor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;

@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerDimensionEffects(RegisterDimensionSpecialEffectsEvent event) {
        event.register(FogHarbor.ID, new FogHarborEffects());
    }
}
