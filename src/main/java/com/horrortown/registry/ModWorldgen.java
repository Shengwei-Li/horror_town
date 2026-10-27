package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.IslandContinents;
import com.horrortown.world.feature.DeadTreeFeature;
import com.horrortown.world.feature.DrownedCypressFeature;
import com.horrortown.world.feature.TideFilmFeature;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModWorldgen {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, HorrorTownMod.MODID);
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, HorrorTownMod.MODID);

    public static final DeferredHolder<Feature<?>, DrownedCypressFeature> DROWNED_CYPRESS =
            FEATURES.register("drowned_cypress", () -> new DrownedCypressFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, DeadTreeFeature> DEAD_TREE =
            FEATURES.register("dead_tree", () -> new DeadTreeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, TideFilmFeature> TIDE_FILM =
            FEATURES.register("tide_film", () -> new TideFilmFeature(NoneFeatureConfiguration.CODEC));

    static {
        DENSITY_FUNCTION_TYPES.register("island_continents", () -> IslandContinents.DATA_CODEC);
    }

    private ModWorldgen() {
    }
}
