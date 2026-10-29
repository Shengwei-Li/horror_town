package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 渗海: in the caves under the island, far from the sea, patches where the rock has turned to dead coral, with
 * barnacles over it and dead coral fans on the walls: the sea has been in here, or is still coming through.
 * Placed at a cave air spot; works over the rock within a few blocks of it. Rarer the further from the coast.
 */
public class CaveSeepFeature extends Feature<NoneFeatureConfiguration> {
    private static final Block[] CORAL = {Blocks.DEAD_BRAIN_CORAL_BLOCK, Blocks.DEAD_TUBE_CORAL_BLOCK,
            Blocks.DEAD_HORN_CORAL_BLOCK, Blocks.TUFF};
    private static final Block[] FANS = {Blocks.DEAD_BRAIN_CORAL_WALL_FAN, Blocks.DEAD_TUBE_CORAL_WALL_FAN,
            Blocks.DEAD_FIRE_CORAL_WALL_FAN};
    private static final int RADIUS = 4;

    public CaveSeepFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos centre = context.origin();
        if (!level.getBlockState(centre).isAir() || random.nextFloat() > InlandBits.seaMark(centre)) {
            return false;
        }
        boolean any = false;
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-RADIUS, -RADIUS, -RADIUS), centre.offset(RADIUS, RADIUS, RADIUS))) {
            if (pos.distSqr(centre) > RADIUS * RADIUS || !level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos open = pos.relative(d);
                if (!level.getBlockState(open).isAir()) {
                    continue;
                }
                // rock facing the cave: some of it turned to coral, and things growing on it
                if (random.nextFloat() < 0.5F) {
                    level.setBlock(pos, CORAL[random.nextInt(CORAL.length)].defaultBlockState(), 2);
                }
                float r = random.nextFloat();
                if (r < 0.3F) {
                    InlandBits.barnacle(level, open, d.getOpposite());
                } else if (r < 0.4F && d.getAxis().isHorizontal()) {
                    BlockState fan = FANS[random.nextInt(FANS.length)].defaultBlockState()
                            .setValue(BaseCoralWallFanBlock.FACING, d).setValue(BaseCoralWallFanBlock.WATERLOGGED, false);
                    level.setBlock(open, fan, 2);
                }
                any = true;
                break;
            }
        }
        return any;
    }
}
