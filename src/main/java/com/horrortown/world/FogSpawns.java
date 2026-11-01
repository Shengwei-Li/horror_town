package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import com.horrortown.registry.ModMobs;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * By day the island's own things keep to the dark like anything else, but when a fog bank rolls in over the coast
 * they come out in it: the drowned and the fishmen up off the mud flats, the salt husks on the salt flats, the
 * hanged in the groves. Never close by, never many, and only where the fog is thick and no lamp is lit.
 */
public final class FogSpawns {
    /** How thick the fog has to be, how often to try, how many of them at most around one player. */
    private static final float MIN_FOG = 0.5F;
    private static final int EVERY_TICKS = 200;
    private static final int MAX_NEAR = 5;
    private static final double MIN_DISTANCE = 24.0;
    private static final double MAX_DISTANCE = 44.0;
    private static final Map<String, EntityType<? extends Mob>[]> BY_BIOME = Map.of(
            "mud_flats", types(ModMobs.DROWNED_ONE.get(), ModMobs.DROWNED_ONE.get(), ModMobs.FISHMAN.get()),
            "fog_marsh", types(ModMobs.DROWNED_ONE.get(), ModMobs.PAPER_EFFIGY.get(), ModMobs.FISHMAN.get()),
            "salt_flats", types(ModMobs.SALT_HUSK.get()),
            "drowned_grove", types(ModMobs.HANGED_ONE.get(), ModMobs.DROWNED_ONE.get()),
            "brine_waste", types(ModMobs.SALT_HUSK.get(), ModMobs.HAIRLESS_DOG.get()));

    private FogSpawns() {
    }

    @SafeVarargs
    private static EntityType<? extends Mob>[] types(EntityType<? extends Mob>... types) {
        return types;
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % EVERY_TICKS != 0 || level.isNight()
                || FogWeather.get(level).level() < MIN_FOG || level.getDifficulty().getId() == 0) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (!player.isSpectator()) {
                tryNear(level, player);
            }
        }
    }

    private static void tryNear(ServerLevel level, ServerPlayer player) {
        if (level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(MAX_DISTANCE)).size() >= MAX_NEAR) {
            return;
        }
        RandomSource random = level.getRandom();
        double angle = random.nextDouble() * Mth.TWO_PI;
        double distance = MIN_DISTANCE + random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE);
        int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
        int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return;
        }
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, y, z);
        Holder<Biome> biome = level.getBiome(pos);
        String name = biome.unwrapKey().map(k -> k.location()).filter(id -> id.getNamespace().equals(HorrorTownMod.MODID))
                .map(ResourceLocation::getPath).orElse("");
        EntityType<? extends Mob>[] choices = BY_BIOME.get(name);
        if (choices == null || level.getBrightness(LightLayer.BLOCK, pos) > 7) {
            return;
        }
        // out of the water, if it comes up out of the water; otherwise standing on the ground
        boolean water = level.getFluidState(pos.below()).is(FluidTags.WATER);
        if (!water && !level.getBlockState(pos.below()).isSolid()) {
            return;
        }
        Mob mob = choices[random.nextInt(choices.length)].create(level);
        if (mob == null) {
            return;
        }
        mob.moveTo(x + 0.5, water ? y - 1 : y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
        if (!level.noCollision(mob)) {
            return;
        }
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(mob);
    }
}
