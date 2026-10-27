package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HorrorTownMod.MODID);

    /**
     * 被注视: the Watcher has noticed you. While it lasts your sanity drains, every monster nearby comes for you,
     * and more of them crawl out of the dark. What it does lives in {@link com.horrortown.world.Sanity}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> WATCHED = EFFECTS.register("watched",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9AB0A0) {
            });

    private ModEffects() {
    }
}
