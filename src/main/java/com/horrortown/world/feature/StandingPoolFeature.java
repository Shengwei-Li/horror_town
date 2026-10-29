package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 死水塘: a shallow pool of stagnant water in a dip of the ground, its bed and banks gone to mud. Small, and only
 * where the ground around it is level enough to hold it.
 */
public class StandingPoolFeature extends Feature<NoneFeatureConfiguration> {
    public StandingPoolFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos surface = context.origin().below();
        int radius = 2 + random.nextInt(3);
        // every block of the rim has to be solid ground at this level, or the water would run out
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                BlockPos rim = surface.offset(dx, 0, dz);
                if (!TreeBits.isGround(level.getBlockState(rim)) || !level.getBlockState(rim.above(2)).isAir()) {
                    return false;
                }
            }
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) + random.nextFloat() * 0.6;
                if (d > radius) {
                    continue;
                }
                int depth = d < radius * 0.5 ? 2 : 1;
                BlockPos top = surface.offset(dx, 0, dz);
                for (int y = 0; y < depth; y++) {
                    level.setBlock(top.below(y), Blocks.WATER.defaultBlockState(), 2);
                }
                level.setBlock(top.below(depth), Blocks.MUD.defaultBlockState(), 2);
                level.setBlock(top.above(), Blocks.AIR.defaultBlockState(), 2); // the grass that stood there
            }
        }
        return true;
    }
}
