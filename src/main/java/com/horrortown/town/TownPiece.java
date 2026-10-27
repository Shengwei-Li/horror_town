package com.horrortown.town;

import com.horrortown.entity.TideMotherIdol;
import com.horrortown.entity.TownRole;
import com.horrortown.entity.Townsfolk;
import com.horrortown.registry.ModEntities;
import com.horrortown.registry.ModStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * One building (or stretch of boardwalk) of the town. It is built procedurally by {@link Buildings} in its own
 * frame: {@code width} across (local x), {@code length} along its facing (local z), from {@code below} blocks
 * under the deck (for posts and pilings) to {@code above} blocks over it.
 */
public class TownPiece extends StructurePiece {
    public enum Kind {
        STREET, PIER, GATE, HOUSE, WELL, MARKET, WAREHOUSE, TEMPLE
    }

    private static final int PAPER_WHITE = 0xE6E1D3;

    final Kind kind;
    final BlockPos origin;
    final Direction facing;
    final int width;
    final int length;
    final long seed;
    /** The town this piece belongs to, for the townsfolk who live in it. */
    final BlockPos townCentre;
    final Direction seaward;

    public TownPiece(Kind kind, BlockPos origin, Direction facing, int width, int length, int below, int above,
                     long seed, TownSite site) {
        super(ModStructures.TOWN_PIECE.get(), 0, box(origin, facing, width, length, below, above));
        this.kind = kind;
        this.origin = origin;
        this.facing = facing;
        this.width = width;
        this.length = length;
        this.seed = seed;
        this.townCentre = site.centre();
        this.seaward = site.seaward();
    }

    public TownPiece(StructurePieceSerializationContext context, CompoundTag tag) {
        super(ModStructures.TOWN_PIECE.get(), tag);
        this.kind = Kind.valueOf(tag.getString("Kind"));
        this.origin = BlockPos.of(tag.getLong("Origin"));
        this.facing = Direction.from2DDataValue(tag.getInt("Facing"));
        this.width = tag.getInt("Width");
        this.length = tag.getInt("Length");
        this.seed = tag.getLong("Seed");
        this.townCentre = BlockPos.of(tag.getLong("TownCentre"));
        this.seaward = Direction.from2DDataValue(tag.getInt("Seaward"));
    }

    private static BoundingBox box(BlockPos origin, Direction facing, int width, int length, int below, int above) {
        return BoundingBox.fromCorners(Builder.toWorld(origin, facing, -1, -below, -1),
                Builder.toWorld(origin, facing, width, above, length));
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putString("Kind", this.kind.name());
        tag.putLong("Origin", this.origin.asLong());
        tag.putInt("Facing", this.facing.get2DDataValue());
        tag.putInt("Width", this.width);
        tag.putInt("Length", this.length);
        tag.putLong("Seed", this.seed);
        tag.putLong("TownCentre", this.townCentre.asLong());
        tag.putInt("Seaward", this.seaward.get2DDataValue());
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator,
                            RandomSource random, BoundingBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {
        Buildings.build(this, new Builder(level, chunkBox, this.origin, this.facing, this.seed));
    }

    /**
     * 纸人: a paper effigy (an armour stand dressed in white) standing at a local spot, turned to face
     * {@code facing} (a world direction).
     */
    void effigy(Builder b, int x, int y, int z, Direction facing) {
        if (!b.inChunk(x, y, z)) {
            return;
        }
        WorldGenLevel level = b.level();
        BlockPos pos = b.world(x, y, z);
        ArmorStand stand = new ArmorStand(level.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setYRot(facing.toYRot());
        stand.setYHeadRot(facing.toYRot());
        stand.setYBodyRot(facing.toYRot());
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack paper = new ItemStack(slot == EquipmentSlot.CHEST ? Items.LEATHER_CHESTPLATE
                    : slot == EquipmentSlot.LEGS ? Items.LEATHER_LEGGINGS : Items.LEATHER_BOOTS);
            paper.set(DataComponents.DYED_COLOR, new DyedItemColor(PAPER_WHITE, false));
            stand.setItemSlot(slot, paper);
        }
        stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.SKELETON_SKULL));
        level.addFreshEntity(stand);
    }

    /** Stands the idol on its pedestal at a local spot, facing {@code facing} (a world direction). */
    void idol(Builder b, int x, int y, int z, Direction facing) {
        if (!b.inChunk(x, y, z)) {
            return;
        }
        WorldGenLevel level = b.level();
        TideMotherIdol idol = ModEntities.TIDE_MOTHER.get().create(level.getLevel());
        if (idol == null) {
            return;
        }
        BlockPos pos = b.world(x, y, z);
        idol.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, facing.toYRot(), 0.0F);
        idol.setYHeadRot(facing.toYRot());
        level.addFreshEntity(idol);
    }

    /** Puts a townsfolk at a local spot of this piece, if that spot is in the chunk being generated. */
    void spawn(Builder b, int x, int y, int z, TownRole role) {
        if (!b.inChunk(x, y, z)) {
            return;
        }
        WorldGenLevel level = b.level();
        Townsfolk folk = ModEntities.TOWNSFOLK.get().create(level.getLevel());
        if (folk == null) {
            return;
        }
        BlockPos pos = b.world(x, y, z);
        folk.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, b.rand(x, y, z, 99) * 360.0F, 0.0F);
        folk.setup(role, pos, this.townCentre, this.seaward);
        folk.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.STRUCTURE, null);
        level.addFreshEntityWithPassengers(folk);
    }
}
