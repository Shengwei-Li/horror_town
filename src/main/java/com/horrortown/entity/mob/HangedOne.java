package com.horrortown.entity.mob;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;

/**
 * 吊者: hangs head-down from the branches of the drowned groves, a noose still round its broken neck, long arms
 * dangling. Walk underneath and it drops on you.
 */
public class HangedOne extends HorrorMonster {
    private static final EntityDataAccessor<Boolean> HANGING =
            SynchedEntityData.defineId(HangedOne.class, EntityDataSerializers.BOOLEAN);
    private static final double DROP_RANGE = 3.0;

    private final RawAnimation hang = loop("hang");

    public HangedOne(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 22.0).add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.ATTACK_DAMAGE, 5.0).add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HANGING, false);
    }

    public boolean isHanging() {
        return this.entityData.get(HANGING);
    }

    private void setHanging(boolean hanging) {
        this.entityData.set(HANGING, hanging);
        this.setNoGravity(hanging);
    }

    /** Spawns up in the branches if there are any overhead, hanging from them. */
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType type,
                                        @Nullable SpawnGroupData data) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, type, data);
        BlockPos feet = this.blockPosition();
        for (int dy = 4; dy <= 14; dy++) {
            BlockState above = level.getBlockState(feet.above(dy));
            if (!above.isAir() && above.isFaceSturdy(level, feet.above(dy), net.minecraft.core.Direction.DOWN)
                    || above.is(net.minecraft.tags.BlockTags.LEAVES) || above.is(net.minecraft.tags.BlockTags.LOGS)) {
                double top = feet.getY() + dy;
                if (level.noCollision(this, this.getBoundingBox().move(0, top - this.getBbHeight() - this.getY(), 0))) {
                    this.setPos(this.getX(), top - this.getBbHeight(), this.getZ());
                    setHanging(true);
                }
                break;
            }
        }
        return result;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        addHumanoidGoals(3);
        this.goalSelector.addGoal(6, new RandomStrollGoal(this, 0.7));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
                target -> !this.isHanging()));
    }

    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || this.isHanging();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide || !this.isHanging()) {
            return;
        }
        this.setDeltaMovement(0.0, 0.0, 0.0);
        for (Player player : this.level().players()) {
            if (player.isSpectator() || player.isCreative() || player.getY() > this.getY()) {
                continue;
            }
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            if (dx * dx + dz * dz < DROP_RANGE * DROP_RANGE && this.getY() - player.getY() < 14.0) {
                setHanging(false);
                this.setTarget(player);
                this.playSound(SoundEvents.LEASH_KNOT_BREAK, 1.0F, 0.6F);
                break;
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.isHanging() && !this.level().isClientSide) {
            setHanging(false);
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected RawAnimation animation(AnimationState<?> state) {
        return this.isHanging() ? this.hang : super.animation(state);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Hanging", this.isHanging());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setHanging(tag.getBoolean("Hanging"));
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return !this.isHanging() && super.removeWhenFarAway(distance);
    }
}
