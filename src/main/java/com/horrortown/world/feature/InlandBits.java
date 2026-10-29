package com.horrortown.world.feature;

import com.horrortown.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.MultifaceBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;

/** Shared helpers for the inland features. */
final class InlandBits {
    private InlandBits() {
    }

    /**
     * How strongly the sea still marks the land here, from 1 on the coast down to a trace in the middle of the
     * island: the barnacles, the coral in the caves, all of it thins out as you go inland, but never quite stops.
     */
    static float seaMark(BlockPos pos) {
        double r = Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ());
        return (float) Math.max(0.12, Math.min(1.0, (r - 500.0) / 800.0));
    }

    /** Barnacles in the empty block at {@code pos}, stuck to the face of the block {@code towards} of it. */
    static void barnacle(WorldGenLevel level, BlockPos pos, Direction towards) {
        BlockState there = level.getBlockState(pos);
        boolean water = there.getFluidState().is(Fluids.WATER) && there.getFluidState().isSource();
        BlockState state;
        if (there.is(ModBlocks.BARNACLES.get())) {
            state = there;
        } else if (there.isAir() || water && there.getBlock() == net.minecraft.world.level.block.Blocks.WATER) {
            state = ModBlocks.BARNACLES.get().defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, water);
        } else {
            return;
        }
        BlockPos wall = pos.relative(towards);
        if (!level.getBlockState(wall).isFaceSturdy(level, wall, towards.getOpposite())) {
            return;
        }
        level.setBlock(pos, state.setValue(MultifaceBlock.getFaceProperty(towards), true), 2);
    }
}
