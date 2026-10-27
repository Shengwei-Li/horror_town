package com.horrortown.town;

import com.horrortown.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * 雾澳镇: the one town of a Fog Harbor world. Every chunk is a candidate (the structure set uses a spacing of one),
 * but only the chunk holding the {@link TownSite} centre starts it.
 */
public class TownStructure extends Structure {
    public static final MapCodec<TownStructure> CODEC = simpleCodec(TownStructure::new);

    public TownStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return TownSite.get(context.chunkGenerator(), context.randomState(), context.heightAccessor(), context.seed())
                .filter(site -> new ChunkPos(site.centre()).equals(context.chunkPos()))
                .map(site -> new GenerationStub(site.centre(),
                        pieces -> TownLayout.addPieces(pieces, site, context.seed())));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TOWN.get();
    }

    /** Whether {@code pos} is inside the temple of the town. */
    public static boolean inTemple(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos,
                holder -> holder.is(ModStructures.TOWN_KEY));
        if (!start.isValid()) {
            return false;
        }
        for (StructurePiece piece : start.getPieces()) {
            if (piece instanceof TownPiece town && town.kind() == TownPiece.Kind.TEMPLE
                    && piece.getBoundingBox().isInside(pos)) {
                return true;
            }
        }
        return false;
    }
}
