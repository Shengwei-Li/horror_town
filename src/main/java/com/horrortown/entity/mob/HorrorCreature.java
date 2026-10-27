package com.horrortown.entity.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** A Fog Harbor creature (not a monster) with a GeckoLib model playing its idle / walk animations. */
public abstract class HorrorCreature extends PathfinderMob implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idle;
    private final RawAnimation walk;

    protected HorrorCreature(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        this.idle = RawAnimation.begin().thenLoop("animation." + name + ".idle");
        this.walk = RawAnimation.begin().thenLoop("animation." + name + ".walk");
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4,
                state -> state.setAndContinue(state.isMoving() ? this.walk : this.idle)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
