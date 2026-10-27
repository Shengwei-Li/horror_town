package com.horrortown.entity.boss;

import com.horrortown.entity.mob.DrownedOne;
import com.horrortown.registry.ModEffects;
import com.horrortown.registry.ModMobs;
import com.horrortown.world.Sanity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
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
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * 潮母真身: the Tide Mother's true body. Forty blocks of her stand in the shallows on a mass of tentacles, a rotting
 * robe over a bloated grey body, a grinning skull of a face wreathed in mist with two red eyes burning in it. She
 * never comes ashore: you go out to her, by boat, swimming, or from the islets near her.
 *
 * <p>She wades slowly after you, but only through water. Her arms are nearly sixty blocks long: she raises one and
 * brings it down on the water where you are (a red ring marks the spot a second before), smashing boats and
 * anything short of obsidian. Come close and her tentacles thrash. She screams, dragging everyone towards her and
 * leaving them {@link ModEffects#WATCHED}, and the drowned come up out of the water around anyone near her. Below
 * half her health she is quicker and worse.
 */
public class TideMotherTrue extends Monster implements GeoEntity {
    private static final EntityDataAccessor<Integer> RISING =
            SynchedEntityData.defineId(TideMotherTrue.class, EntityDataSerializers.INT);
    /** What she is doing: {@link #NONE}, a slam (1..6: right arm 1-3, left arm 4-6, near to far), lash, scream. */
    private static final EntityDataAccessor<Integer> ACTION =
            SynchedEntityData.defineId(TideMotherTrue.class, EntityDataSerializers.INT);
    private static final int NONE = 0;
    private static final int LASH = 7;
    private static final int SCREAM = 8;
    /** Where each slam lands, in blocks in front of her (from tools/gen_boss.py, solved against her model). */
    private static final float[] SLAM_REACH = {16.3F, 27.9F, 39.9F};
    /** How far to her right (right arm) or left (left arm) the hand comes down. */
    private static final float SLAM_SIDE = 7.5F;
    private static final double SLAM_RADIUS = 5.0;
    private static final int SLAM_IMPACT = 20;
    private static final int SLAM_TICKS = 34;
    private static final double LASH_RANGE = 16.0;
    private static final int LASH_IMPACT = 12;
    private static final int LASH_TICKS = 28;
    private static final int SCREAM_TICKS = 50;
    private static final double SCREAM_RANGE = 64.0;
    private static final int RISE_TICKS = 120;
    private static final int DEATH_TICKS = 80;
    private static final double HOME_RANGE = 48.0;
    private static final double KEEP_DISTANCE = 24.0;
    private static final double WADE_SPEED = 0.05;
    private static final int MAX_DROWNED = 14;
    /** Destroy speed at or above which a block holds against her: obsidian and harder. */
    private static final float UNBREAKABLE = 50.0F;
    private static final ParticleOptions WARNING = new DustParticleOptions(new Vector3f(0.85F, 0.08F, 0.06F), 3.0F);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = new ServerBossEvent(this.getDisplayName(),
            BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.NOTCHED_10);
    private final RawAnimation idle = RawAnimation.begin().thenLoop("animation.tide_mother_true.idle");
    private final RawAnimation walk = RawAnimation.begin().thenLoop("animation.tide_mother_true.walk");
    private final RawAnimation rise = RawAnimation.begin().thenPlay("animation.tide_mother_true.rise");
    private final RawAnimation death = RawAnimation.begin().thenPlayAndHold("animation.tide_mother_true.death");
    private final RawAnimation lash = RawAnimation.begin().thenPlay("animation.tide_mother_true.lash");
    private final RawAnimation roar = RawAnimation.begin().thenPlay("animation.tide_mother_true.roar");
    private final RawAnimation[] slams = new RawAnimation[6];

    private BlockPos home;
    private int actionTicks;
    private int cooldown = 40;
    private int screamTimer = 300;
    private int drownedTimer = 100;
    private Vec3 slamAt;
    private boolean enraged;

    public TideMotherTrue(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 1000;
        this.setPersistenceRequired();
        for (int i = 0; i < 6; i++) {
            this.slams[i] = RawAnimation.begin().thenPlayAndHold(
                    "animation.tide_mother_true.slam_" + (i < 3 ? "right_" : "left_") + i % 3);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 1200.0).add(Attributes.ARMOR, 12.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0).add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 96.0);
    }

    /** Freshly called up: she rises out of the water over a few seconds, untouchable until she is up. */
    public void emerge() {
        this.entityData.set(RISING, RISE_TICKS);
    }

    public boolean isRising() {
        return this.entityData.get(RISING) > 0;
    }

    /** For looking at her: hold her mid-slam, arm down on the water; returns where that slam lands. */
    public Vec3 pose(int action) {
        this.entityData.set(ACTION, action);
        return landing((action - 1) % 3, action <= 3);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(RISING, 0);
        builder.define(ACTION, NONE);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, null));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return this.isRising() || super.isInvulnerableTo(source);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide && this.isAlive()) {
            mist();
        }
    }

    /** Mist hangs about her face, seen from far off. */
    private void mist() {
        for (int i = 0; i < 3; i++) {
            double angle = this.random.nextDouble() * Mth.TWO_PI;
            double r = 5 + this.random.nextDouble() * 7;
            this.level().addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    this.getX() + Math.cos(angle) * r, this.getY() + 31 + this.random.nextDouble() * 10,
                    this.getZ() + Math.sin(angle) * r, 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel level = (ServerLevel) this.level();
        if (this.home == null) {
            this.home = this.blockPosition();
        }
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        int rising = this.entityData.get(RISING);
        if (rising > 0) {
            this.entityData.set(RISING, rising - 1);
            if (rising % 10 == 0) {
                burst(level, ParticleTypes.SPLASH, this.getX(), this.getY() + 4, this.getZ(), 200, 6.0, 1.0, 6.0, 0.3);
                this.playSound(SoundEvents.GENERIC_SPLASH, 4.0F, 0.3F);
            }
            return;
        }
        if (!this.enraged && this.getHealth() < this.getMaxHealth() / 2) {
            this.enraged = true;
            startAction(SCREAM);
        }
        LivingEntity target = this.getTarget();
        int action = this.entityData.get(ACTION);
        if (action != NONE) {
            act(level, action, ++this.actionTicks);
            return;
        }
        if (target == null || !target.isAlive()) {
            return;
        }
        wade(level, target);
        if (--this.drownedTimer <= 0) {
            this.drownedTimer = this.enraged ? 100 : 160;
            raiseTheDrowned(level);
        }
        if (--this.cooldown > 0) {
            return;
        }
        if (--this.screamTimer <= 0) {
            this.screamTimer = this.enraged ? 240 : 400;
            startAction(SCREAM);
        } else if (anyoneWithin(level, LASH_RANGE)) {
            startAction(LASH);
        } else {
            aimSlam(target);
        }
    }

    /** Slowly after you, but never out of the water and never far from where she rose. */
    private void wade(ServerLevel level, LivingEntity target) {
        Vec3 to = target.position().subtract(this.position()).multiply(1, 0, 1);
        turnTowards((float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F, 1.5F);
        if (to.length() < KEEP_DISTANCE) {
            return;
        }
        Vec3 step = to.normalize();
        Vec3 ahead = this.position().add(step.scale(6.0));
        BlockPos column = BlockPos.containing(ahead);
        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, column.getX(), column.getZ());
        boolean sea = level.getFluidState(new BlockPos(column.getX(), surface - 1, column.getZ())).is(FluidTags.WATER);
        if (sea && ahead.distanceTo(Vec3.atBottomCenterOf(this.home)) < HOME_RANGE) {
            double speed = WADE_SPEED * (this.enraged ? 1.4 : 1.0);
            this.setDeltaMovement(step.x * speed, this.getDeltaMovement().y, step.z * speed);
            if (this.tickCount % 40 == 0) {
                this.playSound(SoundEvents.GENERIC_SPLASH, 3.0F, 0.3F);
            }
            if (this.tickCount % 5 == 0) {
                wadeThrough(level);
            }
        }
    }

    private void turnTowards(float yaw, float maxStep) {
        float now = Mth.approachDegrees(this.getYRot(), yaw, maxStep);
        this.setYRot(now);
        this.yBodyRot = now;
        this.yHeadRot = now;
    }

    private void startAction(int action) {
        this.entityData.set(ACTION, action);
        this.actionTicks = 0;
        if (action == SCREAM) {
            this.playSound(SoundEvents.ELDER_GUARDIAN_CURSE, 5.0F, 0.35F);
            this.playSound(SoundEvents.WARDEN_ROAR, 5.0F, 0.5F);
        } else if (action == LASH) {
            this.playSound(SoundEvents.ELDER_GUARDIAN_AMBIENT, 5.0F, 0.4F);
        } else {
            this.playSound(SoundEvents.WARDEN_ATTACK_IMPACT, 4.0F, 0.4F);
        }
    }

    /** Picks the arm and the reach that bring a hand down nearest the target, and turns her so it lands on it. */
    private void aimSlam(LivingEntity target) {
        Vec3 to = target.position().subtract(this.position()).multiply(1, 0, 1);
        double distance = to.length();
        if (distance > SLAM_REACH[2] + 8) {
            return;
        }
        int reach = 0;
        for (int i = 1; i < 3; i++) {
            if (Math.abs(SLAM_REACH[i] - distance) < Math.abs(SLAM_REACH[reach] - distance)) {
                reach = i;
            }
        }
        boolean right = this.random.nextBoolean();
        // turn so that the hand (out to one side of straight ahead) comes down on the target
        float offset = (float) (Mth.atan2(SLAM_SIDE, SLAM_REACH[reach]) * Mth.RAD_TO_DEG);
        float yaw = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F + (right ? -offset : offset);
        turnTowards(yaw, 360.0F);
        this.slamAt = landing(reach, right);
        startAction(right ? 1 + reach : 4 + reach);
    }

    /** Where a slam with this reach and arm comes down, given which way she faces now. */
    private Vec3 landing(int reach, boolean right) {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        Vec3 forward = new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
        Vec3 rightSide = new Vec3(-Mth.cos(yaw), 0, -Mth.sin(yaw));
        Vec3 at = this.position().add(forward.scale(SLAM_REACH[reach])).add(rightSide.scale(right ? SLAM_SIDE : -SLAM_SIDE));
        BlockPos column = BlockPos.containing(at);
        int surface = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        return new Vec3(at.x, surface, at.z);
    }

    private void act(ServerLevel level, int action, int t) {
        if (action == SCREAM) {
            if (t == 10) {
                scream(level);
            }
            finishAfter(t, SCREAM_TICKS, 30);
        } else if (action == LASH) {
            if (t < LASH_IMPACT && t % 3 == 0) {
                level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY() + 1, this.getZ(), 60, LASH_RANGE / 2, 0.3, LASH_RANGE / 2, 0.1);
            }
            if (t == LASH_IMPACT) {
                lash(level);
            }
            finishAfter(t, LASH_TICKS, 30);
        } else {
            if (this.slamAt == null) {
                this.slamAt = landing((action - 1) % 3, action <= 3);
            }
            if (t < SLAM_IMPACT && t % 2 == 0) {
                warn(level, this.slamAt);
            }
            if (t == SLAM_IMPACT) {
                slam(level, this.slamAt);
            }
            if (t >= SLAM_TICKS) {
                this.slamAt = null;
            }
            finishAfter(t, SLAM_TICKS, this.enraged ? 20 : 35);
        }
    }

    private void finishAfter(int t, int length, int rest) {
        if (t >= length) {
            this.entityData.set(ACTION, NONE);
            this.cooldown = rest;
        }
    }

    /** A ring of red on the water where her hand is about to come down; seen from anywhere. */
    private void warn(ServerLevel level, Vec3 at) {
        for (ServerPlayer player : level.players()) {
            for (int i = 0; i < 24; i++) {
                double angle = i * Mth.TWO_PI / 24;
                level.sendParticles(player, WARNING, true, at.x + Math.cos(angle) * SLAM_RADIUS, at.y + 0.3,
                        at.z + Math.sin(angle) * SLAM_RADIUS, 1, 0, 0, 0, 0);
            }
        }
    }

    /** Her hand comes down: whatever is under it is crushed, boats are smashed, the water is thrown up. */
    private void slam(ServerLevel level, Vec3 at) {
        AABB area = new AABB(at, at).inflate(SLAM_RADIUS, 4.0, SLAM_RADIUS);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (Entity hit : level.getEntities(this, area, e -> !(e instanceof Monster))) {
            if (hit instanceof Boat boat) {
                boat.ejectPassengers();
                boat.hurt(this.damageSources().mobAttack(this), 100.0F);
            } else if (hit instanceof LivingEntity living && living.hurt(this.damageSources().mobAttack(this), damage)) {
                living.push(0, -0.6, 0);
                living.hurtMarked = true;
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 2), this);
            }
        }
        if (EventHooks.canEntityGrief(level, this)) {
            breakAround(level, BlockPos.containing(at), 3);
        }
        burst(level, ParticleTypes.SPLASH, at.x, at.y + 0.5, at.z, 400, SLAM_RADIUS, 1.0, SLAM_RADIUS, 0.6);
        burst(level, ParticleTypes.CLOUD, at.x, at.y + 1.0, at.z, 60, SLAM_RADIUS / 2, 0.5, SLAM_RADIUS / 2, 0.2);
        level.playSound(null, BlockPos.containing(at), SoundEvents.GENERIC_EXPLODE.value(), this.getSoundSource(), 5.0F, 0.4F);
    }

    /** Her tentacles thrash: everything near her is beaten, thrown back and its boat broken. */
    private void lash(ServerLevel level) {
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.75F;
        for (Entity hit : level.getEntities(this, this.getBoundingBox().inflate(LASH_RANGE, 4.0, LASH_RANGE),
                e -> !(e instanceof Monster) && e.distanceToSqr(this.getX(), e.getY(), this.getZ()) < LASH_RANGE * LASH_RANGE)) {
            if (hit instanceof Boat boat) {
                boat.ejectPassengers();
                boat.hurt(this.damageSources().mobAttack(this), 100.0F);
            } else if (hit instanceof LivingEntity living && living.hurt(this.damageSources().mobAttack(this), damage)) {
                Vec3 away = living.position().subtract(this.position()).multiply(1, 0, 1).normalize().scale(1.6);
                living.push(away.x, 0.6, away.z);
                living.hurtMarked = true;
            }
        }
        burst(level, ParticleTypes.SPLASH, this.getX(), this.getY() + 2, this.getZ(), 600, LASH_RANGE / 2, 1.0, LASH_RANGE / 2, 0.6);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 4.0F, 0.6F);
    }

    /** A scream that drags everyone near her in towards her, darkens their sight and leaves them watched. */
    private void scream(ServerLevel level) {
        for (Player player : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(SCREAM_RANGE),
                p -> !p.isCreative() && !p.isSpectator())) {
            Entity mover = player.getVehicle() != null ? player.getVehicle() : player;
            Vec3 pull = this.position().subtract(mover.position()).multiply(1, 0, 1).normalize().scale(0.9);
            mover.push(pull.x, 0.1, pull.z);
            mover.hurtMarked = true;
            player.addEffect(new MobEffectInstance(ModEffects.WATCHED, 600, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
            if (player instanceof ServerPlayer serverPlayer) {
                Sanity.set(serverPlayer, Sanity.get(serverPlayer) - 10.0F);
            }
        }
    }

    /** The drowned come up out of the water around everyone out here with her. */
    private void raiseTheDrowned(ServerLevel level) {
        AABB around = this.getBoundingBox().inflate(96.0);
        if (level.getEntitiesOfClass(DrownedOne.class, around).size() >= MAX_DROWNED) {
            return;
        }
        for (Player player : level.getEntitiesOfClass(Player.class, around, p -> !p.isCreative() && !p.isSpectator())) {
            int count = this.enraged ? 3 : 2;
            for (int i = 0; i < count; i++) {
                double angle = this.random.nextDouble() * Mth.TWO_PI;
                double distance = 7 + this.random.nextDouble() * 7;
                int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
                int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
                int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
                if (!level.getFluidState(new BlockPos(x, surface - 1, z)).is(FluidTags.WATER)) {
                    continue;
                }
                EntityType<? extends Mob> type = this.enraged && i == 0 ? ModMobs.FISHMAN.get() : ModMobs.DROWNED_ONE.get();
                Mob mob = type.create(level);
                if (mob == null) {
                    continue;
                }
                mob.moveTo(x + 0.5, surface - 1, z + 0.5, this.random.nextFloat() * 360.0F, 0.0F);
                mob.setTarget(player);
                level.addFreshEntity(mob);
                level.sendParticles(ParticleTypes.SPLASH, x + 0.5, surface, z + 0.5, 30, 0.5, 0.3, 0.5, 0.1);
                level.playSound(null, x, surface, z, SoundEvents.DROWNED_AMBIENT_WATER, this.getSoundSource(), 2.0F, 0.6F);
            }
        }
    }

    /** Particles everyone can see, however far off they are. */
    private static void burst(ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
                              double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : level.players()) {
            level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, speed);
        }
    }

    private boolean anyoneWithin(ServerLevel level, double range) {
        return !level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range, 8.0, range),
                p -> !p.isCreative() && !p.isSpectator() && p.distanceToSqr(this.getX(), p.getY(), this.getZ()) < range * range).isEmpty();
    }

    /** Islets in her way give, short of obsidian. */
    private void wadeThrough(ServerLevel level) {
        if (EventHooks.canEntityGrief(level, this)) {
            AABB box = this.getBoundingBox();
            for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY + 1, box.minZ),
                    BlockPos.containing(box.maxX, Math.min(box.maxY, box.minY + 12), box.maxZ))) {
                breakBlock(level, pos);
            }
        }
    }

    private void breakAround(ServerLevel level, BlockPos centre, int radius) {
        for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-radius, -1, -radius), centre.offset(radius, 3, radius))) {
            if (pos.distSqr(centre) <= radius * radius + 1) {
                breakBlock(level, pos);
            }
        }
    }

    private void breakBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        float hardness = state.getDestroySpeed(level, pos);
        if (state.isAir() || !state.getFluidState().isEmpty() || hardness < 0.0F || hardness >= UNBREAKABLE
                || !EventHooks.onEntityDestroyBlock(this, pos, state)) {
            return;
        }
        level.destroyBlock(pos, this.random.nextInt(6) == 0, this);
    }

    @Override
    protected void tickDeath() {
        if (++this.deathTime >= DEATH_TICKS && !this.level().isClientSide && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(RemovalReason.KILLED);
        }
        if (this.level() instanceof ServerLevel level && this.deathTime % 5 == 0) {
            burst(level, ParticleTypes.SPLASH, this.getX(), this.getY() + 3, this.getZ(), 200, 6.0, 1.0, 6.0, 0.3);
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
        if (this.home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(this.home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.enraged = tag.getBoolean("Enraged");
        NbtUtils.readBlockPos(tag, "Home").ifPresent(pos -> this.home = pos);
        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isPushable() {
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
        return 0.4F;
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
        return 6.0F;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 512 * 512;
    }

    private RawAnimation animation(AnimationState<?> state) {
        if (this.isDeadOrDying()) {
            return this.death;
        }
        if (this.isRising()) {
            return this.rise;
        }
        int action = this.entityData.get(ACTION);
        if (action >= 1 && action <= 6) {
            return this.slams[action - 1];
        }
        if (action == LASH) {
            return this.lash;
        }
        if (action == SCREAM) {
            return this.roar;
        }
        return state.isMoving() ? this.walk : this.idle;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 8, state -> state.setAndContinue(animation(state))));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
