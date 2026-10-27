package com.horrortown.entity.mob;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 泥人: six blocks of mud bent nearly double. It waits in the flats as nothing but a low mound; come close and
 * the mound swells, bulging bigger and bigger, until it bursts and the thing heaves itself up out of the mud. Its
 * grip drags you down so you can barely move. Sinks back into the mud when it loses you.
 */
public class MudLurker extends HorrorMonster {
    private static final EntityDataAccessor<Integer> STATE =
            SynchedEntityData.defineId(MudLurker.class, EntityDataSerializers.INT);
    private static final int BURIED = 0;
    private static final int SWELLING = 1;
    private static final int UP = 2;
    private static final double WAKE_RANGE = 7.0;
    private static final int SWELL_TICKS = 50;
    private static final int REBURY_TICKS = 300;
    private static final EntityDimensions BURIED_SIZE = EntityDimensions.scalable(2.0F, 1.0F);

    private final RawAnimation buried = loop("buried");
    private final RawAnimation swell = RawAnimation.begin().thenPlayAndHold("animation.mud_lurker.swell");
    private int idleTicks;
    private int swellTicks;

    public MudLurker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 80.0).add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.ATTACK_DAMAGE, 10.0).add(Attributes.FOLLOW_RANGE, 24.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, BURIED);
    }

    /** Still down in the mud, waiting or swelling. */
    public boolean isUnderground() {
        return this.entityData.get(STATE) != UP;
    }

    private void setState(int state) {
        this.entityData.set(STATE, state);
        this.refreshDimensions();
    }

    /** The mound bursts and it comes up. */
    private void burst() {
        setState(UP);
        this.triggerAnim("rise", "rise");
        this.playSound(SoundEvents.MUD_BREAK, 2.0F, 0.4F);
        this.playSound(SoundEvents.SLIME_SQUISH, 2.0F, 0.5F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                    this.getX(), this.getY() + 0.6, this.getZ(), 120, 1.0, 0.4, 1.0, 0.3);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, false));
        addHumanoidGoals(3);
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isUnderground();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide) {
            return;
        }
        int state = this.entityData.get(STATE);
        if (state == BURIED) {
            Player near = this.level().getNearestPlayer(this, WAKE_RANGE);
            if (near != null && !near.isCreative() && !near.isSpectator()) {
                this.swellTicks = 0;
                this.setTarget(near);
                setState(SWELLING);
            }
        } else if (state == SWELLING) {
            if (++this.swellTicks % 10 == 0) {
                this.playSound(SoundEvents.MUD_STEP, 1.5F, 0.4F + this.swellTicks / (float) SWELL_TICKS * 0.4F);
                if (this.level() instanceof ServerLevel server) {
                    server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MUD.defaultBlockState()),
                            this.getX(), this.getY() + 0.5, this.getZ(), 8, 0.8, 0.2, 0.8, 0.05);
                }
            }
            if (this.swellTicks >= SWELL_TICKS) {
                burst();
            }
        } else if (this.getTarget() == null && this.onGround()) {
            if (++this.idleTicks > REBURY_TICKS) {
                this.idleTicks = 0;
                this.playSound(SoundEvents.MUD_PLACE, 1.5F, 0.4F);
                setState(BURIED);
            }
        } else {
            this.idleTicks = 0;
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isUnderground() && !this.level().isClientSide) {
            burst();
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), this);
        }
        return hit;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return this.isUnderground() ? BURIED_SIZE : super.getDefaultDimensions(pose);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (STATE.equals(key)) {
            this.refreshDimensions();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "rise", 0, state -> PlayState.STOP)
                .triggerableAnim("rise", once("rise")));
    }

    @Override
    protected RawAnimation animation(AnimationState<?> state) {
        int now = this.entityData.get(STATE);
        return now == BURIED ? this.buried : now == SWELLING ? this.swell : super.animation(state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Buried", this.isUnderground());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(STATE, tag.getBoolean("Buried") ? BURIED : UP);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.MUD_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.MUD_BREAK;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.isUnderground() && super.removeWhenFarAway(distance);
    }
}
