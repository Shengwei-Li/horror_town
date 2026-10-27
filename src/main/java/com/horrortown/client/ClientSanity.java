package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.FogHarbor;
import com.horrortown.world.Sanity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * The client side of sanity: the bar above the hunger bar, and what a failing mind does to what you see and
 * hear — whispers below 60, footsteps and things moving just behind you below 35, a darkening screen, and below
 * 15 your own heartbeat and a slow sway.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class ClientSanity {
    private static final ResourceLocation LAYER = ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "sanity");
    private static final int BAR_WIDTH = 81;

    private static float sanity = Sanity.MAX;
    private static float shown = Sanity.MAX;

    private ClientSanity() {
    }

    public static void set(float value) {
        sanity = value;
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null && mc.player != null && FogHarbor.is(mc.level) && !mc.player.isCreative()
                && !mc.player.isSpectator();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.isPaused()) {
            return;
        }
        shown += (sanity - shown) * 0.1F;
        LocalPlayer player = mc.player;
        RandomSource random = player.getRandom();
        if (sanity < 60.0F && random.nextInt(800) == 0) {
            playBehind(player, SoundEvents.AMBIENT_SOUL_SAND_VALLEY_MOOD.value(), 0.6F, 0.6F);
        }
        if (sanity < 35.0F && random.nextInt(500) == 0) {
            SoundEvent[] phantoms = {SoundEvents.ZOMBIE_AMBIENT, SoundEvents.MUD_STEP, SoundEvents.DROWNED_AMBIENT,
                    SoundEvents.WOODEN_DOOR_OPEN};
            playBehind(player, phantoms[random.nextInt(phantoms.length)], 0.8F, 0.8F + random.nextFloat() * 0.2F);
        }
        if (sanity < 15.0F && player.tickCount % 30 == 0) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WARDEN_HEARTBEAT, 0.8F, 0.5F));
        }
    }

    /** A sound from a few blocks behind the player, where they are not looking. */
    private static void playBehind(LocalPlayer player, SoundEvent sound, float volume, float pitch) {
        Vec3 behind = player.position().subtract(player.getLookAngle().multiply(4.0, 0.0, 4.0));
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.AMBIENT, volume,
                pitch, player.getRandom(), behind.x, player.getY(), behind.z));
    }

    /** Below 15 the world sways gently. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (!active() || shown >= 15.0F) {
            return;
        }
        float strength = (15.0F - shown) / 15.0F;
        float time = (float) (Minecraft.getInstance().level.getGameTime() + event.getPartialTick());
        event.setRoll(event.getRoll() + Mth.sin(time * 0.05F) * 3.0F * strength);
    }

    public static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, LAYER, (graphics, deltaTracker) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!active() || mc.options.hideGui) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        // the screen darkens at the edges of a failing mind
        if (shown < 35.0F) {
            int alpha = (int) ((35.0F - shown) / 35.0F * 90.0F);
            graphics.fill(0, 0, width, height, alpha << 24);
        }
        // the bar: an eye and a line of pale green going dark red as it empties, above the hunger bar
        int x = width / 2 + 10;
        int y = height - 39 - 10 - (mc.player.getAirSupply() < mc.player.getMaxAirSupply() ? 10 : 0);
        float fraction = shown / Sanity.MAX;
        int r = (int) Mth.lerp(fraction, 120, 150);
        int g = (int) Mth.lerp(fraction, 20, 175);
        int b = (int) Mth.lerp(fraction, 24, 150);
        graphics.fill(x - 1, y + 2, x + BAR_WIDTH + 1, y + 7, 0xC0101210);
        graphics.fill(x, y + 3, x + Math.round(BAR_WIDTH * fraction), y + 6, 0xFF000000 | r << 16 | g << 8 | b);
        // a little eye at the right end
        int ex = x + BAR_WIDTH + 3;
        graphics.fill(ex, y + 3, ex + 7, y + 6, 0xFFD8D4B8);
        graphics.fill(ex + 1, y + 2, ex + 6, y + 7, 0xFFD8D4B8);
        graphics.fill(ex + 3, y + 3, ex + 4, y + 6, 0xFF101010);
    }
}
