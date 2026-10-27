package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.FogHarbor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * The Fog Harbor colour grade (shaders/post/fog_harbor.json): switched on while you are in a Fog Harbor world,
 * off again when you leave. It never replaces another post effect (e.g. from spectating a creeper).
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class ClientGrade {
    private static final ResourceLocation GRADE =
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "shaders/post/fog_harbor.json");

    private static PostChain active;

    private ClientGrade() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GameRenderer renderer = mc.gameRenderer;
        boolean wanted = mc.level != null && FogHarbor.is(mc.level);
        PostChain current = renderer.currentEffect();
        if (wanted && current == null) {
            renderer.loadEffect(GRADE);
            active = renderer.currentEffect();
        } else if (!wanted && current != null && current == active) {
            renderer.shutdownEffect();
            active = null;
        }
    }
}
