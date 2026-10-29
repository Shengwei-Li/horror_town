package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 霉草垛: a stack of hay left out in a field nobody came back to harvest, gone black and soft with mould, toadstools
 * growing out of it, and a rotten fence post where the field's edge used to be.
 */
public class RottingHayFeature extends Feature<NoneFeatureConfiguration> {
    public RottingHayFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos base = context.origin();
        if (!TreeBits.isGround(level.getBlockState(base.below())) || !level.getBlockState(base).isAir()) {
            return false;
        }
        int bales = 2 + random.nextInt(4);
        BlockPos at = base;
        for (int i = 0; i < bales; i++) {
            BlockPos pos = at;
            while (!level.getBlockState(pos).isAir() && pos.getY() < base.getY() + 3) {
                pos = pos.above(); // pile them up where they would roll together
            }
            if (!level.getBlockState(pos.below()).isSolid()) {
                continue;
            }
            float rot = random.nextFloat();
            BlockState bale = rot < 0.45F ? Blocks.HAY_BLOCK.defaultBlockState()
                    : rot < 0.8F ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MUD.defaultBlockState();
            level.setBlock(pos, bale, 2);
            BlockPos top = pos.above();
            if (level.getBlockState(top).isAir()) {
                float r = random.nextFloat();
                if (r < 0.3F) {
                    level.setBlock(top, Blocks.MOSS_CARPET.defaultBlockState(), 2);
                } else if (r < 0.5F) {
                    level.setBlock(top, Blocks.BROWN_MUSHROOM.defaultBlockState(), 2);
                }
            }
            at = at.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random));
        }
        BlockPos post = base.relative(Direction.Plane.HORIZONTAL.getRandomDirection(random), 3);
        if (random.nextBoolean() && level.getBlockState(post).isAir() && level.getBlockState(post.below()).isSolid()) {
            level.setBlock(post, Blocks.DARK_OAK_FENCE.defaultBlockState(), 2);
        }
        return true;
    }
}
