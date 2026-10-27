package com.horrortown.town;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.TownRole;
import com.horrortown.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.BaseCoralWallFanBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.DripstoneThickness;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * The procedural designs of the town's buildings. Everything is damp and falling apart: missing planks, holed
 * roofs, cobwebs for nets, moss and vines; a few faded red lanterns are the only colour.
 */
final class Buildings {
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final BlockState DARK_PLANKS = Blocks.DARK_OAK_PLANKS.defaultBlockState();
    private static final BlockState SPRUCE_PLANKS = Blocks.SPRUCE_PLANKS.defaultBlockState();
    private static final BlockState BUTTON = Blocks.POLISHED_BLACKSTONE_BUTTON.defaultBlockState()
            .setValue(ButtonBlock.FACE, AttachFace.WALL);
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.defaultBlockState();
    private static final BlockState COBWEB = Blocks.COBWEB.defaultBlockState();
    private static final BlockState FENCE = Blocks.DARK_OAK_FENCE.defaultBlockState();
    private static final BlockState MOSS = Blocks.MOSS_CARPET.defaultBlockState();
    private static final BlockState RED = Blocks.RED_TERRACOTTA.defaultBlockState();
    private static final ResourceKey<LootTable> HOUSE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "chests/town_house"));
    private static final ResourceKey<LootTable> TEMPLE_LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "chests/town_temple"));
    private static final TownRole[] HOUSE_ROLES = {TownRole.FISHER, TownRole.NET_MENDER, TownRole.PORTER,
            TownRole.SALTER, TownRole.FISHER};

    private Buildings() {
    }

    static void build(TownPiece piece, Builder b) {
        switch (piece.kind) {
            case STREET -> boardwalk(piece, b, false);
            case PIER -> boardwalk(piece, b, true);
            case GATE -> gate(piece, b);
            case HOUSE -> house(piece, b);
            case WELL -> well(b);
            case MARKET -> market(piece, b);
            case WAREHOUSE -> warehouse(piece, b);
            case TEMPLE -> temple(piece, b);
        }
    }

    // ------------------------------------------------------------------ materials

    private static BlockState log(Direction.Axis axis) {
        return ModBlocks.ROTWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
    }

    /** Wall boards: mostly rotten, some still dark oak, the odd post; below the tide line they are soaked. */
    private static BlockState plank(Builder b, int x, int y, int z) {
        float r = b.rand(x, y, z, 1);
        if (y <= 2 && r < 0.8F) {
            return ModBlocks.SOAKED_PLANKS.get().defaultBlockState();
        }
        return r < 0.6F ? ModBlocks.ROTTEN_PLANKS.get().defaultBlockState() : r < 0.9F ? DARK_PLANKS
                : log(Direction.Axis.Y);
    }

    /** Deck boards: rotten and dark oak. */
    private static BlockState deck(Builder b, int x, int y, int z) {
        return b.chance(x, y, z, 9, 0.6F) ? ModBlocks.ROTTEN_PLANKS.get().defaultBlockState() : DARK_PLANKS;
    }

    /** A chair (a stair) for someone to sit and look out to sea: its back to the land. */
    private static BlockState chair(Builder b, Direction seaward) {
        return stairs(Blocks.DARK_OAK_STAIRS, b.local(seaward.getOpposite()), Half.BOTTOM);
    }

    private static BlockState stone(Builder b, int x, int y, int z) {
        float r = b.rand(x, y, z, 2);
        return r < 0.4F ? Blocks.MOSSY_STONE_BRICKS.defaultBlockState()
                : r < 0.6F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
    }

    private static BlockState stairs(Block block, Direction facing, Half half) {
        return block.defaultBlockState().setValue(StairBlock.FACING, facing).setValue(StairBlock.HALF, half);
    }

    private static BlockState hangingLantern() {
        return Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
    }

    private static BlockState candles(Builder b, int x, int y, int z) {
        return Blocks.CANDLE.defaultBlockState().setValue(CandleBlock.CANDLES, 1 + (int) (b.rand(x, y, z, 3) * 4))
                .setValue(CandleBlock.LIT, true);
    }

    private static void vine(Builder b, int x, int y, int z, Direction towardsWall, int length) {
        BlockState state = Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(towardsWall), true);
        for (int i = 0; i < length && b.get(x, y - i, z).isAir(); i++) {
            b.set(x, y - i, z, state);
        }
    }

    private static void chest(Builder b, int x, int y, int z, Direction facing, ResourceKey<LootTable> loot) {
        b.set(x, y, z, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, facing));
        if (b.inChunk(x, y, z)) {
            BlockPos pos = b.world(x, y, z);
            RandomizableContainer.setBlockEntityLootTable(b.level(), b.level().getRandom(), pos, loot);
        }
    }

    // ------------------------------------------------------------------ boardwalks

    /**
     * The street and the piers. Over dry ground the street is a trodden mud path; over mud and water it becomes a
     * plank deck on rotten posts, with railings where it runs over water. Lanterns on posts every so often.
     */
    private static void boardwalk(TownPiece piece, Builder b, boolean pier) {
        int width = piece.width;
        int length = piece.length;
        for (int z = 0; z < length; z++) {
            boolean overWater = b.get(width / 2, -2, z).is(Blocks.WATER);
            boolean onLand = !pier && !b.isOpen(width / 2, -1, z) && !b.get(width / 2, -1, z).is(BlockTags.LOGS);
            for (int x = 0; x < width; x++) {
                b.clear(x, 1, z, x, 4, z);
                if (onLand) {
                    b.set(x, 0, z, path(b, x, z));
                    continue;
                }
                boolean edge = x == 0 || x == width - 1;
                if (edge && b.chance(x, 0, z, 4, 0.04F)) {
                    continue; // a rotten plank gone
                }
                b.set(x, 0, z, deck(b, x, 0, z));
            }
            if (onLand) {
                // keep a lantern going along the path now and then
                if (z % 24 == 12) {
                    int x = (z / 12) % 2 == 0 ? 0 : width - 1;
                    b.fill(x, 1, z, x, 2, z, FENCE);
                    b.set(x, 3, z, Blocks.LANTERN.defaultBlockState());
                }
                continue;
            }
            if (z % 4 == 0) {
                b.post(0, -1, z, log(Direction.Axis.Y), 30);
                b.post(width - 1, -1, z, log(Direction.Axis.Y), 30);
            }
            if (pier || overWater) {
                for (int x : new int[]{0, width - 1}) {
                    if (!b.chance(x, 1, z, 6, 0.2F)) {
                        b.set(x, 1, z, FENCE);
                        if (b.chance(x, 2, z, 7, 0.08F)) {
                            b.set(x, 2, z, COBWEB); // a torn net left on the rail
                        }
                    }
                }
            }
            if (z % 24 == 12) {
                int x = (z / 12) % 2 == 0 ? 0 : width - 1;
                b.set(x, 1, z, FENCE);
                b.set(x, 2, z, FENCE);
                b.set(x, 3, z, Blocks.LANTERN.defaultBlockState());
            }
            // barnacles crusting the posts at the waterline
            if (z % 4 == 0 && overWater && b.chance(0, -1, z, 10, 0.5F)) {
                b.set(0, -1, z, Blocks.DEAD_TUBE_CORAL_BLOCK.defaultBlockState());
            }
        }
        // the side piers end in a row of empty chairs looking out to sea
        if (pier && piece.facing != piece.seaward) {
            for (int x = 0; x < width; x++) {
                b.set(x, 1, length - 2, chair(b, piece.seaward));
            }
        }
    }

    private static BlockState path(Builder b, int x, int z) {
        float r = b.rand(x, 0, z, 8);
        return r < 0.4F ? Blocks.PACKED_MUD.defaultBlockState()
                : r < 0.65F ? Blocks.COARSE_DIRT.defaultBlockState()
                : r < 0.82F ? Blocks.MUD.defaultBlockState()
                : r < 0.92F ? Blocks.GRAVEL.defaultBlockState()
                : DARK_PLANKS;
    }

    // ------------------------------------------------------------------ gate (牌坊)

    private static void gate(TownPiece piece, Builder b) {
        b.clear(0, 1, 0, 10, 10, 2);
        for (int x : new int[]{2, 8}) {
            b.fill(x - 1, 0, 0, x + 1, 0, 2, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            b.post(x, -1, 1, Blocks.MOSSY_COBBLESTONE.defaultBlockState(), 8);
            for (int y = 1; y <= 7; y++) {
                b.set(x, y, 1, stone(b, x, y, 1));
            }
        }
        for (int x = 1; x <= 9; x++) {
            b.set(x, 7, 1, log(Direction.Axis.X));
            if (x >= 3 && x <= 7) {
                b.set(x, 5, 1, RED);
            }
        }
        b.set(5, 6, 1, DARK_PLANKS);
        for (int x = 0; x <= 10; x++) {
            boolean end = x == 0 || x == 10;
            b.set(x, 8, 0, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, Half.BOTTOM));
            b.set(x, 8, 2, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, Half.BOTTOM));
            b.set(x, 8, 1, end ? Blocks.DEEPSLATE_TILE_WALL.defaultBlockState() : TILES);
            if (end) {
                b.set(x, 9, 1, Blocks.DEEPSLATE_TILE_WALL.defaultBlockState()); // the curled-up ends
            }
        }
        b.set(3, 6, 1, hangingLantern());
        b.set(7, 6, 1, hangingLantern());
        vine(b, 2, 6, 0, Direction.SOUTH, 3);
        vine(b, 8, 5, 2, Direction.NORTH, 4);
        // two paper figures wait outside the gate, facing whoever comes along the road
        Direction outwards = piece.facing.getOpposite();
        piece.effigy(b, 0, 0, -2, outwards);
        piece.effigy(b, 10, 0, -2, outwards);
    }

    // ------------------------------------------------------------------ houses (吊脚楼)

    private static void house(TownPiece piece, Builder street) {
        boolean collapsed = street.rand(0, 0, 0, 10) < 0.25F;
        // many have settled into the mud, a block or two below the street
        float sinking = street.rand(0, 0, 0, 14);
        Builder b = street.shifted(sinking < 0.35F ? -1 : sinking < 0.5F ? -2 : 0);
        shack(b, 7, 7, 4, collapsed);
        // inside: a barrel, a chest, candles, cobweb nets in the corners, and chairs all turned to the sea
        b.set(1, 1, 1, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        b.set(1, 2, 1, candles(b, 1, 2, 1));
        chest(b, 5, 1, 1, Direction.SOUTH, HOUSE_LOOT);
        b.set(5, 1, 3, Blocks.CAULDRON.defaultBlockState());
        b.set(2, 1, 4, chair(b, piece.seaward));
        b.set(4, 1, 4, chair(b, piece.seaward));
        if (b.chance(0, 0, 0, 15, 0.5F)) {
            b.set(3, 4, 3, hangingLantern());
        }
        b.set(1, 4, 5, COBWEB);
        b.set(5, 4, 1, COBWEB);
        if (b.chance(0, 0, 0, 16, 0.2F)) {
            piece.effigy(b, 5, 1, 7, piece.facing); // a paper figure keeping watch on the porch
        }
        if (!collapsed) {
            TownRole role = HOUSE_ROLES[(int) (b.rand(0, 0, 0, 11) * HOUSE_ROLES.length)];
            piece.spawn(b, 3, 1, 4, role);
            if (b.rand(0, 0, 0, 12) < 0.4F) {
                piece.spawn(b, 2, 1, 3, HOUSE_ROLES[(int) (b.rand(0, 0, 0, 13) * HOUSE_ROLES.length)]);
            }
        }
    }

    /**
     * A plank shack on stilts: {@code w} x {@code d} footprint, door in the middle of the front (z = d - 1) wall,
     * a porch row in front of that, a gabled roof of black tiles with its ridge running front to back.
     */
    private static void shack(Builder b, int w, int d, int wallHeight, boolean collapsed) {
        int top = wallHeight;
        int layers = (w + 1) / 2;
        b.clear(-1, 1, -1, w, top + layers + 1, d);
        // stilts
        for (int x = 0; x < w; x += (w - 1) / 2) {
            for (int z = 0; z < d; z += (d - 1) / 2) {
                b.post(x, -1, z, log(Direction.Axis.Y), 30);
            }
        }
        // floor, with a few rotten planks missing
        int door = w / 2;
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                if (x != door && b.chance(x, 0, z, 20, 0.05F)) {
                    continue;
                }
                b.set(x, 0, z, deck(b, x, 0, z));
                if (b.chance(x, 1, z, 22, 0.1F)) {
                    b.set(x, 1, z, MOSS);
                }
            }
        }
        // porch
        for (int x = 1; x < w - 1; x++) {
            b.set(x, 0, d, deck(b, x, 0, d));
        }
        b.set(1, 1, d, FENCE);
        b.set(w - 2, 1, d, FENCE);
        if (b.chance(0, 2, d, 23, 0.5F)) {
            b.set(w - 2, 2, d, COBWEB);
        }
        // walls
        int collapseX = w / 2;
        for (int y = 1; y <= top; y++) {
            for (int x = 0; x < w; x++) {
                for (int z = 0; z < d; z++) {
                    boolean wallX = x == 0 || x == w - 1;
                    boolean wallZ = z == 0 || z == d - 1;
                    if (!wallX && !wallZ) {
                        continue;
                    }
                    if (collapsed && x > collapseX && z < d / 2 && y > 1 + (int) (b.rand(x, 0, z, 24) * 2)) {
                        continue; // that corner has fallen in
                    }
                    if (wallX && wallZ) {
                        b.set(x, y, z, log(Direction.Axis.Y));
                    } else if (y > 1 && b.chance(x, y, z, 25, 0.06F)) {
                        b.set(x, y, z, AIR); // a hole in the boards
                    } else {
                        b.set(x, y, z, plank(b, x, y, z));
                    }
                }
            }
        }
        // windows: broken, some stuffed with old nets
        int[][] windows = {{w / 2, 2, 0}, {0, 2, d / 2}, {w - 1, 2, d / 2}};
        for (int[] wnd : windows) {
            b.set(wnd[0], wnd[1], wnd[2], b.chance(wnd[0], 2, wnd[2], 26, 0.3F) ? COBWEB : AIR);
        }
        // door: sometimes boarded up from outside, sometimes gone
        float doorKind = b.rand(0, 0, 0, 27);
        if (doorKind < 0.25F) {
            b.set(door, 1, d - 1, log(Direction.Axis.X));
            b.set(door, 2, d - 1, AIR);
            b.set(door, 3, d - 1, log(Direction.Axis.X));
            b.set(door, 2, d - 1, Blocks.DARK_OAK_TRAPDOOR.defaultBlockState()
                    .setValue(TrapDoorBlock.FACING, Direction.SOUTH).setValue(TrapDoorBlock.OPEN, true));
        } else if (doorKind < 0.85F) {
            BlockState doorState = Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING, Direction.SOUTH)
                    .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT);
            b.set(door, 1, d - 1, doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER));
            b.set(door, 2, d - 1, doorState.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        } else {
            b.set(door, 1, d - 1, AIR);
            b.set(door, 2, d - 1, AIR);
        }
        // barnacles clustered on the outside walls below the tide line
        for (int z = 0; z < d; z++) {
            for (int y = 0; y <= 2; y++) {
                if (b.chance(-1, y, z, 37, 0.12F) && b.get(-1, y, z).isAir()) {
                    b.set(-1, y, z, Blocks.DEAD_BRAIN_CORAL_WALL_FAN.defaultBlockState()
                            .setValue(BaseCoralWallFanBlock.FACING, Direction.WEST));
                }
                if (b.chance(w, y, z, 38, 0.12F) && b.get(w, y, z).isAir()) {
                    b.set(w, y, z, Blocks.DEAD_BRAIN_CORAL_WALL_FAN.defaultBlockState()
                            .setValue(BaseCoralWallFanBlock.FACING, Direction.EAST));
                }
            }
        }
        // now and then someone has scratched an eye into the outside of a wall
        if (b.chance(0, 0, 0, 39, 0.2F)) {
            BlockState button = BUTTON.setValue(ButtonBlock.FACING, Direction.EAST);
            int[][] eye = {{3, 2}, {3, 3}, {3, 4}, {2, 1}, {2, 3}, {2, 5}, {1, 2}, {1, 3}, {1, 4}};
            for (int[] e : eye) {
                if (b.get(w, e[0], e[1]).isAir()) {
                    b.set(w, e[0], e[1], button);
                }
            }
        }
        // tie beam to hang a lantern from
        for (int x = 0; x < w; x++) {
            b.set(x, top + 1, d / 2, log(Direction.Axis.X));
        }
        // gable ends
        for (int i = 0; i < layers - 1; i++) {
            for (int x = i; x < w - i; x++) {
                if (!(collapsed && x > collapseX)) {
                    b.set(x, top + 1 + i, 0, plank(b, x, top + 1 + i, 0));
                }
                b.set(x, top + 1 + i, d - 1, plank(b, x, top + 1 + i, d - 1));
            }
        }
        // roof
        for (int i = 0; i < layers; i++) {
            int y = top + 1 + i;
            for (int z = -1; z <= d; z++) {
                for (int side = 0; side < 2; side++) {
                    int x = side == 0 ? i - 1 : w - i;
                    if (b.chance(x, y, z, 28, 0.07F)
                            || collapsed && x >= collapseX && z < d / 2 + 1 && b.chance(x, y, z, 29, 0.85F)) {
                        continue;
                    }
                    b.set(x, y, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, side == 0 ? Direction.EAST : Direction.WEST,
                            Half.BOTTOM));
                }
            }
        }
        for (int z = -1; z <= d; z++) {
            if (!(collapsed && z < d / 2 + 1) && !b.chance(w / 2, 0, z, 30, 0.05F)) {
                b.set(w / 2, top + layers, z, Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState());
            }
        }
        // vines creeping down the outside walls
        for (int z = 1; z < d - 1; z += 2) {
            if (b.chance(-1, 0, z, 31, 0.4F)) {
                vine(b, -1, top, z, Direction.EAST, 1 + (int) (b.rand(-1, 0, z, 32) * 4));
            }
            if (b.chance(w, 0, z, 33, 0.4F)) {
                vine(b, w, top, z, Direction.WEST, 1 + (int) (b.rand(w, 0, z, 34) * 4));
            }
        }
        if (collapsed) {
            // what is left of the roof lies on the floor
            for (int x = collapseX; x < w - 1; x++) {
                for (int z = 1; z < d / 2; z++) {
                    if (b.chance(x, 1, z, 35, 0.35F)) {
                        b.set(x, 1, z, b.chance(x, 1, z, 36, 0.5F) ? Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState()
                                : stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, Half.BOTTOM));
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ well (古井)

    private static void well(Builder b) {
        int depth = 28;
        b.clear(0, 1, 0, 4, 6, 4);
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                boolean ring = x == 0 || x == 4 || z == 0 || z == 4;
                boolean shaft = !ring;
                for (int y = 0; y >= -depth; y--) {
                    if (ring) {
                        b.set(x, y, z, b.chance(x, y, z, 40, 0.5F) ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                                : Blocks.COBBLESTONE.defaultBlockState());
                    } else if (shaft) {
                        b.set(x, y, z, y <= -depth + 3 ? Blocks.WATER.defaultBlockState() : AIR);
                    }
                }
                if (ring) {
                    b.set(x, 1, z, Blocks.MOSSY_COBBLESTONE_WALL.defaultBlockState());
                }
            }
        }
        for (int x = 0; x < 5; x++) {
            b.set(x, 0, -1, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
            b.set(x, 0, 5, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        }
        b.set(0, 2, 2, FENCE);
        b.set(0, 3, 2, FENCE);
        b.set(4, 2, 2, FENCE);
        b.set(4, 3, 2, FENCE);
        for (int x = 0; x < 5; x++) {
            b.set(x, 4, 2, log(Direction.Axis.X));
        }
        b.set(2, 3, 2, Blocks.CHAIN.defaultBlockState());
        b.set(2, 2, 2, Blocks.CHAIN.defaultBlockState());
        vine(b, 1, 0, 1, Direction.WEST, 6);
        vine(b, 3, -2, 3, Direction.EAST, 8);
    }

    // ------------------------------------------------------------------ fish market (渔市)

    private static void market(TownPiece piece, Builder b) {
        int w = 9;
        int d = 7;
        b.clear(-1, 1, -1, w, 5, d);
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                b.set(x, 0, z, deck(b, x, 0, z));
            }
        }
        for (int x : new int[]{0, 4, 8}) {
            for (int z : new int[]{0, 6}) {
                b.post(x, -1, z, log(Direction.Axis.Y), 30);
                b.fill(x, 1, z, x, 3, z, log(Direction.Axis.Y));
            }
        }
        BlockState slab = Blocks.DEEPSLATE_TILE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.BOTTOM);
        for (int x = -1; x <= w; x++) {
            for (int z = -1; z <= d; z++) {
                if (!b.chance(x, 4, z, 51, 0.12F)) {
                    b.set(x, 4, z, slab);
                }
            }
        }
        // stalls: barrels, tubs of water, drying kelp, nets
        for (int x = 1; x < w - 1; x += 2) {
            b.set(x, 1, 2, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        }
        b.set(7, 1, 4, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        b.set(1, 1, 4, Blocks.DRIED_KELP_BLOCK.defaultBlockState());
        b.set(1, 2, 4, Blocks.DRIED_KELP_BLOCK.defaultBlockState());
        b.set(3, 3, 1, COBWEB);
        b.set(6, 3, 5, COBWEB);
        b.set(2, 3, 3, hangingLantern());
        b.set(6, 3, 3, hangingLantern());
        piece.spawn(b, 3, 1, 4, TownRole.FISHER);
        piece.spawn(b, 5, 1, 5, TownRole.FISHER);
    }

    // ------------------------------------------------------------------ salt warehouse (盐仓)

    private static void warehouse(TownPiece piece, Builder b) {
        shack(b, 9, 9, 5, false);
        BlockState salt = ModBlocks.SALT_CRUST.get().defaultBlockState();
        for (int x = 1; x < 8; x++) {
            for (int z = 1; z < 4; z++) {
                int height = 1 + (int) (b.rand(x, 0, z, 60) * 3);
                for (int y = 1; y <= height; y++) {
                    b.set(x, y, z, salt);
                }
            }
        }
        for (int z = 4; z < 7; z++) {
            b.set(1, 1, z, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
            b.set(7, 1, z, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.UP));
        }
        chest(b, 7, 2, 5, Direction.WEST, HOUSE_LOOT);
        b.set(4, 5, 6, hangingLantern());
        piece.spawn(b, 4, 1, 6, TownRole.SALTER);
    }

    // ------------------------------------------------------------------ temple (潮母庙)

    /**
     * The tide mother's temple, standing on stone piers out in the sea. Local z = 24 is the front step facing the
     * town; the idol fills the back half of the hall.
     */
    private static void temple(TownPiece piece, Builder b) {
        int w = 21;
        int d = 25;
        b.clear(0, 1, 0, w - 1, 26, d - 1);
        // platform on thick stone piers
        for (int x = 0; x < w; x++) {
            for (int z = 0; z < d; z++) {
                b.set(x, 0, z, stone(b, x, 0, z));
                if (x == 0 || x == w - 1 || z == 0 || z == d - 1) {
                    b.set(x, -1, z, stone(b, x, -1, z));
                }
            }
        }
        for (int px : new int[]{0, 6, 13, 19}) {
            for (int pz : new int[]{0, 8, 15, 23}) {
                for (int dx = 0; dx < 2; dx++) {
                    for (int dz = 0; dz < 2; dz++) {
                        b.post(px + dx, -2, pz + dz, Blocks.STONE_BRICKS.defaultBlockState(), 50);
                    }
                }
            }
        }
        // hall walls: stone footing, dark planks, posts; walls rise to meet the roof
        int x0 = 3, x1 = 17, z0 = 2, z1 = 20, wallTop = 9;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = x == x0 || x == x1 || z == z0 || z == z1;
                if (!wall) {
                    b.set(x, 0, z, (x + z) % 2 == 0 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : TILES);
                    continue;
                }
                int roofY = roofHeight(x, z);
                for (int y = 1; y < roofY; y++) {
                    boolean post = (x == x0 || x == x1) ? z % 4 == 2 : x % 4 == 3 || x == x1;
                    BlockState state;
                    if (y <= 2) {
                        state = b.chance(x, y, z, 70, 0.3F) ? Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                                : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
                    } else if (post) {
                        state = log(Direction.Axis.Y);
                    } else {
                        state = b.chance(x, y, z, 71, 0.03F) ? AIR : plank(b, x, y, z);
                    }
                    b.set(x, y, z, state);
                }
            }
        }
        // tall window slits down the sides
        for (int z = 6; z <= 14; z += 4) {
            for (int y = 4; y <= 6; y++) {
                b.set(x0, y, z, AIR);
                b.set(x1, y, z, AIR);
            }
        }
        // front door, three wide, with a red plaque over it
        b.fill(9, 1, z1, 11, 4, z1, AIR);
        b.fill(9, 5, z1, 11, 5, z1, log(Direction.Axis.X));
        b.set(10, 6, z1, RED);
        b.set(10, 7, z1, RED);
        // red pillars holding the front eave, and steps down to the pier
        for (int x : new int[]{5, 15}) {
            for (int y = 1; y <= 9; y++) {
                b.set(x, y, 22, RED);
            }
        }
        // a skirt of tiles around the hall below the main roof
        for (int x = x0 - 1; x <= x1 + 1; x++) {
            b.set(x, 6, z0 - 1, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, Half.BOTTOM));
            b.set(x, 6, z1 + 1, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, Half.BOTTOM));
        }
        for (int z = z0; z <= z1; z++) {
            b.set(x0 - 1, 6, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, Half.BOTTOM));
            b.set(x1 + 1, 6, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, Half.BOTTOM));
        }
        // the main hipped roof
        for (int k = 0; ; k++) {
            int ax = 1 + k, bx = w - 2 - k, az = k, bz = 22 - k;
            int y = 10 + k;
            if (bx - ax < 2) {
                b.fill(ax, y, az, bx, y, bz, TILES);
                break;
            }
            for (int x = ax; x <= bx; x++) {
                b.set(x, y, az, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.SOUTH, Half.BOTTOM));
                b.set(x, y, bz, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.NORTH, Half.BOTTOM));
            }
            for (int z = az + 1; z < bz; z++) {
                b.set(ax, y, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.EAST, Half.BOTTOM));
                b.set(bx, y, z, stairs(Blocks.DEEPSLATE_TILE_STAIRS, Direction.WEST, Half.BOTTOM));
            }
        }
        // curled-up eave corners
        for (int[] c : new int[][]{{1, 0}, {19, 0}, {1, 22}, {19, 22}}) {
            b.set(c[0], 10, c[1], TILES);
            b.set(c[0], 11, c[1], Blocks.DEEPSLATE_TILE_WALL.defaultBlockState());
        }
        for (int[] l : new int[][]{{2, 1}, {18, 1}, {2, 21}, {18, 21}, {10, 22}, {7, 22}, {13, 22}}) {
            b.set(l[0], 9, l[1], hangingLantern());
        }
        // inside: two rows of posts, candles along the walls, an offering chest
        for (int z = 6; z <= 18; z += 4) {
            b.fill(6, 1, z, 6, wallTop, z, log(Direction.Axis.Y));
            b.fill(14, 1, z, 14, wallTop, z, log(Direction.Axis.Y));
        }
        for (int z = 4; z <= 18; z += 2) {
            b.set(x0 + 1, 1, z, candles(b, x0 + 1, 1, z));
            b.set(x1 - 1, 1, z, candles(b, x1 - 1, 1, z));
        }
        chest(b, x0 + 1, 1, 18, Direction.EAST, TEMPLE_LOOT);
        idol(b);
        piece.spawn(b, 10, 1, 16, TownRole.KEEPER);
    }

    /** Height of the main roof surface over a spot (the stairs of the hipped roof). */
    private static int roofHeight(int x, int z) {
        return 10 + Math.min(Math.min(x - 1, 19 - x), Math.min(z, 22 - z));
    }

    /**
     * 潮母: the idol. A hooded, faceless thing on a black pedestal, its lower body a heap of coiling tentacles
     * that spill across the floor, long arms reaching for the worshippers, pale eyes everywhere.
     */
    private static void idol(Builder b) {
        BlockState flesh = Blocks.DARK_PRISMARINE.defaultBlockState();
        BlockState sucker = Blocks.PRISMARINE.defaultBlockState();
        BlockState eye = Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState();
        BlockState hood = Blocks.BLACK_WOOL.defaultBlockState();
        double cx = 10.0, cz = 7.0;

        // pedestal
        b.fill(6, 1, 3, 14, 1, 10, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        b.fill(7, 2, 4, 13, 2, 9, TILES);

        // tentacles coiling out over the floor and up the back wall
        int tentacles = 9;
        for (int i = 0; i < tentacles; i++) {
            double angle = Math.PI * 2 * i / tentacles + b.rand(i, 0, 0, 80) * 0.5;
            double curl = (b.rand(i, 0, 0, 81) - 0.5) * 2.0;
            double reach = 3.5 + b.rand(i, 0, 0, 82) * 3.0;
            for (double t = 0.0; t <= 1.0; t += 0.03) {
                double a = angle + curl * t * t * 2.5;
                double r = 1.5 + reach * t;
                int x = Mth.floor(cx + Math.cos(a) * r);
                int z = Mth.floor(cz + Math.sin(a) * r * 0.8);
                int y = Mth.floor(4.0 + 3.0 * Math.sin(Math.PI * t * 0.9) - 3.5 * t);
                // stay inside the hall and keep the aisle up to the offerings clear
                if (x < 4 || x > 16 || z < 3 || z > 15 || y < 1 || z > 10 && Math.abs(x - 10) <= 2) {
                    continue;
                }
                b.set(x, y, z, b.chance(x, y, z, 83, 0.06F) ? eye : b.chance(x, y, z, 84, 0.2F) ? sucker : flesh);
                if (t < 0.45) {
                    b.set(x, y + 1, z, flesh);
                }
            }
        }
        // lower body and torso: two lumpy ellipsoids
        blob(b, cx, 6.0, cz, 3.5, 3.0, 2.6, flesh, Blocks.SCULK.defaultBlockState(), 85);
        blob(b, cx, 10.5, cz, 2.6, 2.6, 2.0, flesh, sucker, 86);
        // arms reaching forward and down, ending in hooked claws
        for (int side = -1; side <= 1; side += 2) {
            double sx = cx + side * 2.8, sy = 11.5, sz = cz + 1.0;
            double hx = cx + side * 4.5, hy = 7.5, hz = cz + 5.5;
            for (double t = 0.0; t <= 1.0; t += 0.05) {
                b.set(Mth.floor(Mth.lerp(t, sx, hx)), Mth.floor(Mth.lerp(t, sy, hy)), Mth.floor(Mth.lerp(t, sz, hz)),
                        flesh);
            }
            int hxI = Mth.floor(hx), hyI = Mth.floor(hy), hzI = Mth.floor(hz);
            b.fill(hxI, hyI, hzI, hxI + side, hyI, hzI + 1, flesh);
            for (int dx = 0; dx <= 1; dx++) {
                b.set(hxI + side * dx, hyI - 1, hzI + 1, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                        .setValue(PointedDripstoneBlock.TIP_DIRECTION, Direction.DOWN)
                        .setValue(PointedDripstoneBlock.THICKNESS, DripstoneThickness.TIP));
            }
        }
        // the hood: a black dome, open at the front onto nothing but eyes
        double hy = 14.0, hr = 2.6;
        for (int x = Mth.floor(cx - hr) - 1; x <= Mth.ceil(cx + hr) + 1; x++) {
            for (int y = Mth.floor(hy - hr) - 1; y <= Mth.ceil(hy + hr) + 1; y++) {
                for (int z = Mth.floor(cz - hr) - 1; z <= Mth.ceil(cz + hr) + 2; z++) {
                    double dx = x + 0.5 - cx, dy = y + 0.5 - hy, dz = z + 0.5 - cz;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist > hr + 0.5) {
                        continue;
                    }
                    boolean front = dz > hr - 1.3 && dy < 1.2 && Math.abs(dx) < 1.8;
                    if (front) {
                        b.set(x, y, z, Blocks.BLACK_CONCRETE.defaultBlockState());
                    } else if (dist > hr - 0.7) {
                        b.set(x, y, z, hood);
                    } else {
                        b.set(x, y, z, Blocks.BLACK_CONCRETE.defaultBlockState());
                    }
                }
            }
        }
        int faceZ = Mth.floor(cz + hr);
        for (int[] e : new int[][]{{9, 14}, {11, 14}, {10, 13}, {8, 13}, {12, 13}, {10, 15}}) {
            b.set(e[0], e[1], faceZ, eye);
        }
        // offerings at its feet
        for (int x = 6; x <= 14; x++) {
            for (int z = 11; z <= 13; z++) {
                float r = b.rand(x, 1, z, 90);
                if (r < 0.25F) {
                    b.set(x, 1, z, candles(b, x, 1, z));
                } else if (r < 0.32F) {
                    b.set(x, 1, z, Blocks.BONE_BLOCK.defaultBlockState());
                } else if (r < 0.37F) {
                    b.set(x, 1, z, Blocks.SKELETON_SKULL.defaultBlockState()
                            .setValue(SkullBlock.ROTATION, (int) (b.rand(x, 1, z, 91) * 16)));
                } else if (r < 0.41F) {
                    b.set(x, 1, z, Blocks.DRIED_KELP_BLOCK.defaultBlockState());
                }
            }
        }
        b.set(7, 1, 12, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        b.set(13, 1, 12, Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        b.fill(9, 1, 14, 11, 1, 14, Blocks.POLISHED_DEEPSLATE_SLAB.defaultBlockState());
    }

    private static void blob(Builder b, double cx, double cy, double cz, double rx, double ry, double rz,
                             BlockState main, BlockState spots, int salt) {
        for (int x = Mth.floor(cx - rx); x <= Mth.ceil(cx + rx); x++) {
            for (int y = Mth.floor(cy - ry); y <= Mth.ceil(cy + ry); y++) {
                for (int z = Mth.floor(cz - rz); z <= Mth.ceil(cz + rz); z++) {
                    double dx = (x + 0.5 - cx) / rx, dy = (y + 0.5 - cy) / ry, dz = (z + 0.5 - cz) / rz;
                    double lump = (b.rand(x, y, z, salt) - 0.5) * 0.35;
                    if (dx * dx + dy * dy + dz * dz <= 1.0 + lump) {
                        b.set(x, y, z, b.chance(x, y, z, salt + 1, 0.12F) ? spots
                                : b.chance(x, y, z, salt + 2, 0.04F) ? Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState()
                                : main);
                    }
                }
            }
        }
    }
}
