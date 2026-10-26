package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.FogWeather;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Development only, off unless the game is started with {@code -Dhorror_town.autoshot}: once a singleplayer
 * world is loaded, turns the player into a spectator, steps through a few times of day / fog levels / view
 * directions, saves a screenshot of each to {@code screenshots/ht_*.png}, then quits.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class DevAutoShot {
    private static final boolean ENABLED = System.getProperty("horror_town.autoshot") != null;
    /** Ticks to let chunks load before the first shot, and between shots. */
    private static final int WARMUP = 400;
    private static final int STEP = 120;
    private static final int SETTLE = 100;
    /** {time of day, fog, yaw, pitch, height above spawn} */
    private static final float[][] SHOTS = {
            {6000, 0.0F, 0, 5, 2},
            {6000, 0.0F, 90, 5, 2},
            {6000, 0.0F, 180, 5, 2},
            {6000, 0.0F, 270, 5, 2},
            {6000, 0.9F, 0, 5, 2},
            {6000, 0.0F, 0, -25, 2},
            {12600, 0.0F, 270, 0, 2},
            {18000, 0.0F, 90, 5, 2},
            {6000, 0.0F, 0, 35, 60},
            {6000, 0.0F, 180, 35, 60},
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
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    player.setGameMode(GameType.SPECTATOR);
                    var spawn = level.getSharedSpawnPos();
                    double y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            spawn.getX(), spawn.getZ()) + shot[4];
                    player.teleportTo(level, spawn.getX() + 0.5, y, spawn.getZ() + 0.5, shot[2], shot[3]);
                }
            });
            ClientFog.forceNow(shot[1]);
        } else if (t % STEP == SETTLE) {
            ClientFog.forceNow(shot[1]);
            Screenshot.grab(mc.gameDirectory, "ht_" + index + ".png", mc.getMainRenderTarget(), message -> {
            });
        }
    }
}
