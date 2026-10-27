package com.horrortown.entity.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 纸人: a paper effigy with painted red cheeks. It never moves while anyone is looking at it. Look away and it is
 * closer. Paper burns: fire tears it apart.
 */
public class PaperEffigy extends HorrorMonster {
    private static final double SEEN_RANGE = 48.0;
    private static final double SEEN_CONE = 0.55;

    private boolean seen;

    public PaperEffigy(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 6.0).add(Attributes.FOLLOW_RANGE, 40.0);
    }

    @Override
    protected void registerGoals() {
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, false));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    public void tick() {
        if (!this.level().isClientSide) {
            this.seen = isSeen();
            if (this.seen) {
                this.getNavigation().stop();
                this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
            }
        }
        super.tick();
    }

    /** Is anyone looking this way, with nothing in between? */
    private boolean isSeen() {
        for (Player player : this.level().players()) {
            if (player.isSpectator() || player.distanceToSqr(this) > SEEN_RANGE * SEEN_RANGE) {
                continue;
            }
            Vec3 to = this.getEyePosition().subtract(player.getEyePosition()).normalize();
            if (player.getViewVector(1.0F).dot(to) > SEEN_CONE && player.hasLineOfSight(this)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.seen;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, source.is(DamageTypeTags.IS_FIRE) ? amount * 3.0F : amount);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BOOK_PAGE_TURN;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BOOK_PUT;
    }
}
