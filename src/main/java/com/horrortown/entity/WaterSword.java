package com.horrortown.entity;

import com.horrortown.item.TideStaffItem;
import com.horrortown.registry.ModGear;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 水剑: a blade of sea water drawn out of the air by the Tide Mother's staff. While the staff is held up it hangs
 * in an arc behind its wielder's shoulders; let go and every one of them flies at what the wielder is looking at,
 * bending a little towards the nearest thing alive, cutting through up to three of them before it bursts.
 */
public class WaterSword extends Projectile {
    public static final int MAX_SWORDS = 5;
    private static final EntityDataAccessor<Boolean> HOVERING =
            SynchedEntityData.defineId(WaterSword.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> SLOT =
            SynchedEntityData.defineId(WaterSword.class, EntityDataSerializers.INT);
    private static final float DAMAGE = 7.0F;
    private static final int PIERCE = 3;
    private static final int LIFETIME = 60;
    private static final double SPEED = 2.2;
    private static final double SPEED_UNDERWATER = 3.2;
    private static final double HOMING_RANGE = 5.0;
    private static final double HOMING = 0.18;

    private final Set<Integer> struck = new HashSet<>();
    private int flown;

    public WaterSword(EntityType<? extends WaterSword> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public WaterSword(Level level, Player owner, int slot) {
        this(ModGear.WATER_SWORD_ENTITY.get(), level);
        this.setOwner(owner);
        this.entityData.set(SLOT, slot);
        Vec3 at = hoverPosition(owner, slot);
        this.moveTo(at.x, at.y, at.z, -owner.getYRot(), -owner.getXRot());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HOVERING, true);
        builder.define(SLOT, 0);
    }

    public boolean hovering() {
        return this.entityData.get(HOVERING);
    }

    /** Where a sword in this slot hangs: fanned out in an arc behind and above the shoulders. */
    private static Vec3 hoverPosition(Entity owner, int slot) {
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 right = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        double a = Math.toRadians(-64 + 32 * slot);
        return owner.getEyePosition().subtract(forward.scale(0.9)).add(right.scale(Math.sin(a) * 1.5))
                .add(0, Math.cos(a) * 1.1 - 0.1, 0);
    }

    /** Let go of: off towards {@code target}. */
    public void launch(Vec3 target, boolean underwater) {
        this.entityData.set(HOVERING, false);
        Vec3 to = target.subtract(this.position()).normalize().scale(underwater ? SPEED_UNDERWATER : SPEED);
        this.setDeltaMovement(to);
        face(to);
        this.playSound(SoundEvents.TRIDENT_THROW.value(), 0.8F, 1.4F + this.random.nextFloat() * 0.3F);
    }

    private void face(Vec3 motion) {
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    @Override
    public void tick() {
        super.tick();
        Entity owner = this.getOwner();
        if (hovering()) {
            if (!(owner instanceof Player player) || !player.isAlive() || !(player.getUseItem().getItem() instanceof TideStaffItem)) {
                if (!this.level().isClientSide) {
                    burst();
                }
                return;
            }
            Vec3 at = hoverPosition(player, this.entityData.get(SLOT));
            this.setPos(at.x, at.y, at.z);
            this.setYRot(-player.getYRot()); // projectiles turn the other way round from players
            this.setXRot(-player.getXRot());
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
            if (this.level().isClientSide && this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.FALLING_WATER, at.x, at.y, at.z, 0, 0, 0);
            }
            return;
        }
        if (++this.flown > LIFETIME) {
            if (!this.level().isClientSide) {
                burst();
            }
            return;
        }
        Vec3 motion = this.getDeltaMovement();
        LivingEntity prey = nearestPrey(owner);
        if (prey != null) {
            Vec3 to = prey.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(motion.length());
            motion = motion.lerp(to, HOMING);
            this.setDeltaMovement(motion);
        }
        Vec3 from = this.position();
        Vec3 next = from.add(motion);
        if (!this.level().isClientSide) {
            BlockHitResult wall = this.level().clip(new ClipContext(from, next, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, this));
            if (wall.getType() != HitResult.Type.MISS) {
                next = wall.getLocation();
            }
            EntityHitResult hit;
            Vec3 start = from;
            while ((hit = ProjectileUtil.getEntityHitResult(this.level(), this, start, next,
                    this.getBoundingBox().expandTowards(motion).inflate(1.0), this::canHitEntity)) != null) {
                strike(hit.getEntity());
                if (this.struck.size() >= PIERCE) {
                    burst();
                    return;
                }
                start = hit.getLocation();
            }
            if (wall.getType() != HitResult.Type.MISS) {
                this.setPos(next.x, next.y, next.z);
                burst();
                return;
            }
        } else {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(this.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SPLASH,
                        from.x + motion.x * i * 0.5, from.y + motion.y * i * 0.5, from.z + motion.z * i * 0.5, 0, 0, 0);
            }
        }
        face(motion);
        this.setPos(next.x, next.y, next.z);
    }

    private LivingEntity nearestPrey(Entity owner) {
        Vec3 ahead = this.position().add(this.getDeltaMovement().normalize().scale(HOMING_RANGE));
        return this.level().getEntitiesOfClass(LivingEntity.class, new AABB(ahead, ahead).inflate(HOMING_RANGE),
                        e -> e.isAlive() && e != owner && (e instanceof Enemy) && !this.struck.contains(e.getId()))
                .stream().min((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this))).orElse(null);
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target) && !this.struck.contains(target.getId()) && !(target instanceof WaterSword);
    }

    private void strike(Entity target) {
        this.struck.add(target.getId());
        Entity owner = this.getOwner();
        target.hurt(this.damageSources().indirectMagic(this, owner == null ? this : owner), DAMAGE);
        if (target instanceof LivingEntity living) {
            Vec3 push = this.getDeltaMovement().multiply(1, 0, 1).normalize().scale(0.6);
            living.push(push.x, 0.15, push.z);
            living.hurtMarked = true;
        }
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY(0.6), target.getZ(), 24,
                    0.3, 0.3, 0.3, 0.2);
        }
        this.playSound(SoundEvents.TRIDENT_HIT, 0.8F, 1.3F);
    }

    /** It loses its shape and falls as water. */
    private void burst() {
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY(), this.getZ(), 30, 0.4, 0.4, 0.4, 0.15);
            server.sendParticles(ParticleTypes.FALLING_WATER, this.getX(), this.getY(), this.getZ(), 10, 0.3, 0.2, 0.3, 0);
        }
        this.playSound(SoundEvents.GENERIC_SPLASH, 0.5F, 1.6F);
        this.discard();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }
}
