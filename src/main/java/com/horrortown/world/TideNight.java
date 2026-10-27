package com.horrortown.world;

import com.horrortown.entity.mob.DrownedOne;
import com.horrortown.network.ModNetwork;
import com.horrortown.registry.ModMobs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 潮汛夜: every few nights the tide comes in. A horn sounds out at sea at dusk, the fog turns sickly green, and
 * the Watcher (观潮者) rises out of the water off every stretch of coast to stare at the land until dawn, and the
 * drowned crawl up out of the shallows towards anyone on the shore.
 */
public class TideNight extends SavedData {
    private static final String NAME = "horror_town_tide";
    /** Nights between tides: at least this many days, plus up to {@link #EXTRA_DAYS} more. */
    private static final int MIN_DAYS = 2;
    private static final int EXTRA_DAYS = 2;
    private static final long DUSK = 13000L;
    private static final long DAWN = 23000L;
    /** How often the drowned come up, how many at most around one player, how far off in the water. */
    private static final int CRAWL_TICKS = 160;
    private static final int MAX_DROWNED = 8;
    private static final int CRAWL_MIN = 12;
    private static final int CRAWL_MAX = 28;

    private long nextDay = -1;
    private boolean active;

    public static TideNight get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(TideNight::new, TideNight::load), NAME);
    }

    private static TideNight load(CompoundTag tag, HolderLookup.Provider registries) {
        TideNight data = new TideNight();
        data.nextDay = tag.getLong("NextDay");
        data.active = tag.getBoolean("Active");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("NextDay", this.nextDay);
        tag.putBoolean("Active", this.active);
        return tag;
    }

    public boolean active() {
        return this.active;
    }

    public void tick(ServerLevel level) {
        long day = level.getDayTime() / 24000L;
        long time = level.getDayTime() % 24000L;
        if (this.nextDay < 0) {
            this.nextDay = day + MIN_DAYS;
            setDirty();
        }
        boolean night = time >= DUSK && time < DAWN;
        if (!this.active && night && day >= this.nextDay) {
            start(level);
        } else if (this.active && !night) {
            end(level, day);
        } else if (this.active && level.getGameTime() % CRAWL_TICKS == 0) {
            for (ServerPlayer player : level.players()) {
                if (!player.isSpectator() && Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ())
                        > Sanity.COAST_RADIUS) {
                    crawlTowards(level, player);
                }
            }
        }
    }

    /** One or two of the drowned come up out of the water somewhere off the shore from you. */
    private static void crawlTowards(ServerLevel level, ServerPlayer player) {
        if (level.getEntitiesOfClass(DrownedOne.class, player.getBoundingBox().inflate(48.0)).size() >= MAX_DROWNED) {
            return;
        }
        RandomSource random = level.getRandom();
        int count = 1 + random.nextInt(2);
        for (int tries = 0; tries < 12 && count > 0; tries++) {
            double angle = random.nextDouble() * Mth.TWO_PI;
            double distance = CRAWL_MIN + random.nextDouble() * (CRAWL_MAX - CRAWL_MIN);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
                continue;
            }
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (!level.getFluidState(new BlockPos(x, surface - 1, z)).is(FluidTags.WATER)) {
                continue;
            }
            DrownedOne drowned = ModMobs.DROWNED_ONE.get().create(level);
            if (drowned == null) {
                return;
            }
            drowned.moveTo(x + 0.5, surface - 1, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!player.isCreative()) {
                drowned.setTarget(player);
            }
            level.addFreshEntity(drowned);
            level.sendParticles(ParticleTypes.SPLASH, x + 0.5, surface, z + 0.5, 20, 0.5, 0.3, 0.5, 0.1);
            level.playSound(null, x, surface, z, SoundEvents.DROWNED_AMBIENT_WATER, SoundSource.HOSTILE, 1.5F, 0.6F);
            count--;
        }
    }

    public void start(ServerLevel level) {
        this.active = true;
        setDirty();
        for (ServerPlayer player : level.players()) {
            player.playNotifySound(SoundEvents.RAID_HORN.value(), SoundSource.AMBIENT, 1.0F, 0.5F);
            player.displayClientMessage(Component.translatable("message.horror_town.tide.start"), true);
        }
        PacketDistributor.sendToPlayersInDimension(level, new ModNetwork.TidePayload(true));
    }

    public void end(ServerLevel level, long day) {
        this.active = false;
        this.nextDay = day + MIN_DAYS + level.getRandom().nextInt(EXTRA_DAYS + 1);
        setDirty();
        PacketDistributor.sendToPlayersInDimension(level, new ModNetwork.TidePayload(false));
    }

    public void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new ModNetwork.TidePayload(this.active));
    }
}
