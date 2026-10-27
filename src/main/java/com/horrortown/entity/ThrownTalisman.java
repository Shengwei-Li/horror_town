package com.horrortown.entity;

import com.horrortown.entity.mob.PaperEffigy;
import com.horrortown.registry.ModEffects;
import com.horrortown.registry.ModGear;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * 镇魂符, thrown: it sticks to what it hits and pins it where it stands for a few seconds. A paper figure goes up in
 * flames instead.
 */
public class ThrownTalisman extends ThrowableItemProjectile {
    private static final int BOUND_TICKS = 100;

    public ThrownTalisman(EntityType<? extends ThrownTalisman> type, Level level) {
        super(type, level);
    }

    public ThrownTalisman(Level level, LivingEntity thrower) {
        super(ModGear.TALISMAN_ENTITY.get(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModGear.SOUL_TALISMAN.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!(result.getEntity() instanceof LivingEntity living) || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (living instanceof PaperEffigy) {
            living.igniteForSeconds(8.0F);
            living.hurt(this.damageSources().thrown(this, this.getOwner()), 12.0F);
            level.sendParticles(ParticleTypes.FLAME, living.getX(), living.getY(1.0), living.getZ(), 30, 0.3, 0.6, 0.3, 0.05);
            this.playSound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
        } else {
            living.addEffect(new MobEffectInstance(ModEffects.BOUND, BOUND_TICKS, 0), this.getOwner());
            this.playSound(SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.4F);
        }
        level.sendParticles(ParticleTypes.ENCHANT, living.getX(), living.getY(0.8), living.getZ(), 40, 0.4, 0.6, 0.4, 0.5);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.discard();
        }
    }
}
