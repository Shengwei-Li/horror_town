package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/** The Fog Harbor world type: an overworld with its own dimension type, so the sky and fog only change there. */
public final class FogHarbor {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "fog_harbor");
    public static final ResourceKey<DimensionType> DIMENSION_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE, ID);

    private FogHarbor() {
    }

    public static boolean is(Level level) {
        return level.dimensionTypeRegistration().is(DIMENSION_TYPE);
    }
}
