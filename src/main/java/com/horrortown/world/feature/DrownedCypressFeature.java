package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 水杉: a tall, straight, grey trunk standing in the shallows, roots flaring out into the mud, a few bare
 * branches high up with thin tufts of leaves and vines hanging off them. Some have lost their crown.
 */
public class DrownedCypressFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAX_WATER = 4;

    public DrownedCypressFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin();
        if (!TreeBits.isGround(level.getBlockState(base.below())) || TreeBits.waterDepth(level, base) > MAX_WATER) {
            return false;
        }
        boolean thick = random.nextFloat() < 0.35F;
        boolean broken = random.nextFloat() < 0.25F;
        int height = 16 + random.nextInt(14) + (thick ? 6 : 0);
        if (broken) {
            height = height * 2 / 3;
        }
        int size = thick ? 2 : 1;
        for (int y = 0; y < height + 2; y++) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    if (!TreeBits.canReplace(level, base.offset(dx, y, dz))) {
                        return false;
                    }
                }
            }
        }

        // trunk, sunk one block into the ground
        for (int y = -1; y < height; y++) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    TreeBits.log(level, base.offset(dx, y, dz), Direction.Axis.Y);
                }
            }
        }
        roots(level, random, base, size);
        if (broken) {
            // a jagged snapped top and maybe one dead stub of a branch
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {
                    if (random.nextBoolean()) {
                        TreeBits.logIfFree(level, base.offset(dx, height, dz), Direction.Axis.Y);
                    }
                }
            }
            if (random.nextBoolean()) {
                branch(level, random, base.above(height - 2 - random.nextInt(3)), size, false);
            }
            return true;
        }

        int branches = 2 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            int y = height - 1 - random.nextInt(Math.max(1, height * 2 / 5));
            branch(level, random, base.above(y), size, true);
        }
        // a thin spire of a crown
        BlockPos top = base.offset(size / 2, height, size / 2);
        TreeBits.logIfFree(level, top, Direction.Axis.Y);
        TreeBits.clump(level, random, top.above(), 1, 0.6F);
        TreeBits.clump(level, random, top.below(2), 2, 0.25F);
        return true;
    }

    /** Buttress roots spreading out of the trunk base and bending down into the mud. */
    private static void roots(WorldGenLevel level, RandomSource random, BlockPos base, int size) {
        float chance = size == 2 ? 0.5F : 0.75F;
        for (int cx = 0; cx < size; cx++) {
            for (int cz = 0; cz < size; cz++) {
                for (Direction dir : Direction.Plane.HORIZONTAL) {
                    int nx = cx + dir.getStepX();
                    int nz = cz + dir.getStepZ();
                    // only from trunk cells facing out on this side
                    if ((nx >= 0 && nx < size && nz >= 0 && nz < size) || random.nextFloat() > chance) {
                        continue;
                    }
                    root(level, base.offset(cx, 0, cz), dir, 1 + random.nextInt(size == 2 ? 4 : 3), base.getY());
                }
            }
        }
    }

    private static void root(WorldGenLevel level, BlockPos start, Direction dir, int length,
                             int baseY) {
        for (int i = 1; i <= length; i++) {
            int y = i <= length / 2 ? 1 : 0;
            BlockPos pos = start.relative(dir, i).above(y);
            if (!TreeBits.logIfFree(level, pos, dir.getAxis())) {
                return;
            }
            // fill down to the ground so the root does not float
            BlockPos below = pos.below();
            while (TreeBits.canReplace(level, below) && below.getY() > baseY - 3) {
                TreeBits.log(level, below, Direction.Axis.Y);
                below = below.below();
            }
        }
    }

    /** A bare branch reaching out (and a little up) from the trunk, with a thin tuft of leaves at its end. */
    private static void branch(WorldGenLevel level, RandomSource random, BlockPos from, int size, boolean leaves) {
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos pos = from.offset(dir.getStepX() > 0 ? size - 1 : 0, 0, dir.getStepZ() > 0 ? size - 1 : 0);
        int length = 2 + random.nextInt(3);
        for (int i = 0; i < length; i++) {
            pos = pos.relative(dir);
            if (i > 0 && random.nextInt(3) == 0) {
                pos = pos.above();
            }
            if (!TreeBits.logIfFree(level, pos, dir.getAxis())) {
                return;
            }
        }
        if (leaves) {
            TreeBits.clump(level, random, pos.above(), 2, 0.35F);
        } else if (random.nextInt(3) == 0) {
            TreeBits.hangingVine(level, random, pos.below(), 2 + random.nextInt(4));
        }
    }
}
