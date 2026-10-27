package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.town.TownPiece;
import com.horrortown.town.TownStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, HorrorTownMod.MODID);
    public static final DeferredRegister<StructurePieceType> PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, HorrorTownMod.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<TownStructure>> TOWN =
            TYPES.register("fog_town", () -> () -> TownStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TOWN_PIECE =
            PIECES.register("town_piece", () -> (StructurePieceType) TownPiece::new);
    public static final ResourceKey<Structure> TOWN_KEY = ResourceKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "fog_town"));

    private ModStructures() {
    }
}
