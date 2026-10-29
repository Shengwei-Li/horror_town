package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Barnacles over a whole chunk, far from the sea: crusted round the feet of the tree trunks (up to a few blocks
 * off the ground), or, on the crags, scattered over the bare rock; thick near the coast and thinning out towards
 * the middle of the island. Runs once per chunk, after the trees.
 */
public class BarnacleGrowthFeature extends Feature<NoneFeatureConfiguration> {
    /** How high up a trunk they climb, and how thick they are there. */
    private static final int TRUNK_HEIGHT = 4;
    private static final float TRUNK_CHANCE = 0.45F;
    private static final float ROCK_CHANCE = 0.06F;
    private final boolean rocks;

    public BarnacleGrowthFeature(Codec<NoneFeatureConfiguration> codec, boolean rocks) {
        super(codec);
        this.rocks = rocks;
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos corner = context.origin();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        float mark = InlandBits.seaMark(corner);
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                int x = corner.getX() + dx;
                int z = corner.getZ() + dz;
                if (this.rocks) {
                    int ground = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                    // the bare rock face: the top block and the few below it, wherever it meets the air
                    for (int y = ground - 1; y > ground - 6; y--) {
                        pos.set(x, y, z);
                        BlockState rock = level.getBlockState(pos);
                        if (!rock.is(BlockTags.BASE_STONE_OVERWORLD)) {
                            continue;
                        }
                        for (Direction d : Direction.values()) {
                            if (d != Direction.DOWN && random.nextFloat() < ROCK_CHANCE * mark) {
                                InlandBits.barnacle(level, pos.relative(d), d.getOpposite());
                            }
                        }
                    }
                    continue;
                }
                // trunks: from the treetops down, find where each one stands on the ground
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y > top - 40; y--) {
                    pos.set(x, y, z);
                    if (!level.getBlockState(pos).is(BlockTags.LOGS)) {
                        continue;
                    }
                    BlockState below = level.getBlockState(pos.below());
                    if (below.is(BlockTags.LOGS) || below.is(BlockTags.LEAVES) || !below.isSolid()) {
                        continue;
                    }
                    for (int h = 0; h < TRUNK_HEIGHT; h++) {
                        BlockPos log = pos.above(h);
                        if (!level.getBlockState(log).is(BlockTags.LOGS)) {
                            break;
                        }
                        float chance = TRUNK_CHANCE * mark * (1.0F - (float) h / TRUNK_HEIGHT * 0.6F);
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            if (random.nextFloat() < chance) {
                                InlandBits.barnacle(level, log.relative(d), d.getOpposite());
                            }
                        }
                    }
                    break;
                }
            }
        }
        return true;
    }
}
