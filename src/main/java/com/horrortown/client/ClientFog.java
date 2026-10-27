package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.FogHarbor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

/**
 * Fog in a Fog Harbor world. There is always a haze that eats the far landscape; on top of that the server
 * rolls fog banks in and out ({@link #setTarget}), and some biomes are mistier than others.
 * The fog colour is also the colour of the whole sky (see {@link FogHarborEffects}).
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class ClientFog {
    /** Furthest you can ever see, in clear weather (blocks). */
    private static final float HAZE_DISTANCE = 128.0F;
    /** How close the fog comes in the thickest bank (blocks). */
    private static final float BANK_DISTANCE = 12.0F;
    private static final float RISE = 0.0015F;
    private static final float LIFT = 0.001F;
    /** Grey-green overcast, and the paler grey of a fog bank. */
    private static final float SKY_R = 0.56F, SKY_G = 0.60F, SKY_B = 0.58F;
    private static final float BANK_R = 0.70F, BANK_G = 0.73F, BANK_B = 0.71F;
    private static final float MIN_BRIGHTNESS = 0.12F;
    /** On a tide night the fog goes a sickly green, and never so thick that you cannot see what is out at sea. */
    private static final float TIDE_R = 0.38F, TIDE_G = 0.52F, TIDE_B = 0.40F;
    private static final float TIDE_MAX_FOG = 0.2F;
    private static final float TIDE_NEAR = 40.0F;
    private static final float TIDE_FAR = 150.0F;

    private static final ResourceKey<Biome> DROWNED_GROVE = biome("drowned_grove");
    private static final ResourceKey<Biome> FOG_MARSH = biome("fog_marsh");
    private static final ResourceKey<Biome> MUD_FLATS = biome("mud_flats");
    private static final ResourceKey<Biome> ABYSSAL_TRENCH = biome("abyssal_trench");

    private static float target;
    private static float bank;
    private static float biomeMist;

    private ClientFog() {
    }

    private static ResourceKey<Biome> biome(String name) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, name));
    }

    public static void setTarget(float level) {
        target = level;
    }

    /** Jumps straight to {@code level} (used by the dev screenshot run). */
    static void forceNow(float level) {
        target = bank = level;
    }

    /** How thick the fog is right now, 0..1. */
    public static float thickness() {
        return Mth.clamp(bank + biomeMist, 0.0F, 1.0F);
    }

    private static boolean active() {
        ClientLevel level = Minecraft.getInstance().level;
        return level != null && FogHarbor.is(level);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            target = bank = biomeMist = 0.0F;
            return;
        }
        bank = target > bank ? Math.min(target, bank + RISE) : Math.max(target, bank - LIFT);

        float mist = 0.0F;
        Holder<Biome> here = mc.level.getBiome(BlockPos.containing(mc.gameRenderer.getMainCamera().getPosition()));
        if (here.is(DROWNED_GROVE)) {
            mist = 0.3F;
        } else if (here.is(ABYSSAL_TRENCH)) {
            mist = 0.15F;
        } else if (here.is(MUD_FLATS)) {
            mist = 0.12F;
        } else if (here.is(FOG_MARSH)) {
            mist = 0.08F;
        }
        biomeMist += (mist - biomeMist) * 0.01F;
    }

    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        if (!active() || event.getType() != FogType.NONE || event.getMode() != FogRenderer.FogMode.FOG_TERRAIN) {
            return;
        }
        float t = ClientTide.active() ? Math.min(thickness(), TIDE_MAX_FOG) : thickness();
        float f = t * t * (3.0F - 2.0F * t); // smoothstep
        float far = Mth.lerp(f, HAZE_DISTANCE, BANK_DISTANCE);
        float near = Mth.lerp(f, far * 0.1F, -4.0F);
        if (ClientTide.active()) {
            // a tide night: the haze starts further out, so the shape at sea stands out against it
            far = Math.max(far, TIDE_FAR);
            near = Math.max(near, TIDE_NEAR);
        }
        // never push fog further out than vanilla wants it (render distance, blindness, darkness)
        event.setFarPlaneDistance(Math.min(event.getFarPlaneDistance(), far));
        event.setNearPlaneDistance(Math.min(event.getNearPlaneDistance(), near));
        event.setCanceled(true); // apply the modified distances
    }

    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        if (!active() || event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        float partialTick = (float) event.getPartialTick();
        float light = Mth.clamp(level.getSkyDarken(partialTick), MIN_BRIGHTNESS, 1.0F)
                * (1.0F - level.getRainLevel(partialTick) * 0.25F)
                * (1.0F - level.getThunderLevel(partialTick) * 0.3F);
        float f = Math.min(1.0F, bank * 1.2F);
        float red = Mth.lerp(f, SKY_R, BANK_R);
        float green = Mth.lerp(f, SKY_G, BANK_G);
        float blue = Mth.lerp(f, SKY_B, BANK_B);
        if (ClientTide.active()) {
            red = TIDE_R;
            green = TIDE_G;
            blue = TIDE_B;
            light = Math.max(light, 0.35F);
        }
        event.setRed(red * light);
        event.setGreen(green * light);
        event.setBlue(blue * light);
    }
}
