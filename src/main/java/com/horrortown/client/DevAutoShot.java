package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.town.Builder;
import com.horrortown.town.TownSite;
import com.horrortown.registry.ModMobs;
import com.horrortown.world.FogWeather;
import com.horrortown.world.TideNight;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.arguments.EntityAnchorArgument;
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
    /** {time of day, fog (negative: a tide night), camera x, y, z, target x, y, z} in the town's frame. */
    /** A fog value that means: stand one of every creature in a row on the street, frozen, to be looked at. */
    private static final float LINEUP = -2.0F;
    /** A fog value that means: raise the Tide Mother out at sea in front of the town, held mid-slam. */
    private static final float MOTHER = -3.0F;
    private static final float[][] SHOTS = {
            {6000, MOTHER, 1000, 0, 0, 0, 0, 0},
            {6000, MOTHER, 1001, 0, 0, 0, 0, 0},
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
                FogWeather.get(level).set(level, Math.max(0.0F, shot[1]), 1_000_000);
                level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class,
                        new net.minecraft.world.phys.AABB(level.getSharedSpawnPos()).inflate(200),
                        m -> !m.isNoAi()).forEach(net.minecraft.world.entity.Entity::discard);
                if (shot[1] == LINEUP) {
                    lineUp(level);
                } else if (shot[1] == MOTHER) {
                    raiseMother(level);
                } else if (shot[1] < 0.0F && !TideNight.get(level).active()) {
                    TideNight.get(level).start(level);
                }
                TownSite.get(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level,
                        level.getSeed()).ifPresent(site -> {
                    Vec3 eye = town(site, shot[2], shot[3], shot[4]);
                    Vec3 look = town(site, shot[5], shot[6], shot[7]).subtract(eye);
                    if (shot[2] >= 1000 && motherAt != null) {
                        // 1000: from her side, out over the water; 1001: from above, looking down on her slam
                        Vec3 side = Vec3.atLowerCornerOf(site.seaward().getClockWise().getNormal());
                        Vec3 sea = Vec3.atLowerCornerOf(site.seaward().getNormal());
                        eye = shot[2] == 1000 ? motherAt.add(side.scale(48)).add(sea.scale(-20)).add(0, 26, 0)
                                : motherAt.add(sea.scale(-10)).add(side.scale(6)).add(0, 70, 0);
                        look = (shot[2] == 1000 ? motherAt.add(sea.scale(-16)).add(0, 14, 0)
                                : motherAt.add(sea.scale(-26))).subtract(eye);
                    }
                    float yaw = (float) (Mth.atan2(-look.x, look.z) * Mth.RAD_TO_DEG);
                    float pitch = (float) (-Mth.atan2(look.y, look.horizontalDistance()) * Mth.RAD_TO_DEG);
                    for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                        player.setGameMode(GameType.SPECTATOR);
                        player.teleportTo(level, eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
                    }
                });
            });
            ClientFog.forceNow(Math.max(0.0F, shot[1]));
            if (shot[1] < 0.0F && shot[1] != LINEUP && shot[1] != MOTHER) {
                WatcherApparition.riseNow();
            }
        } else if (t % STEP == SETTLE - 20 && shot[1] < 0.0F && shot[1] != LINEUP && shot[1] != MOTHER
                && WatcherApparition.position() != null) {
            // tide shots: turn to face the Watcher, a second before the picture is taken
            mc.player.lookAt(EntityAnchorArgument.Anchor.EYES, WatcherApparition.position().add(0, 20, 0));
            mc.player.yRotO = mc.player.getYRot();
            mc.player.xRotO = mc.player.getXRot();
        } else if (t % STEP == SETTLE) {
            ClientFog.forceNow(Math.max(0.0F, shot[1]));
            Screenshot.grab(mc.gameDirectory, "ht_" + index + ".png", mc.getMainRenderTarget(), message -> {
            });
        }
    }

    private static boolean linedUp;

    /** One of each creature, stood in a row across the street, frozen and facing the camera. */
    private static void lineUp(ServerLevel level) {
        if (linedUp) {
            return;
        }
        linedUp = true;
        TownSite.get(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level,
                level.getSeed()).ifPresent(site -> {
            var types = ModMobs.ENTITIES.getEntries().stream().map(holder -> holder.get()).toList();
            for (int i = 0; i < types.size(); i++) {
                Vec3 at = town(site, -17.5F + i * 3.5F, 1, -20);
                var entity = types.get(i).create(level);
                if (entity instanceof net.minecraft.world.entity.Mob mob) {
                    var tag = new net.minecraft.nbt.CompoundTag();
                    mob.saveWithoutId(tag);
                    tag.putBoolean("Buried", false);
                    mob.load(tag);
                    mob.setNoAi(true);
                    mob.setPersistenceRequired();
                    mob.moveTo(at.x, at.y, at.z, site.seaward().getOpposite().toYRot(), 0.0F);
                    mob.setYHeadRot(site.seaward().getOpposite().toYRot());
                    mob.setYBodyRot(site.seaward().getOpposite().toYRot());
                    level.addFreshEntity(mob);
                }
            }
            var mother = com.horrortown.registry.ModEntities.TIDE_MOTHER_TRUE.get().create(level);
            if (mother != null) {
                Vec3 at = town(site, 0, 1, -6);
                mother.setNoAi(true);
                mother.moveTo(at.x, at.y, at.z, site.seaward().getOpposite().toYRot(), 0.0F);
                mother.setYBodyRot(site.seaward().getOpposite().toYRot());
                mother.setYHeadRot(site.seaward().getOpposite().toYRot());
                level.addFreshEntity(mother);
            }
        });
    }

    private static boolean motherUp;
    private static Vec3 motherAt;

    /** The Tide Mother out in the first deep water in front of the town, facing it, held mid-slam, with a
     * glowstone post where that slam is worked out to land. */
    private static void raiseMother(ServerLevel level) {
        if (motherUp) {
            return;
        }
        motherUp = true;
        TownSite.get(level.getChunkSource().getGenerator(), level.getChunkSource().randomState(), level,
                level.getSeed()).ifPresent(site -> {
            for (int z = 60; z <= 120; z += 2) {
                BlockPos column = Builder.toWorld(site.centre(), site.seaward(), 0, 0, z);
                int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
                int floor = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR, column.getX(), column.getZ());
                if (surface - floor < 3 && z < 120) {
                    continue;
                }
                var mother = com.horrortown.registry.ModEntities.TIDE_MOTHER_TRUE.get().create(level);
                float yaw = site.seaward().getOpposite().toYRot();
                mother.setNoAi(true);
                mother.moveTo(column.getX() + 0.5, floor, column.getZ() + 0.5, yaw, 0.0F);
                mother.setYBodyRot(yaw);
                mother.setYHeadRot(yaw);
                Vec3 lands = mother.pose(2);
                level.addFreshEntity(mother);
                motherAt = mother.position();
                for (int dy = 0; dy < 4; dy++) {
                    level.setBlockAndUpdate(BlockPos.containing(lands).above(dy), net.minecraft.world.level.block.Blocks.GLOWSTONE.defaultBlockState());
                }
                return;
            }
        });
    }

    private static Vec3 town(TownSite site, float x, float y, float z) {
        BlockPos pos = Builder.toWorld(site.centre(), site.seaward(), Mth.floor(x), Mth.floor(y), Mth.floor(z));
        return Vec3.atBottomCenterOf(pos);
    }
}
