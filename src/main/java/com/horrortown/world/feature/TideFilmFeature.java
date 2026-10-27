package com.horrortown.world.feature;

import com.horrortown.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.synth.PerlinSimplexNoise;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

import java.util.List;

/**
 * Lays a film of shallow water over the low mud of the flats: every bare mud surface right at sea level, in
 * broad wandering patches, so the flats near the sea look permanently half drowned. Runs once per chunk.
 */
public class TideFilmFeature extends Feature<NoneFeatureConfiguration> {
    /** Scale and cut-off of the patch noise: lower cut-off, more of the flats under water. */
    private static final double PATCH_SCALE = 0.04;
    private static final double PATCH_CUTOFF = -0.25;
    private static final PerlinSimplexNoise PATCHES =
            new PerlinSimplexNoise(new WorldgenRandom(new LegacyRandomSource(2077L)), List.of(0));

    public TideFilmFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos corner = context.origin();
        int sea = context.chunkGenerator().getSeaLevel();
        BlockState film = ModBlocks.TIDE_FILM.get().defaultBlockState();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = (corner.getX() & ~15) + dx;
                int z = (corner.getZ() & ~15) + dz;
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
                if (top - 1 < sea - 1 || top - 1 > sea) {
                    continue; // only the lowest mud, right at the waterline
                }
                BlockState ground = level.getBlockState(pos.set(x, top - 1, z));
                if (!ground.is(ModBlocks.ROT_MUD.get())) {
                    continue;
                }
                if (PATCHES.getValue(x * PATCH_SCALE, z * PATCH_SCALE, false) < PATCH_CUTOFF) {
                    continue;
                }
                if (level.getBlockState(pos.set(x, top, z)).isAir()) {
                    level.setBlock(pos, film, 2);
                    placed = true;
                }
            }
        }
        return placed;
    }
}
