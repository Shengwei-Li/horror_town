package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.TideMotherIdol;
import com.horrortown.entity.Townsfolk;
import com.horrortown.entity.Watcher;
import com.horrortown.entity.boss.TideMotherTrue;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HorrorTownMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Townsfolk>> TOWNSFOLK = ENTITIES.register("townsfolk",
            () -> EntityType.Builder.of(Townsfolk::new, MobCategory.MISC)
                    .sized(0.6F, 2.4F)
                    .eyeHeight(2.15F)
                    .clientTrackingRange(10)
                    .build("townsfolk"));

    public static final DeferredHolder<EntityType<?>, EntityType<TideMotherIdol>> TIDE_MOTHER = ENTITIES.register(
            "tide_mother", () -> EntityType.Builder.<TideMotherIdol>of(TideMotherIdol::new, MobCategory.MISC)
                    .sized(5.0F, 16.0F)
                    .clientTrackingRange(16)
                    .updateInterval(Integer.MAX_VALUE)
                    .fireImmune()
                    .build("tide_mother"));

    /** Client-side only: the apparition at sea on tide nights. Never saved, never sent. */
    public static final DeferredHolder<EntityType<?>, EntityType<Watcher>> WATCHER = ENTITIES.register("watcher",
            () -> EntityType.Builder.<Watcher>of(Watcher::new, MobCategory.MISC)
                    .sized(10.0F, 30.0F)
                    .noSave()
                    .noSummon()
                    .clientTrackingRange(0)
                    .build("watcher"));

    /** 潮母真身: the boss, called up out of the sea by an offering at the idol. */
    public static final DeferredHolder<EntityType<?>, EntityType<TideMotherTrue>> TIDE_MOTHER_TRUE = ENTITIES.register(
            "tide_mother_true", () -> EntityType.Builder.<TideMotherTrue>of(TideMotherTrue::new, MobCategory.MONSTER)
                    .sized(3.5F, 10.0F)
                    .eyeHeight(9.0F)
                    .clientTrackingRange(16)
                    .fireImmune()
                    .build("tide_mother_true"));

    private ModEntities() {
    }
}
