package com.horrortown.world.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * 镇海碑: a weathered stone stele standing alone in the waste, put up long ago to hold the sea back. A board nailed
 * to its face still carries the warning in red.
 */
public class SeaStelaFeature extends Feature<NoneFeatureConfiguration> {
    private static final String[][] WARNINGS = {
            {"镇海", "", "潮至勿近", ""},
            {"", "海不过此", "", ""},
            {"潮母在上", "", "莫回头", ""},
            {"此处", "曾是海", "", ""},
    };

    public SeaStelaFeature(Codec<NoneFeatureConfiguration> codec) {
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
        level.setBlock(base.below(), Blocks.MOSSY_STONE_BRICKS.defaultBlockState(), 2);
        int height = 3 + random.nextInt(2);
        for (int y = 0; y < height; y++) {
            BlockState stone = y == height - 1 ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                    : random.nextInt(3) == 0 ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
            level.setBlock(base.above(y), stone, 2);
        }
        level.setBlock(base.above(height), Blocks.STONE_BRICK_SLAB.defaultBlockState(), 2);
        // the warning, facing out the way the sea used to be (any way will do: the sea was everywhere)
        Direction face = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos board = base.above(1).relative(face);
        if (level.getBlockState(board).isAir() || level.getBlockState(board).canBeReplaced()) {
            level.setBlock(board, Blocks.DARK_OAK_WALL_SIGN.defaultBlockState().setValue(WallSignBlock.FACING, face), 2);
            if (level.getBlockEntity(board) instanceof SignBlockEntity sign) {
                String[] lines = WARNINGS[random.nextInt(WARNINGS.length)];
                SignText text = new SignText().setColor(DyeColor.RED).setHasGlowingText(random.nextInt(3) == 0);
                for (int i = 0; i < 4; i++) {
                    text = text.setMessage(i, Component.literal(lines[i]));
                }
                // written as saved data: during worldgen the sign is not in a level yet, so its setters cannot be used
                CompoundTag tag = new CompoundTag();
                tag.put("front_text", SignText.DIRECT_CODEC.encodeStart(NbtOps.INSTANCE, text).getOrThrow());
                tag.putBoolean("is_waxed", true);
                sign.loadWithComponents(tag, level.registryAccess());
            }
        }
        // a few stubs of candle burnt down at its foot
        BlockPos candle = base.relative(face.getClockWise());
        if (random.nextBoolean() && level.getBlockState(candle).isAir()) {
            level.setBlock(candle, Blocks.RED_CANDLE.defaultBlockState(), 2);
        }
        return true;
    }
}
