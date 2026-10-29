package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.IslandContinents;
import com.horrortown.world.feature.BarnacleGrowthFeature;
import com.horrortown.world.feature.CaveSeepFeature;
import com.horrortown.world.feature.DeadTreeFeature;
import com.horrortown.world.feature.DrownedCypressFeature;
import com.horrortown.world.feature.RottingHayFeature;
import com.horrortown.world.feature.SeaStelaFeature;
import com.horrortown.world.feature.StandingPoolFeature;
import com.horrortown.world.feature.TideBoneFeature;
import com.horrortown.world.feature.TideFilmFeature;
import com.horrortown.world.feature.WreckedBoatFeature;
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

    public static final DeferredHolder<Feature<?>, TideBoneFeature> TIDE_BONES =
            FEATURES.register("tide_bones", () -> new TideBoneFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, BarnacleGrowthFeature> BARNACLE_TRUNKS =
            FEATURES.register("barnacle_trunks", () -> new BarnacleGrowthFeature(NoneFeatureConfiguration.CODEC, false));
    public static final DeferredHolder<Feature<?>, BarnacleGrowthFeature> BARNACLE_ROCKS =
            FEATURES.register("barnacle_rocks", () -> new BarnacleGrowthFeature(NoneFeatureConfiguration.CODEC, true));
    public static final DeferredHolder<Feature<?>, WreckedBoatFeature> WRECKED_BOAT =
            FEATURES.register("wrecked_boat", () -> new WreckedBoatFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, SeaStelaFeature> SEA_STELA =
            FEATURES.register("sea_stela", () -> new SeaStelaFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, RottingHayFeature> ROTTING_HAY =
            FEATURES.register("rotting_hay", () -> new RottingHayFeature(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, CaveSeepFeature> CAVE_SEEP =
            FEATURES.register("cave_seep", () -> new CaveSeepFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, StandingPoolFeature> STANDING_POOL =
            FEATURES.register("standing_pool", () -> new StandingPoolFeature(NoneFeatureConfiguration.CODEC));

    static {
        DENSITY_FUNCTION_TYPES.register("island_continents", () -> IslandContinents.DATA_CODEC);
    }

    private ModWorldgen() {
    }
}
