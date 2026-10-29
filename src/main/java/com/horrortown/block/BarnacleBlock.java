package com.horrortown.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.GlowLichenBlock;

/**
 * 藤壶: barnacles crusted over whatever they grow on, any face of it, in or out of water. Inland they grow on the
 * trunks of trees and the walls of caves far from any sea, as if the tide had once stood over the whole island.
 */
public class BarnacleBlock extends GlowLichenBlock {
    public static final MapCodec<GlowLichenBlock> CODEC = simpleCodec(BarnacleBlock::new);

    public BarnacleBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GlowLichenBlock> codec() {
        return CODEC;
    }
}
