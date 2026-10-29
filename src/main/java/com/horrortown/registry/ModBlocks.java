package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.block.BarnacleBlock;
import com.horrortown.block.RotMudBlock;
import com.horrortown.block.TideFilmBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
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

    /** 朽烂木板 and 水渍木板: the boards the town is built from, rotten, and soaked below the tide line */
    public static final DeferredBlock<Block> ROTTEN_PLANKS = BLOCKS.registerSimpleBlock("rotten_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).instrument(NoteBlockInstrument.BASS)
                    .strength(1.5F, 2.0F).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<Block> SOAKED_PLANKS = BLOCKS.registerSimpleBlock("soaked_planks",
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_GRAY).instrument(NoteBlockInstrument.BASS)
                    .strength(1.5F, 2.0F).sound(SoundType.WOOD));
    /** 浅水: the thin film of water on the low mud flats */
    public static final DeferredBlock<TideFilmBlock> TIDE_FILM = BLOCKS.registerBlock("tide_film", TideFilmBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable().noCollission().noOcclusion()
                    .instabreak().sound(SoundType.EMPTY).noLootTable().pushReaction(PushReaction.DESTROY));

    /** 藤壶 */
    public static final DeferredBlock<BarnacleBlock> BARNACLES = BLOCKS.registerBlock("barnacles", BarnacleBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.TERRACOTTA_WHITE).replaceable().noCollission()
                    .strength(0.3F).sound(SoundType.CORAL_BLOCK).pushReaction(PushReaction.DESTROY));

    private ModBlocks() {
    }
}
