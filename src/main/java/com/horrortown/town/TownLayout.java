package com.horrortown.town;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;

/**
 * The plan of 雾澳镇, in the town's frame: z runs from the land gate (negative) out to sea (positive),
 * x across the street (positive x is on the left when looking out to sea), the origin is where the street meets
 * the waterfront.
 *
 * <pre>
 *                        [ temple ]          z  61..85
 *                            ‖ pier          z  21..60
 *          ═══ side pier ════╬════ side pier ═══   z 35..37
 *    [market]  ‖  [warehouse]                z  12..20
 *    [house]   ‖  [house]                    z   0, -10
 *    [well]    ‖  [house]                    z -22
 *    [house]   ‖  [house]                    z -36, -46
 *            [gate]                          z -57
 * </pre>
 */
final class TownLayout {
    private static final int STREET_HALF = 2;
    private static final int[] HOUSE_ROWS = {0, -10, -36, -46};

    private TownLayout() {
    }

    static void addPieces(StructurePiecesBuilder pieces, TownSite site, long seed) {
        Plan plan = new Plan(pieces, site, seed);
        Direction sea = site.seaward();
        Direction land = sea.getOpposite();
        Direction plusX = sea.getCounterClockWise();
        Direction minusX = plusX.getOpposite();

        plan.add(TownPiece.Kind.STREET, 0, -55, sea, 5, 76, 20, 8, 2);
        plan.add(TownPiece.Kind.GATE, 0, -58, sea, 11, 3, 6, 12, 5);
        plan.add(TownPiece.Kind.PIER, 0, 21, sea, 3, 40, 30, 6, 1);
        plan.add(TownPiece.Kind.PIER, -2, 36, minusX, 3, 17, 30, 6, 1);
        plan.add(TownPiece.Kind.PIER, 2, 36, plusX, 3, 17, 30, 6, 1);

        // houses face the street; each is placed by the middle of its front porch
        int i = 0;
        for (int z : HOUSE_ROWS) {
            plan.add(TownPiece.Kind.HOUSE, -STREET_HALF - 1, z, plusX, 7, 8, 20, 12, 3, 7, 100 + i++);
            plan.add(TownPiece.Kind.HOUSE, STREET_HALF + 1, z, minusX, 7, 8, 20, 12, 3, 7, 100 + i++);
        }
        plan.add(TownPiece.Kind.HOUSE, STREET_HALF + 1, -22, minusX, 7, 8, 20, 12, 3, 7, 100 + i);
        plan.add(TownPiece.Kind.WELL, -STREET_HALF - 1, -22, plusX, 5, 5, 40, 8, 2, 4, 200);
        plan.add(TownPiece.Kind.MARKET, -STREET_HALF - 1, 16, plusX, 9, 7, 20, 8, 4, 6, 300);
        plan.add(TownPiece.Kind.WAREHOUSE, STREET_HALF + 1, 16, minusX, 9, 10, 20, 14, 4, 9, 400);
        // the temple faces back towards the town; its front step meets the end of the pier
        plan.add(TownPiece.Kind.TEMPLE, 0, 61, land, 21, 25, 45, 28, 10, 24, 500);
    }

    /** Turns town-frame positions into pieces. */
    private record Plan(StructurePiecesBuilder pieces, TownSite site, long seed) {
        /** A piece whose local (width/2, 0, 0) — the middle of its back/start edge — is at town (x, z). */
        void add(TownPiece.Kind kind, int x, int z, Direction facing, int width, int length, int below, int above,
                 int salt) {
            add(kind, x, z, facing, width, length, below, above, width / 2, 0, salt);
        }

        /** A piece whose local (anchorX, 0, anchorZ) is at town (x, z). */
        void add(TownPiece.Kind kind, int x, int z, Direction facing, int width, int length, int below, int above,
                 int anchorX, int anchorZ, int salt) {
            BlockPos anchor = Builder.toWorld(this.site.centre(), this.site.seaward(), x, 0, z);
            BlockPos origin = anchor.subtract(Builder.toWorld(BlockPos.ZERO, facing, anchorX, 0, anchorZ));
            this.pieces.addPiece(new TownPiece(kind, origin, facing, width, length, below, above,
                    this.seed * 31 + salt, this.site));
        }
    }
}
