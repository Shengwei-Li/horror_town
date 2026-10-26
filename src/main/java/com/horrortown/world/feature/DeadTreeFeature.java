package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** 枯树 and 断桩: a crooked, leafless dead tree leaning off to one side, or just a rotten stump. */
public class DeadTreeFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MAX_WATER = 2;

    public DeadTreeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin();
        if (!TreeBits.isGround(level.getBlockState(base.below())) || TreeBits.waterDepth(level, base) > MAX_WATER
                || !TreeBits.canReplace(level, base)) {
            return false;
        }

        if (random.nextFloat() < 0.3F) {
            int height = 1 + random.nextInt(3);
            for (int y = 0; y < height; y++) {
                if (!TreeBits.logIfFree(level, base.above(y), Direction.Axis.Y)) {
                    break;
                }
            }
            return true;
        }

        Direction lean = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        int height = 4 + random.nextInt(6);
        BlockPos pos = base;
        TreeBits.log(level, base.below(), Direction.Axis.Y);
        for (int y = 0; y < height; y++) {
            if (!TreeBits.logIfFree(level, pos, Direction.Axis.Y)) {
                return true;
            }
            // the trunk kinks over towards the lean every few blocks
            if (y > 1 && random.nextInt(3) == 0) {
                pos = pos.relative(lean);
                if (!TreeBits.logIfFree(level, pos, lean.getAxis())) {
                    return true;
                }
            }
            pos = pos.above();
        }

        int branches = 1 + random.nextInt(3);
        for (int i = 0; i < branches; i++) {
            Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            BlockPos branch = pos.below(1 + random.nextInt(3));
            int length = 1 + random.nextInt(3);
            for (int j = 0; j < length; j++) {
                branch = branch.relative(dir);
                if (random.nextBoolean()) {
                    branch = branch.above();
                }
                if (!TreeBits.logIfFree(level, branch, dir.getAxis())) {
                    break;
                }
            }
            if (random.nextInt(3) == 0) {
                TreeBits.hangingVine(level, random, branch.below(), 1 + random.nextInt(4));
            }
        }
        return true;
    }
}
