package com.horrortown.world.feature;

import com.horrortown.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Shared bits for the drowned trees: what they may grow through, logs, sparse leaves, hanging vines. */
final class TreeBits {
    private TreeBits() {
    }

    /** Ground a drowned tree can stand on. */
    static boolean isGround(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(Blocks.CLAY) || state.is(Blocks.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.PACKED_MUD)
                || state.is(ModBlocks.ROT_MUD.get()) || state.is(ModBlocks.SALT_CRUST.get());
    }

    /** Air, water and soft plants can be overgrown; nothing else. */
    static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.WATER) || state.is(BlockTags.REPLACEABLE_BY_TREES)
                || state.is(Blocks.SEAGRASS) || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.LILY_PAD);
    }

    /** How many water blocks stand on top of {@code pos}, counting up from it (stops at 8). */
    static int waterDepth(WorldGenLevel level, BlockPos pos) {
        int depth = 0;
        while (depth < 8 && level.getBlockState(pos.above(depth)).is(Blocks.WATER)) {
            depth++;
        }
        return depth;
    }

    static void log(WorldGenLevel level, BlockPos pos, Direction.Axis axis) {
        level.setBlock(pos, ModBlocks.ROTWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis), 2);
    }

    static boolean logIfFree(WorldGenLevel level, BlockPos pos, Direction.Axis axis) {
        if (!canReplace(level, pos)) {
            return false;
        }
        log(level, pos, axis);
        return true;
    }

    /** A thin, ragged clump of leaves: each spot within {@code radius} only gets a leaf with {@code chance}. */
    static void clump(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, float chance) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 1, radius))) {
            int dx = pos.getX() - centre.getX();
            int dy = pos.getY() - centre.getY();
            int dz = pos.getZ() - centre.getZ();
            if (dx * dx + dz * dz + dy * dy * 2 > radius * radius + 1 || random.nextFloat() > chance) {
                continue;
            }
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.MANGROVE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, true), 2);
                if (random.nextInt(5) == 0) {
                    hangingVine(level, random, pos.below(), 2 + random.nextInt(5));
                }
            }
        }
    }

    /** Cave vines (without berries) hanging down from {@code top}, as long as there is air. */
    static void hangingVine(WorldGenLevel level, RandomSource random, BlockPos top, int length) {
        int n = 0;
        while (n < length && level.getBlockState(top.below(n)).isAir()) {
            n++;
        }
        for (int i = 0; i < n; i++) {
            BlockState state = i == n - 1
                    ? Blocks.CAVE_VINES.defaultBlockState().setValue(GrowingPlantHeadBlock.AGE, 25)
                    : Blocks.CAVE_VINES_PLANT.defaultBlockState();
            level.setBlock(top.below(i), state.setValue(CaveVines.BERRIES, false), 2);
        }
    }
}
