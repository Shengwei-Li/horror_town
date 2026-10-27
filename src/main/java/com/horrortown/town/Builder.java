package com.horrortown.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Places blocks for one town piece in the piece's own frame: local +z is the piece's facing, +x is to its
 * right as seen from behind, y is up from the deck. Blocks outside the chunk being generated are skipped, so a
 * piece can be built chunk by chunk; all randomness is hashed from the position, so every chunk agrees.
 */
public final class Builder {
    private final WorldGenLevel level;
    private final BoundingBox chunkBox;
    private final BlockPos origin;
    private final Rotation rotation;
    private final long seed;

    public Builder(WorldGenLevel level, BoundingBox chunkBox, BlockPos origin, Direction facing, long seed) {
        this.level = level;
        this.chunkBox = chunkBox;
        this.origin = origin;
        this.rotation = rotationFor(facing);
        this.seed = seed;
    }

    /** The rotation that turns a design facing south (+z) to face {@code facing}. */
    public static Rotation rotationFor(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos origin, Direction facing, int x, int y, int z) {
        return origin.offset(new BlockPos(x, y, z).rotate(rotationFor(facing)));
    }

    /** The same frame, moved {@code dy} blocks up (negative: down), e.g. for a house sunk into the mud. */
    public Builder shifted(int dy) {
        return new Builder(this.level, this.chunkBox, this.origin.above(dy), facingOf(this.rotation), this.seed);
    }

    /** A world direction as seen from this frame (the inverse of {@link #dir}). */
    public Direction local(Direction world) {
        return world.getAxis().isVertical() ? world : inverse(this.rotation).rotate(world);
    }

    private static Rotation inverse(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    public BlockPos world(int x, int y, int z) {
        return toWorld(this.origin, facingOf(this.rotation), x, y, z);
    }

    private static Direction facingOf(Rotation rotation) {
        return rotation.rotate(Direction.SOUTH);
    }

    /** A local direction (designed facing south) turned into the world. */
    public Direction dir(Direction local) {
        return local.getAxis().isVertical() ? local : this.rotation.rotate(local);
    }

    public boolean inChunk(int x, int y, int z) {
        return this.chunkBox.isInside(world(x, y, z));
    }

    public void set(int x, int y, int z, BlockState state) {
        BlockPos pos = world(x, y, z);
        if (this.chunkBox.isInside(pos)) {
            this.level.setBlock(pos, state.rotate(this.rotation), 2);
        }
    }

    public BlockState get(int x, int y, int z) {
        return this.level.getBlockState(world(x, y, z));
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    /** Clears everything natural (terrain, plants, water) out of a box, so the building is not buried. */
    public void clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
    }

    /** True if nothing solid is at this spot: air, water or a soft plant. */
    public boolean isOpen(int x, int y, int z) {
        BlockState state = get(x, y, z);
        return state.isAir() || state.is(Blocks.WATER) || state.canBeReplaced() || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.LOGS) || state.is(Blocks.CAVE_VINES) || state.is(Blocks.CAVE_VINES_PLANT);
    }

    /** A post from {@code top} down to the ground (or sea floor), at most {@code max} blocks long. */
    public void post(int x, int top, int z, BlockState state, int max) {
        for (int y = top; y > top - max && isOpen(x, y, z); y--) {
            set(x, y, z, state);
        }
    }

    /** A stable pseudo-random number in [0, 1) for this spot and purpose. */
    public float rand(int x, int y, int z, int salt) {
        long h = Mth.getSeed(x * 31 + salt, y * 17 - salt, z * 13 + salt * 7) ^ this.seed * 0x9E3779B97F4A7C15L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 40) / (float) (1L << 24);
    }

    public boolean chance(int x, int y, int z, int salt, float p) {
        return rand(x, y, z, salt) < p;
    }

    public WorldGenLevel level() {
        return this.level;
    }

    public BoundingBox chunkBox() {
        return this.chunkBox;
    }
}
