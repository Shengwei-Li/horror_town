package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.TideMotherIdol;
import com.horrortown.entity.Townsfolk;
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
                    .sized(0.6F, 1.95F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("townsfolk"));

    public static final DeferredHolder<EntityType<?>, EntityType<TideMotherIdol>> TIDE_MOTHER = ENTITIES.register(
            "tide_mother", () -> EntityType.Builder.<TideMotherIdol>of(TideMotherIdol::new, MobCategory.MISC)
                    .sized(5.0F, 16.0F)
                    .clientTrackingRange(16)
                    .updateInterval(Integer.MAX_VALUE)
                    .fireImmune()
                    .build("tide_mother"));

    private ModEntities() {
    }
}
