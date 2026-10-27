package com.horrortown.entity.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;

/**
 * 水蛭鳗: a black eel-leech that flops and hops over the mud flats like a mudskipper. It hops at you, and when it
 * reaches you it latches on (rides you) and keeps biting until you knock it off.
 */
public class LeechEel extends HorrorMonster {
    private static final double LATCH_RANGE = 1.6;
    private static final int BITE_TICKS = 40;

    private int hopCooldown;

    public LeechEel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 1.0).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        LivingEntity target = this.getTarget();
        if (this.isPassenger()) {
            if (this.getVehicle() instanceof LivingEntity host && this.tickCount % BITE_TICKS == 0) {
                host.hurt(this.damageSources().mobAttack(this), 1.0F);
                this.playSound(SoundEvents.SLIME_ATTACK, 0.8F, 1.4F);
            }
            return;
        }
        if (target != null && target.isAlive() && this.distanceTo(target) < LATCH_RANGE
                && target.getPassengers().isEmpty()) {
            this.startRiding(target, true);
            return;
        }
        if (this.onGround() && --this.hopCooldown <= 0) {
            hop(target);
        }
    }

    /** A flop through the air: at the target if there is one, otherwise anywhere. */
    private void hop(LivingEntity target) {
        Vec3 direction;
        if (target != null) {
            direction = target.position().subtract(this.position()).multiply(1.0, 0.0, 1.0).normalize();
            this.hopCooldown = 15 + this.random.nextInt(20);
        } else {
            float angle = this.random.nextFloat() * Mth.TWO_PI;
            direction = new Vec3(Mth.cos(angle), 0.0, Mth.sin(angle));
            this.hopCooldown = 60 + this.random.nextInt(80);
        }
        this.setDeltaMovement(direction.x * 0.35, 0.42, direction.z * 0.35);
        this.setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F);
        this.yBodyRot = this.getYRot();
        this.triggerAnim("hop", "hop");
        this.playSound(SoundEvents.SLIME_JUMP_SMALL, 0.7F, 0.8F + this.random.nextFloat() * 0.3F);
    }

    /** Knocked off by a hit. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isPassenger() && !this.level().isClientSide) {
            this.stopRiding();
        }
        return super.hurt(source, amount);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        controllers.add(new AnimationController<>(this, "hop", 0, state -> PlayState.STOP)
                .triggerableAnim("hop", once("hop")));
    }

    @Override
    public boolean canDrownInFluidType(net.neoforged.neoforge.fluids.FluidType type) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SLIME_DEATH_SMALL;
    }
}
