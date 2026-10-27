package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.town.Builder;
import com.horrortown.town.TownSite;
import com.horrortown.world.FogWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Development only, off unless the game is started with {@code -Dhorror_town.autoshot}: once a singleplayer
 * world is loaded, minimizes the window, turns the player into a spectator, steps through a set of camera spots around the town at
 * different times of day and fog, saves a screenshot of each to {@code screenshots/ht_*.png}, then quits.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class DevAutoShot {
    static final boolean ENABLED = System.getProperty("horror_town.autoshot") != null;
    /** Ticks to let chunks load before the first shot, and between shots. */
    private static final int WARMUP = 400;
    private static final int STEP = 140;
    private static final int SETTLE = 120;
    /** {time of day, fog, camera x, y, z, target x, y, z} in the town's frame (y from the deck). */
    private static final float[][] SHOTS = {
            {6000, 0.0F, 0, 2, -66, 0, 4, 0},
            {6000, 0.0F, 0, 2, -30, 0, 3, 20},
            {6000, 0.0F, 0, 3, 35, 0, 12, 75},
            {6000, 0.0F, 0, 4, 63, 0, 12, 79},
            {6000, 0.0F, -30, 40, -80, 0, 0, 10},
            {6000, 0.0F, -5, 2, 1, -9, 2, -2},
            {12600, 0.6F, 2, 2, -10, 0, 3, 60},
            {18000, 0.0F, 0, 2, -30, 0, 3, 20},
            {18000, 0.0F, 0, 4, 63, 0, 12, 79},
            {6000, 0.0F, 45, 30, 40, 0, 5, 30},
            {6000, 0.0F, 0, 9, 66, 0, 11, 79},
            {18000, 0.0F, 0, 9, 66, 0, 13, 79},
    };

    private static int ticks;

    private DevAutoShot() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        IntegratedServer server = mc.getSingleplayerServer();
        if (!ENABLED || mc.level == null || mc.player == null || server == null) {
            return;
        }
        if (ticks == 0) {
            // minimize as soon as the world is in, so the game does not hold on to the mouse and keyboard;
            // it keeps rendering (and screenshotting) in the background
            GLFW.glfwIconifyWindow(mc.getWindow().getWindow());
        }
        int t = ++ticks - WARMUP;
        if (t < 0) {
            return;
        }
        int index = t / STEP;
        if (index >= SHOTS.length) {
            mc.stop();
            return;
        }
        float[] shot = SHOTS[index];
        mc.options.hideGui = true;
        if (mc.screen != null) {
            mc.setScreen(null);
        }
        if (t % STEP == 0) {
            server.execute(() -> {
                ServerLevel level = server.overworld();
                level.setDayTime((long) shot[0]);
                FogWeather.get(level).set(level, shot[1], 1_000_000);
                TownSite.get(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level,
                        level.getSeed()).ifPresent(site -> {
                    Vec3 eye = town(site, shot[2], shot[3], shot[4]);
                    Vec3 look = town(site, shot[5], shot[6], shot[7]).subtract(eye);
                    float yaw = (float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
                    float pitch = (float) (-Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG);
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        player.setGameMode(GameType.SPECTATOR);
                        player.teleportTo(level, eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
                    }
                });
            });
            ClientFog.forceNow(shot[1]);
        } else if (t % STEP == SETTLE) {
            ClientFog.forceNow(shot[1]);
            Screenshot.grab(mc.gameDirectory, "ht_" + index + ".png", mc.getMainRenderTarget(), message -> {
            });
        }
    }

    private static Vec3 town(TownSite site, float x, float y, float z) {
        BlockPos pos = Builder.toWorld(site.centre(), site.seaward(), Mth.floor(x), Mth.floor(y), Mth.floor(z));
        return Vec3.atBottomCenterOf(pos);
    }
}
