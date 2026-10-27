package com.horrortown.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.util.Mth;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Continentalness for the Fog Harbor island: one landmass of about {@code radius} blocks around the world
 * centre, sinking into open sea on every side, with small scattered islets out in the sea.
 * <p>
 * {@code wobble} (vanilla continentalness noise works well) bends the coastline so it is not a circle and adds
 * some local variation; {@code islets} is a patchy noise whose peaks out at sea poke up as islets.
 * The output follows the vanilla continentalness scale: below -0.455 deep ocean, -0.19 ocean,
 * -0.11 coast, 0.03 near inland, 0.3 mid inland, above that far inland.
 */
public record IslandContinents(DensityFunction wobble, DensityFunction islets, double radius)
        implements DensityFunction {
    public static final MapCodec<IslandContinents> DATA_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("wobble").forGetter(IslandContinents::wobble),
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("islets").forGetter(IslandContinents::islets),
            Codec.doubleRange(64.0, 100_000.0).fieldOf("radius").forGetter(IslandContinents::radius)
    ).apply(instance, IslandContinents::new));
    public static final KeyDispatchDataCodec<IslandContinents> CODEC = KeyDispatchDataCodec.of(DATA_CODEC);

    /** How far (as a fraction of the radius) the wobble noise pushes the coastline in or out. */
    private static final double COAST_WOBBLE = 0.18;
    private static final double LOCAL_VARIATION = 0.08;
    /** Islet noise above this rises out of the sea; islets only appear this far out (fraction of radius). */
    private static final double ISLET_THRESHOLD = 0.5;
    /** Islet noise above this already lifts the sea floor into shallows around the islet. */
    private static final double ISLET_SHALLOWS = 0.3;
    private static final double ISLET_START = 1.2;

    @Override
    public double compute(FunctionContext context) {
        double x = context.blockX();
        double z = context.blockZ();
        double r = Math.sqrt(x * x + z * z) / this.radius;
        double wobble = this.wobble.compute(context);
        double d = r + wobble * COAST_WOBBLE;

        double value = profile(d) + wobble * LOCAL_VARIATION;
        if (r > ISLET_START) {
            double islet = this.islets.compute(context);
            if (islet > ISLET_SHALLOWS) {
                // fade islets in over the first stretch of open sea so none sit right off the coast; each one
                // stands in a patch of shallows, so the nearer ones can be waded out to
                double fade = Mth.clamp((r - ISLET_START) * 4.0, 0.0, 1.0);
                double rise = (islet - ISLET_SHALLOWS) / (ISLET_THRESHOLD - ISLET_SHALLOWS);
                value = Math.max(value, Mth.lerp(fade, value, -0.45 + rise * 0.27));
            }
        }
        return Mth.clamp(value, -1.1, 1.0);
    }

    /** Continentalness by (wobbled) distance from the centre: high inland, falling through the coast into deep sea. */
    private static double profile(double d) {
        if (d < 0.3) {
            return 0.55;
        } else if (d < 1.0) {
            return Mth.lerp((d - 0.3) / 0.7, 0.55, -0.2);
        } else if (d < 1.35) {
            // the long shallow shelf
            return Mth.lerp((d - 1.0) / 0.35, -0.2, -0.5);
        }
        return Mth.lerp(Math.min(1.0, (d - 1.35) / 0.35), -0.5, -1.0);
    }

    @Override
    public void fillArray(double[] array, ContextProvider contextProvider) {
        contextProvider.fillAllDirectly(array, this);
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new IslandContinents(this.wobble.mapAll(visitor), this.islets.mapAll(visitor), this.radius));
    }

    @Override
    public double minValue() {
        return -1.1;
    }

    @Override
    public double maxValue() {
        return 1.0;
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
