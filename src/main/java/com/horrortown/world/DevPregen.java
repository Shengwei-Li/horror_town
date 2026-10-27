package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Development only (-Dhorror_town.pregen): once the world has been generated, draw a map of the overworld's
 * biomes and terrain height around the centre to {@code fog_harbor_map.png}, then stop the server.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID)
public final class DevPregen {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int HALF_SIZE = 3600;
    private static final int STEP = 32;
    private static final Map<String, Integer> COLOURS = Map.of(
            "mud_flats", 0x3A3528,
            "fog_marsh", 0x5E6B45,
            "salt_flats", 0xD8D4C4,
            "drowned_grove", 0x2F4A2A,
            "murky_sea", 0x2A4652,
            "abyssal_trench", 0x0E1C26);

    private DevPregen() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (System.getProperty("horror_town.pregen") == null) {
            return;
        }
        var server = event.getServer();
        // the dev client opens this world unattended: skip the "experimental settings, make a backup?" prompt
        if (server.getWorldData() instanceof PrimaryLevelData data) {
            data.withConfirmedWarning(true);
        }
        // drawing takes a while: do it off the server thread so the watchdog does not kill the server
        Thread thread = new Thread(() -> {
            try {
                drawMap(server.overworld());
            } catch (IOException | RuntimeException e) {
                LOGGER.error("Could not draw the world map", e);
            }
            server.execute(() -> server.halt(false));
        }, "Horror Town map");
        thread.setDaemon(true);
        thread.start();
    }

    private static void drawMap(ServerLevel level) throws IOException {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        int size = HALF_SIZE * 2 / STEP;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        int sea = generator.getSeaLevel();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                int x = -HALF_SIZE + i * STEP;
                int z = -HALF_SIZE + j * STEP;
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                Holder<Biome> biome = generator.getBiomeSource().getNoiseBiome(x >> 2, sea >> 2, z >> 2, random.sampler());
                String name = biome.unwrapKey().map(k -> k.location().getPath()).orElse("");
                int rgb = COLOURS.getOrDefault(name, 0xFF00FF);
                // land gets lighter with height, sea darker with depth
                float shade = height >= sea ? 1.0F + Math.min(0.6F, (height - sea) / 40.0F) : Math.max(0.35F, 1.0F - (sea - height) / 50.0F);
                int r = Math.min(255, (int) (((rgb >> 16) & 255) * shade));
                int g = Math.min(255, (int) (((rgb >> 8) & 255) * shade));
                int b = Math.min(255, (int) ((rgb & 255) * shade));
                if (Math.abs(x) < STEP && Math.abs(z) < STEP) {
                    r = 255; g = 0; b = 0;
                }
                image.setRGB(i, j, (r << 16) | (g << 8) | b);
            }
        }
        var spawn = level.getSharedSpawnPos();
        int sx = (spawn.getX() + HALF_SIZE) / STEP;
        int sz = (spawn.getZ() + HALF_SIZE) / STEP;
        for (int d = -3; d <= 3; d++) {
            for (int[] p : new int[][]{{sx + d, sz}, {sx, sz + d}}) {
                if (p[0] >= 0 && p[0] < size && p[1] >= 0 && p[1] < size) {
                    image.setRGB(p[0], p[1], 0xFFFF00);
                }
            }
        }
        File file = new File("fog_harbor_map.png");
        ImageIO.write(image, "png", file);
        LOGGER.info("Wrote world map to {} (spawn at {})", file.getAbsolutePath(), spawn);
    }
}
