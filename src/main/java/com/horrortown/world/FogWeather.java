package com.horrortown.world;

import com.horrortown.network.FogPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The fog cycle of a Fog Harbor world, stored with the level. Thin haze most of the time; every so often
 * a fog bank rolls in off the sea and sits over the land for a few minutes. Banks are likelier at night.
 */
public class FogWeather extends SavedData {
    private static final String NAME = "horror_town_fog";
    private static final float DAY_BANK_CHANCE = 0.35F;
    private static final float NIGHT_BANK_CHANCE = 0.6F;

    private float level;
    private int ticksLeft;

    public static FogWeather get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(FogWeather::new, FogWeather::load), NAME);
    }

    private static FogWeather load(CompoundTag tag, HolderLookup.Provider registries) {
        FogWeather data = new FogWeather();
        data.level = tag.getFloat("Level");
        data.ticksLeft = tag.getInt("TicksLeft");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putFloat("Level", this.level);
        tag.putInt("TicksLeft", this.ticksLeft);
        return tag;
    }

    public void tick(ServerLevel world) {
        if (--this.ticksLeft > 0) {
            if (this.ticksLeft % 200 == 0) {
                setDirty();
            }
            return;
        }
        RandomSource random = world.getRandom();
        float bankChance = world.isNight() ? NIGHT_BANK_CHANCE : DAY_BANK_CHANCE;
        if (random.nextFloat() < bankChance) {
            set(world, 0.55F + random.nextFloat() * 0.45F, Mth.nextInt(random, 3, 8) * 1200);
        } else {
            set(world, random.nextFloat() * 0.15F, Mth.nextInt(random, 4, 12) * 1200);
        }
    }

    /** Sets the fog to {@code level} for {@code ticks}, then the cycle picks again. */
    public void set(ServerLevel world, float level, int ticks) {
        this.level = Mth.clamp(level, 0.0F, 1.0F);
        this.ticksLeft = ticks;
        setDirty();
        PacketDistributor.sendToPlayersInDimension(world, new FogPayload(this.level));
    }

    public void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new FogPayload(this.level));
    }
}
