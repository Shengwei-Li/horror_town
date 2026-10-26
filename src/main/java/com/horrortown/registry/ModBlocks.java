package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.block.RotMudBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(HorrorTownMod.MODID);

    /** 腐泥 */
    public static final DeferredBlock<RotMudBlock> ROT_MUD = BLOCKS.registerBlock("rot_mud", RotMudBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_BLACK).strength(0.5F).sound(SoundType.MUD)
                    .speedFactor(0.8F).isValidSpawn((state, level, pos, type) -> false));
    /** 盐壳 */
    public static final DeferredBlock<Block> SALT_CRUST = BLOCKS.registerSimpleBlock("salt_crust",
            BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6F).sound(SoundType.CALCITE));
    /** 朽木: grey, waterlogged wood of the drowned trees */
    public static final DeferredBlock<RotatedPillarBlock> ROTWOOD_LOG = BLOCKS.registerBlock("rotwood_log",
            RotatedPillarBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASS)
                    .strength(2.0F).sound(SoundType.WOOD).ignitedByLava());

    private ModBlocks() {
    }
}
