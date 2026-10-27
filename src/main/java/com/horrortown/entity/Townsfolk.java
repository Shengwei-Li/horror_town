package com.horrortown.entity;

import com.horrortown.town.TownStructure;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.EnumSet;

/**
 * 镇民: the people of the fog town. They work the docks by day, shut themselves in at night and stand at the
 * window staring out to sea. They trade for copper coins and say odd things. As the days go by they change:
 * pale and glassy-eyed at first, then gills and scales, until they hardly speak at all ({@link #stage()}).
 * Walk into their temple and every one of them stops to stare at you.
 */
public class Townsfolk extends PathfinderMob implements Merchant, GeoEntity {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final EntityDataAccessor<Integer> ROLE =
            SynchedEntityData.defineId(Townsfolk.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> STARING =
            SynchedEntityData.defineId(Townsfolk.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.townsfolk.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.townsfolk.walk");
    /** Days into the world when they turn: stage 1 (gills, scales) and stage 2 (fish-eyed, mute). */
    public static final long STAGE_1_DAY = 10;
    public static final long STAGE_2_DAY = 30;
    private static final int DAY_ROAM = 44;
    private static final int LINES_PER_STAGE = 6;
    private static final int KEEPER_LINES = 3;

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private int offersStage = -1;
    private long restockedDay = -1;
    @Nullable
    private BlockPos home;
    @Nullable
    private BlockPos townCentre;
    private Direction seaward = Direction.NORTH;
    @Nullable
    private Player stareTarget;
    private int stareTicks;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public Townsfolk(EntityType<? extends Townsfolk> type, Level level) {
        super(type, level);
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.5)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    /** Set when the town is generated: what they do, where they sleep, and where the town and the sea are. */
    public void setup(TownRole role, BlockPos home, BlockPos townCentre, Direction seaward) {
        this.entityData.set(ROLE, role.ordinal());
        this.home = home;
        this.townCentre = townCentre;
        this.seaward = seaward;
        this.setPersistenceRequired();
    }

    public TownRole role() {
        return TownRole.byId(this.entityData.get(ROLE));
    }

    /** How far the town has turned: 0 pale, 1 gilled and scaled, 2 fish-eyed and mute. */
    public int stage() {
        long day = this.level().getDayTime() / 24000L;
        return day >= STAGE_2_DAY ? 2 : day >= STAGE_1_DAY ? 1 : 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ROLE, 0);
        builder.define(STARING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FaceCustomerGoal());
        this.goalSelector.addGoal(2, new StareAtTrespasserGoal());
        this.goalSelector.addGoal(3, new GoHomeAtNightGoal());
        this.goalSelector.addGoal(4, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new MoveTowardsRestrictionGoal(this, 0.5));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.45));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F, 0.25F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.horror_town.townsfolk." + this.role().getSerializedName());
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.townCentre != null && !this.isNight()) {
            this.restrictTo(this.townCentre, DAY_ROAM);
        }
        if (this.stareTicks > 0) {
            this.stareTicks--;
        }
        this.entityData.set(STARING, this.stareTicks > 0 || this.tradingPlayer != null);
        if (this.tickCount % 20 == 0 && this.level() instanceof ServerLevel level) {
            Player player = level.getNearestPlayer(this, 48.0);
            if (player != null && !player.isSpectator() && TownStructure.inTemple(level, player.blockPosition())) {
                this.stareTarget = player;
                this.stareTicks = 60;
            }
            long day = level.getDayTime() / 24000L;
            if (day != this.restockedDay && this.offers != null && this.tradingPlayer == null) {
                this.restockedDay = day;
                this.offers.forEach(MerchantOffer::resetUses);
            }
        }
    }

    private boolean isNight() {
        long time = this.level().getDayTime() % 24000L;
        return time >= 12500L && time < 23500L;
    }

    /** Staring at a trespasser or a customer: the head tilts over (see TownsfolkRenderer). */
    public boolean isStaring() {
        return this.entityData.get(STARING);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "move", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    // ------------------------------------------------------------------ talking and trading

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isAlive() || this.tradingPlayer != null || hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide) {
            player.sendSystemMessage(Component.translatable("townsfolk.horror_town.says", this.getDisplayName(),
                    this.pickLine()));
            if (this.stage() < 2 || this.role() == TownRole.KEEPER) {
                MerchantOffers offers = this.getOffers();
                if (!offers.isEmpty()) {
                    this.setTradingPlayer(player);
                    this.openTradingScreen(player, this.getDisplayName(), 1);
                }
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private Component pickLine() {
        if (this.role() == TownRole.KEEPER) {
            return Component.translatable("townsfolk.horror_town.keeper." + this.random.nextInt(KEEPER_LINES));
        }
        return Component.translatable("townsfolk.horror_town.line." + this.stage() + "." + this.random.nextInt(LINES_PER_STAGE));
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.level().isClientSide) {
            throw new IllegalStateException("Cannot load townsfolk offers on the client");
        }
        int stage = this.stage();
        if (this.offers == null || this.offersStage != stage) {
            this.offers = TownTrades.offers(this.role(), stage);
            this.offersStage = stage;
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        this.playSound(SoundEvents.VILLAGER_YES, 0.6F, 0.6F);
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.VILLAGER_YES;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        this.tradingPlayer = null;
    }

    // ------------------------------------------------------------------ sounds: they are quiet, then they gurgle

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return this.stage() >= 2 ? SoundEvents.DROWNED_AMBIENT : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.stage() >= 2 ? SoundEvents.DROWNED_HURT : SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.stage() >= 2 ? SoundEvents.DROWNED_DEATH : SoundEvents.PLAYER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F + this.random.nextFloat() * 0.1F;
    }

    // ------------------------------------------------------------------ saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Role", this.entityData.get(ROLE));
        if (this.home != null) {
            tag.putLong("Home", this.home.asLong());
        }
        if (this.townCentre != null) {
            tag.putLong("TownCentre", this.townCentre.asLong());
        }
        tag.putInt("Seaward", this.seaward.get2DDataValue());
        if (this.offers != null) {
            tag.put("Offers", MerchantOffers.CODEC.encodeStart(
                    this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers).getOrThrow());
            tag.putInt("OffersStage", this.offersStage);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(ROLE, tag.getInt("Role"));
        this.home = tag.contains("Home") ? BlockPos.of(tag.getLong("Home")) : null;
        this.townCentre = tag.contains("TownCentre") ? BlockPos.of(tag.getLong("TownCentre")) : null;
        this.seaward = Direction.from2DDataValue(tag.getInt("Seaward"));
        if (tag.contains("Offers")) {
            MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Offers"))
                    .resultOrPartial(Util.prefix("Failed to load townsfolk offers: ", LOGGER::warn))
                    .ifPresent(offers -> this.offers = offers);
            this.offersStage = tag.getInt("OffersStage");
        }
    }

    // ------------------------------------------------------------------ goals

    /** Stands still and looks at whoever is trading with them. */
    private class FaceCustomerGoal extends Goal {
        FaceCustomerGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Townsfolk.this.tradingPlayer != null && Townsfolk.this.isAlive();
        }

        @Override
        public void start() {
            Townsfolk.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (Townsfolk.this.tradingPlayer != null) {
                Townsfolk.this.getLookControl().setLookAt(Townsfolk.this.tradingPlayer, 30.0F, 30.0F);
            }
        }
    }

    /** Someone is in the temple: drop everything and stare. */
    private class StareAtTrespasserGoal extends Goal {
        StareAtTrespasserGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Townsfolk.this.stareTicks > 0 && Townsfolk.this.stareTarget != null
                    && Townsfolk.this.stareTarget.isAlive();
        }

        @Override
        public void start() {
            Townsfolk.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            if (Townsfolk.this.stareTarget != null) {
                Townsfolk.this.getLookControl().setLookAt(Townsfolk.this.stareTarget, 90.0F, 90.0F);
            }
        }
    }

    /** At night they go home, shut the door, and stand looking out towards the sea until morning. */
    private class GoHomeAtNightGoal extends Goal {
        GoHomeAtNightGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Townsfolk.this.home != null && Townsfolk.this.isNight();
        }

        @Override
        public void start() {
            Townsfolk.this.restrictTo(Townsfolk.this.home, 2);
        }

        @Override
        public void tick() {
            BlockPos home = Townsfolk.this.home;
            if (home == null) {
                return;
            }
            if (Townsfolk.this.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > 2.25) {
                if (Townsfolk.this.getNavigation().isDone()) {
                    Townsfolk.this.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.5);
                }
            } else {
                Townsfolk.this.getNavigation().stop();
                BlockPos sea = home.relative(Townsfolk.this.seaward, 20);
                Townsfolk.this.getLookControl().setLookAt(sea.getX() + 0.5, home.getY() + 1.6, sea.getZ() + 0.5);
            }
        }

        @Override
        public void stop() {
            if (Townsfolk.this.townCentre != null) {
                Townsfolk.this.restrictTo(Townsfolk.this.townCentre, DAY_ROAM);
            }
        }
    }
}
