package com.horrortown.entity.boss;

import com.horrortown.registry.ModEffects;
import com.horrortown.registry.ModMobs;
import com.horrortown.world.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 潮母真身: the Tide Mother's true body, ten blocks of bloated grey flesh in a rotting robe, a grinning skull of a
 * face behind long wet hair. Called up out of the sea by an offering at the idol.
 *
 * <p>She wades ashore through anything in her way: every block softer than obsidian gives (obsidian, crying
 * obsidian, reinforced deepslate and bedrock hold). She brings both arms down on whatever is in front of her,
 * calls the drowned up out of the water around her, and every so often screams, which drags everyone near her
 * towards her and leaves them {@link ModEffects#WATCHED}. Below half her health she is quicker and worse.
 */
public class TideMotherTrue extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> RISING =
            SynchedEntityData.defineId(TideMotherTrue.class, EntityDataSerializers.INT);
    private static final int RISE_TICKS = 80;
    private static final int SLAM_COOLDOWN = 50;
    private static final int SLAM_WINDUP = 13;
    private static final double SLAM_RADIUS = 6.0;
    private static final int SCREAM_EVERY = 240;
    private static final double SCREAM_RANGE = 32.0;
    private static final int SUMMON_EVERY = 320;
    private static final int DEATH_TICKS = 60;
    /** Destroy speed at or above which a block holds against her: obsidian and harder. */
    private static final float UNBREAKABLE = 50.0F;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.BLUE, BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(true).setCreateWorldFog(true);
    private final RawAnimation idle = RawAnimation.begin().thenLoop("animation.tide_mother_true.idle");
    private final RawAnimation walk = RawAnimation.begin().thenLoop("animation.tide_mother_true.walk");
    private final RawAnimation rise = RawAnimation.begin().thenPlay("animation.tide_mother_true.rise");
    private final RawAnimation death = RawAnimation.begin().thenPlayAndHold("animation.tide_mother_true.death");

    private int slamCooldown;
    private int slamWindup;
    private int screamTimer = SCREAM_EVERY / 2;
    private int summonTimer = SUMMON_EVERY / 2;
    private boolean enraged;

    public TideMotherTrue(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0).add(Attributes.ARMOR, 10.0)
                .add(Attributes.ATTACK_DAMAGE, 18.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 64.0)
                .add(Attributes.STEP_HEIGHT, 2.0);
    }

    /** Freshly called up: she rises out of the water over a few seconds, untouchable until she is up. */
    public void emerge() {
        this.entityData.set(RISING, RISE_TICKS);
    }

    public boolean isRising() {
        return this.entityData.get(RISING) > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RISING, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.6));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 32.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isRising();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return this.isRising() || super.isInvulnerableTo(source);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        ServerLevel level = (ServerLevel) this.level();
        int rising = this.entityData.get(RISING);
        if (rising > 0) {
            this.entityData.set(RISING, rising - 1);
            if (rising % 10 == 0) {
                level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 1, this.getZ(), 80, 2.0, 0.5, 2.0, 0.2);
                this.playSound(SoundEvents.GENERIC_SPLASH, 3.0F, 0.4F);
            }
            return;
        }
        if (!this.enraged && this.getHealth() < this.getMaxHealth() / 2) {
            this.enraged = true;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.27);
            scream(level);
        }
        if (this.tickCount % 5 == 0) {
            wadeThrough(level);
        }
        if (this.slamCooldown > 0) {
            this.slamCooldown--;
        }
        if (this.slamWindup > 0 && --this.slamWindup == 0) {
            slam(level);
        }
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }
        if (--this.screamTimer <= 0) {
            this.screamTimer = this.enraged ? SCREAM_EVERY * 2 / 3 : SCREAM_EVERY;
            scream(level);
        }
        if (--this.summonTimer <= 0) {
            this.summonTimer = this.enraged ? SUMMON_EVERY * 2 / 3 : SUMMON_EVERY;
            callTheDrowned(level, target);
        }
    }

    /** In reach she raises both arms; the blow lands a moment later (see {@link #slam}). */
    @Override
    public boolean doHurtTarget(Entity target) {
        if (this.slamCooldown > 0 || this.slamWindup > 0) {
            return false;
        }
        this.slamWindup = SLAM_WINDUP;
        this.slamCooldown = this.enraged ? SLAM_COOLDOWN * 2 / 3 : SLAM_COOLDOWN;
        this.triggerAnim("action", "slam");
        this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 2.0F, 0.5F);
        return true;
    }

    /** Both arms come down in front of her: everything there is hurt, thrown and slowed. */
    private void slam(ServerLevel level) {
        Vec3 front = this.position().add(Vec3.directionFromRotation(0.0F, this.getYRot()).scale(3.0));
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity hit : level.getEntitiesOfClass(LivingEntity.class, new AABB(front, front).inflate(SLAM_RADIUS, 3.0, SLAM_RADIUS),
                e -> e != this && !(e instanceof Monster))) {
            if (hit.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = hit.position().subtract(this.position()).multiply(1, 0, 1).normalize().scale(1.4);
                hit.push(away.x, 0.7, away.z);
                hit.hurtMarked = true;
                hit.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), this);
            }
        }
        BlockState under = level.getBlockState(BlockPos.containing(front).below());
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, under.isAir() ? Blocks.MUD.defaultBlockState() : under),
                front.x, front.y + 0.3, front.z, 160, 2.5, 0.3, 2.5, 0.4);
        level.sendParticles(ParticleTypes.SPLASH, front.x, front.y + 0.5, front.z, 120, 3.0, 0.5, 3.0, 0.3);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.5F);
    }

    /** A scream that drags everyone near her in towards her and leaves them watched. */
    private void scream(ServerLevel level) {
        this.triggerAnim("action", "roar");
        this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 4.0F, 0.4F);
        this.playSound(SoundEvents.WARDEN_ROAR, 4.0F, 0.6F);
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(SCREAM_RANGE),
                p -> !p.isCreative() && !p.isSpectator())) {
            Vec3 pull = this.position().subtract(player.position()).multiply(1, 0, 1).normalize().scale(1.1);
            player.push(pull.x, 0.3, pull.z);
            player.hurtMarked = true;
            player.addEffect(new MobEffectInstance(ModEffects.WATCHED, 600, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0, false, false));
            if (player instanceof ServerPlayer serverPlayer) {
                Sanity.set(serverPlayer, Sanity.get(serverPlayer) - 10.0F);
            }
        }
    }

    /** The drowned come up out of the ground and water around her. */
    private void callTheDrowned(ServerLevel level, LivingEntity target) {
        int count = this.enraged ? 4 : 2;
        for (int i = 0; i < count; i++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            double distance = 5 + this.random.nextDouble() * 6;
            EntityType<? extends Mob> type = this.enraged && i == 0 ? ModMobs.FISHMAN.get() : ModMobs.DROWNED_ONE.get();
            Mob mob = type.create(level);
            if (mob == null) {
                continue;
            }
            double x = this.getX() + Math.cos(angle) * distance;
            double z = this.getZ() + Math.sin(angle) * distance;
            BlockPos pos = BlockPos.containing(x, this.getY() + 4, z);
            while (pos.getY() > level.getMinBuildHeight() && level.getBlockState(pos.below()).getCollisionShape(level, pos.below()).isEmpty()) {
                pos = pos.below();
            }
            mob.moveTo(x, pos.getY(), z, this.random.nextFloat() * 360.0F, 0.0F);
            mob.setTarget(target);
            level.addFreshEntity(mob);
            level.sendParticles(ParticleTypes.SPLASH, x, pos.getY() + 0.5, z, 30, 0.5, 0.5, 0.5, 0.1);
        }
        this.playSound(SoundEvents.DROWNED_AMBIENT_WATER, 3.0F, 0.5F);
    }

    /** Anything in her way short of obsidian gives. */
    private void wadeThrough(ServerLevel level) {
        if (!EventHooks.canEntityGrief(level, this)) {
            return;
        }
        Vec3 ahead = this.getDeltaMovement().multiply(1, 0, 1);
        AABB box = this.getBoundingBox().expandTowards(ahead.normalize().scale(this.horizontalCollision ? 1.0 : 0.3))
                .deflate(0.05);
        boolean broke = false;
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY + 0.01, box.minZ),
                BlockPos.containing(box.maxX, box.maxY, box.maxZ))) {
            BlockState state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (state.isAir() || !state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty()
                    || hardness < 0.0F || hardness >= UNBREAKABLE || !EventHooks.onEntityDestroyBlock(this, pos, state)) {
                continue;
            }
            level.destroyBlock(pos, this.random.nextInt(4) == 0, this);
            broke = true;
        }
        if (broke) {
            this.playSound(SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 2.0F, 0.5F);
        }
    }

    @Override
    protected void tickDeath() {
        if (++this.deathTime >= DEATH_TICKS && !this.level().isClientSide && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
        if (this.level() instanceof ServerLevel level && this.deathTime % 5 == 0) {
            level.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 1, this.getZ(), 60, 2.0, 1.0, 2.0, 0.2);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Enraged", this.enraged);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
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
    protected boolean canRide(Entity vehicle) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ELDER_GUARDIAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ELDER_GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ELDER_GUARDIAN_DEATH;
    }

    @Override
    protected float getSoundVolume() {
        return 4.0F;
    }

    private RawAnimation animation(AnimationState<?> state) {
        if (this.isDeadOrDying()) {
            return this.death;
        }
        if (this.isRising()) {
            return this.rise;
        }
        return state.isMoving() ? this.walk : this.idle;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 6, state -> state.setAndContinue(animation(state))));
        controllers.add(new AnimationController<>(this, "action", 2, state -> PlayState.STOP)
                .triggerableAnim("slam", RawAnimation.begin().thenPlay("animation.tide_mother_true.slam"))
                .triggerableAnim("roar", RawAnimation.begin().thenPlay("animation.tide_mother_true.roar")));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
