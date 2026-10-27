package com.horrortown.world.feature;

import com.horrortown.registry.ModGear;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 潮骨: the skeleton of something enormous on the sea floor, half sunk in the silt: a spine running along the
 * bottom with ribs curving up out of it on both sides like the frame of a wrecked boat.
 */
public class TideBoneFeature extends Feature<NoneFeatureConfiguration> {
    private static final int MIN_DEPTH = 4;

    public TideBoneFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin();
        if (!level.getFluidState(base.above(MIN_DEPTH - 1)).is(FluidTags.WATER)) {
            return false;
        }
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction side = along.getClockWise();
        int length = 8 + random.nextInt(10);
        int ribs = 3 + random.nextInt(3);
        BlockPos spine = base.below().relative(along.getOpposite(), length / 2);
        for (int i = 0; i < length; i++) {
            BlockPos pos = spine.relative(along, i);
            set(level, random.nextInt(3) == 0 ? pos.below() : pos, along.getAxis(), true);
            // the ribs, at intervals along the middle of the spine, the biggest in the middle
            int fromMiddle = Math.abs(i - length / 2);
            if (i % 3 == 1 && fromMiddle <= ribs * 3 / 2) {
                int radius = Math.max(2, 6 - fromMiddle / 2 - random.nextInt(2));
                for (int s : new int[]{1, -1}) {
                    if (random.nextInt(6) == 0) {
                        continue; // broken off
                    }
                    for (int a = 0; a <= 80; a += 10) {
                        double r = Math.toRadians(a);
                        int out = (int) Math.round(Math.cos(r) * radius);
                        int up = (int) Math.round(Math.sin(r) * radius * 1.3);
                        set(level, pos.relative(side, s * out).above(up), Direction.Axis.Y, false);
                    }
                }
            }
        }
        return true;
    }

    /** Bone in place of water, or (for the half-buried spine) of the silt itself. */
    private static void set(WorldGenLevel level, BlockPos pos, Direction.Axis axis, boolean buried) {
        BlockState there = level.getBlockState(pos);
        if (there.getFluidState().is(FluidTags.WATER) || there.canBeReplaced() || buried && !there.isAir()) {
            level.setBlock(pos, ModGear.TIDE_BONE.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 2);
        }
    }
}
