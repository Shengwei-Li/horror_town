package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 弃船: a fishing boat left high and dry a long way from any water, overturned, its hull half sunk in the cracked
 * ground and rotted through in places, as if the sea had gone out one night and never come back for it.
 */
public class WreckedBoatFeature extends Feature<NoneFeatureConfiguration> {
    public WreckedBoatFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin().below();
        if (!TreeBits.isGround(level.getBlockState(base)) || !level.getBlockState(base.above()).isAir()) {
            return false;
        }
        Direction along = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction across = along.getClockWise();
        int length = 6 + random.nextInt(3);
        // keeled over: one side has sunk a block deeper than the other
        int sunk = random.nextBoolean() ? 1 : -1;
        BlockState planks = Blocks.SPRUCE_PLANKS.defaultBlockState();
        BlockState dark = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState slab = Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int i = 0; i < length; i++) {
            boolean end = i == 0 || i == length - 1;
            BlockPos row = base.relative(along, i - length / 2);
            if (end) {
                // the bow and stern: a single post, the bow standing a little proud
                set(level, random, row.above(i == 0 ? 2 : 1), dark);
                set(level, random, row.above(1), planks);
                continue;
            }
            for (int s = -1; s <= 1; s++) {
                BlockPos col = row.relative(across, s);
                int drop = s == sunk ? 1 : 0;
                if (s == 0) {
                    // the keel along the top of the upturned hull
                    set(level, random, col.above(2 - drop), random.nextInt(4) == 0 ? slab : dark);
                } else {
                    // the sides, gunwales down in the dirt
                    set(level, random, col.above(1 - drop), planks);
                    set(level, random, col.above(2 - drop), random.nextInt(3) == 0 ? slab : planks);
                }
            }
        }
        // the oar it was left with
        if (random.nextBoolean()) {
            BlockPos oar = base.relative(across, 3).relative(along, random.nextInt(3) - 1).above();
            if (level.getBlockState(oar).isAir()) {
                level.setBlock(oar, Blocks.SPRUCE_FENCE.defaultBlockState(), 2);
            }
        }
        return true;
    }

    /** One board of the hull, unless it has rotted away. */
    private static void set(WorldGenLevel level, RandomSource random, BlockPos pos, BlockState state) {
        if (random.nextFloat() < 0.15F) {
            return;
        }
        BlockState there = level.getBlockState(pos);
        if (there.isAir() || there.canBeReplaced() || TreeBits.isGround(there)) {
            level.setBlock(pos, state, 2);
        }
    }
}
