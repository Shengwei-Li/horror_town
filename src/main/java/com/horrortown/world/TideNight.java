package com.horrortown.world;

import com.horrortown.network.ModNetwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 潮汛夜: every few nights the tide comes in. A horn sounds out at sea at dusk, the fog turns sickly green, and
 * the Watcher (观潮者) rises out of the water off every stretch of coast to stare at the land until dawn.
 */
public class TideNight extends SavedData {
    private static final String NAME = "horror_town_tide";
    /** Nights between tides: at least this many days, plus up to {@link #EXTRA_DAYS} more. */
    private static final int MIN_DAYS = 2;
    private static final int EXTRA_DAYS = 2;
    private static final long DUSK = 13000L;
    private static final long DAWN = 23000L;

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
