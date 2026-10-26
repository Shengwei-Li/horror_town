package com.horrortown.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Optional;

/** Finds a dry spot on the island's coast: walks outwards from the centre until the land turns into coast. */
final class CoastSpawn {
    private static final int RAYS = 16;
    private static final int MIN_RADIUS = 800;
    private static final int MAX_RADIUS = 3000;
    private static final int STEP = 16;
    /** Continentalness of the coastal ring (see IslandContinents). */
    private static final float COAST_INNER = -0.06F;
    private static final float COAST_OUTER = -0.19F;

    private CoastSpawn() {
    }

    static Optional<BlockPos> find(ServerLevel level) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState random = level.getChunkSource().randomState();
        Climate.Sampler sampler = random.sampler();
        int sea = generator.getSeaLevel();
        float start = (float) (Math.floorMod(level.getSeed(), 360L) * Mth.DEG_TO_RAD);
        for (int ray = 0; ray < RAYS; ray++) {
            float angle = start + ray * Mth.TWO_PI / RAYS;
            for (int r = MIN_RADIUS; r <= MAX_RADIUS; r += STEP) {
                int x = Mth.floor(Mth.cos(angle) * r);
                int z = Mth.floor(Mth.sin(angle) * r);
                float c = Climate.unquantizeCoord(sampler.sample(x >> 2, sea >> 2, z >> 2).continentalness());
                if (c > COAST_INNER) {
                    continue;
                }
                if (c < COAST_OUTER) {
                    break; // reached the sea on this ray without finding dry coast
                }
                int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
                if (height > sea) {
                    return Optional.of(new BlockPos(x, height, z));
                }
            }
        }
        return Optional.empty();
    }
}
