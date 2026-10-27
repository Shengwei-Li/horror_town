package com.horrortown.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Where the town stands: a spot on the island's coast with land behind it and open water in front.
 * Worked out from the seed alone, so the structure placement and the world spawn agree on it.
 *
 * @param centre   where the main street meets the waterfront, at deck height
 * @param seaward  the direction the pier and temple go out into the sea
 */
public record TownSite(BlockPos centre, Direction seaward) {
    /** The boardwalks and house floors sit this far above sea level. */
    public static final int DECK_ABOVE_SEA = 2;
    /** How far the gate stands inland of the centre; spawn is just outside it. */
    public static final int GATE_DISTANCE = 56;

    private static final int RAYS = 24;
    private static final int MIN_RADIUS = 800;
    private static final int MAX_RADIUS = 3000;
    private static final int STEP = 8;
    /** The coastline: continentalness where the town centre goes (see IslandContinents). */
    private static final float COAST = -0.11F;
    private static final Map<Long, Optional<TownSite>> CACHE = new ConcurrentHashMap<>();

    public static Optional<TownSite> get(ChunkGenerator generator, RandomState random, LevelHeightAccessor height,
                                         long seed) {
        return CACHE.computeIfAbsent(seed, s -> find(generator, random, height, s));
    }

    /** Where new players appear: on the road just outside the town gate. */
    public BlockPos spawn() {
        return this.centre.relative(this.seaward, -GATE_DISTANCE - 4);
    }

    private static Optional<TownSite> find(ChunkGenerator generator, RandomState random, LevelHeightAccessor height,
                                           long seed) {
        Climate.Sampler sampler = random.sampler();
        int sea = generator.getSeaLevel();
        float start = (float) (Math.floorMod(seed, 360L) * Mth.DEG_TO_RAD);
        for (int ray = 0; ray < RAYS; ray++) {
            float angle = start + ray * Mth.TWO_PI / RAYS;
            for (int r = MIN_RADIUS; r <= MAX_RADIUS; r += STEP) {
                int x = Mth.floor(Mth.cos(angle) * r);
                int z = Mth.floor(Mth.sin(angle) * r);
                float c = Climate.unquantizeCoord(sampler.sample(x >> 2, sea >> 2, z >> 2).continentalness());
                if (c > COAST) {
                    continue;
                }
                BlockPos centre = new BlockPos(x, sea + DECK_ABOVE_SEA, z);
                // straight out from the island centre, snapped to the nearest compass direction
                Direction seaward = Direction.fromYRot(Mth.atan2(-x, z) * Mth.RAD_TO_DEG);
                if (fits(generator, random, height, centre, seaward, sea)) {
                    return Optional.of(new TownSite(centre, seaward));
                }
                break; // this ray's coast does not suit; try the next one
            }
        }
        return Optional.empty();
    }

    /** Land behind the centre for the street, water in front for the pier and temple. */
    private static boolean fits(ChunkGenerator generator, RandomState random, LevelHeightAccessor height,
                                BlockPos centre, Direction seaward, int sea) {
        // the street needs low, flat land: dry, but not so high it has to be dug through
        int land = 0;
        for (int d = 10; d <= GATE_DISTANCE; d += 6) {
            int y = surface(generator, random, height, centre.relative(seaward, -d));
            if (y >= sea - 1 && y <= sea + DECK_ABOVE_SEA + 1) {
                land++;
            }
        }
        int water = 0;
        for (int d = 40; d <= 90; d += 6) {
            if (surface(generator, random, height, centre.relative(seaward, d)) < sea - 1) {
                water++;
            }
        }
        return land >= 6 && water >= 7;
    }

    private static int surface(ChunkGenerator generator, RandomState random, LevelHeightAccessor height, BlockPos pos) {
        return generator.getBaseHeight(pos.getX(), pos.getZ(), Heightmap.Types.OCEAN_FLOOR_WG, height, random);
    }
}
