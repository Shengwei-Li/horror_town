package com.horrortown.entity.mob;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * A Fog Harbor monster with a GeckoLib model: plays "animation.&lt;name&gt;.idle" or ".walk" (from
 * tools/gen_mobs.py) and lets subclasses swap in their own. Humanoid ones open wooden doors and break their way
 * through soft blocks to reach you ({@link #addHumanoidGoals}).
 */
public abstract class HorrorMonster extends Monster implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    protected final String name;
    private final RawAnimation idle;
    private final RawAnimation walk;

    protected HorrorMonster(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
        this.idle = loop("idle");
        this.walk = loop("walk");
    }

    protected RawAnimation loop(String animation) {
        return RawAnimation.begin().thenLoop("animation." + this.name + "." + animation);
    }

    protected RawAnimation once(String animation) {
        return RawAnimation.begin().thenPlay("animation." + this.name + "." + animation);
    }

    /** What to play right now; by default walking when moving, otherwise idle. */
    protected RawAnimation animation(AnimationState<?> state) {
        return state.isMoving() ? this.walk : this.idle;
    }

    /** Opens wooden doors (not iron ones) and claws through wood, glass and earth when it cannot get to you. */
    protected void addHumanoidGoals(int priority) {
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
        this.goalSelector.addGoal(priority, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(priority, new BreakSoftBlockGoal(this));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, state -> state.setAndContinue(animation(state))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
