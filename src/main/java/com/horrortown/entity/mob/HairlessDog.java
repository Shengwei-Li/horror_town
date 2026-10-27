package com.horrortown.entity.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * 镇上的狗: a pale, hairless dog, all ribs. By day it follows you at a distance, stopping when you stop, always
 * watching. After dark, alone, it comes for you.
 */
public class HairlessDog extends HorrorMonster {
    private static final double KEEP_AWAY = 9.0;
    private static final double FOLLOW_FROM = 16.0;

    public HairlessDog(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 4.0).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3, true));
        this.goalSelector.addGoal(4, new StalkGoal());
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.8));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> this.level().isNight()));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide && !this.level().isNight() && this.getTarget() != null
                && this.getLastHurtByMob() != this.getTarget()) {
            this.setTarget(null); // daylight: back to watching
        }
    }

    /** By day: keep the nearest player in sight, never closer than {@link #KEEP_AWAY}. */
    private class StalkGoal extends Goal {
        private Player player;

        StalkGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (HairlessDog.this.getTarget() != null) {
                return false;
            }
            this.player = HairlessDog.this.level().getNearestPlayer(HairlessDog.this, 32.0);
            return this.player != null && !this.player.isSpectator();
        }

        @Override
        public void tick() {
            HairlessDog.this.getLookControl().setLookAt(this.player, 30.0F, 30.0F);
            double distance = HairlessDog.this.distanceTo(this.player);
            if (distance > FOLLOW_FROM) {
                HairlessDog.this.getNavigation().moveTo(this.player, 0.9);
            } else if (distance < KEEP_AWAY) {
                HairlessDog.this.getNavigation().stop();
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.level().isNight() ? SoundEvents.WOLF_GROWL : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WOLF_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WOLF_DEATH;
    }
}
