package com.horrortown.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 观潮者: the Watcher in the Tide, risen out of the sea on a tide night — an enormous bald head up to its chin
 * in the water, staring, grinning, its fingers hooked over the waves. It blinks now and then; once it has you
 * its jaw sags open. It exists only on the client, as an apparition out in the fog off the coast (see
 * WatcherApparition); what it does to you happens in Sanity.
 */
public class Watcher extends Entity implements GeoEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.watcher.idle");
    private static final RawAnimation BLINK = RawAnimation.begin().thenPlay("animation.watcher.blink");
    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("animation.watcher.closed");
    private static final RawAnimation GAPE = RawAnimation.begin().thenLoop("animation.watcher.gape");

    /** Whether its mouth hangs open: set by the client while the Watcher has you. */
    private boolean gaping;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Watcher(EntityType<? extends Watcher> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        this.xo = this.getX();
        this.yo = this.getY();
        this.zo = this.getZ();
        this.yRotO = this.getYRot();
        this.tickCount++;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "idle", 0, state -> state.setAndContinue(IDLE)));
        controllers.add(new AnimationController<>(this, "mouth", 40,
                state -> state.setAndContinue(this.gaping ? GAPE : CLOSED)));
        controllers.add(new AnimationController<>(this, "blink", 0, state -> PlayState.STOP)
                .triggerableAnim("blink", BLINK));
    }

    public void setGaping(boolean gaping) {
        this.gaping = gaping;
    }

    public void blink() {
        this.triggerAnim("blink", "blink");
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
