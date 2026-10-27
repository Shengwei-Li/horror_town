package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import com.horrortown.network.ModNetwork;
import com.horrortown.registry.ModAttachments;
import com.horrortown.registry.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 理智: sanity, 0..100, only in a Fog Harbor world. It drains in the open dark at night and fast while the
 * Watcher has its eye on you; it comes back by lamplight indoors, with sleep, and with a meal. Low sanity brings
 * whispers and phantom footsteps (client side), and here: more monsters, and at the bottom, darkness.
 * <p>
 * On a tide night, standing under the open sky on the coast lets the Watcher notice you: after
 * {@link #NOTICE_SECONDS} you are {@link ModEffects#WATCHED}, and while that lasts every monster around comes
 * for you and more keep crawling out of the dark. Get under a roof, or far enough inland, and it loses you.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID)
public final class Sanity {
    public static final float MAX = 100.0F;
    /** Further from the island centre than this you are in sight of the sea, and of the Watcher. */
    public static final double COAST_RADIUS = 1300.0;
    private static final int NOTICE_SECONDS = 8;
    private static final int WATCHED_TICKS = 2400;
    private static final double AGGRO_RANGE = 40.0;

    private static final Map<UUID, Integer> EXPOSURE = new HashMap<>();
    private static final Map<UUID, Float> LAST_SENT = new HashMap<>();

    private Sanity() {
    }

    public static float get(Player player) {
        return player.getData(ModAttachments.SANITY);
    }

    public static void set(ServerPlayer player, float value) {
        player.setData(ModAttachments.SANITY, Mth.clamp(value, 0.0F, MAX));
        sync(player, true);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0) {
            return;
        }
        ServerLevel level = player.serverLevel();
        if (!FogHarbor.is(level) || player.isCreative() || player.isSpectator()) {
            return;
        }
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        boolean underSky = level.canSeeSky(eyes);
        boolean night = level.isNight();
        int lamplight = level.getBrightness(LightLayer.BLOCK, eyes);
        boolean onCoast = Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ()) > COAST_RADIUS;
        boolean tide = TideNight.get(level).active();

        // the Watcher noticing you
        int exposure = EXPOSURE.getOrDefault(player.getUUID(), 0);
        exposure = tide && underSky && onCoast ? exposure + 1 : Math.max(0, exposure - 2);
        if (exposure >= NOTICE_SECONDS) {
            exposure = NOTICE_SECONDS;
            if (!player.hasEffect(ModEffects.WATCHED)) {
                player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 1.0F, 0.6F);
                player.displayClientMessage(Component.translatable("message.horror_town.watched"), true);
            }
            player.addEffect(new MobEffectInstance(ModEffects.WATCHED, WATCHED_TICKS, 0, false, true, true));
        }
        EXPOSURE.put(player.getUUID(), exposure);
        boolean watched = player.hasEffect(ModEffects.WATCHED);

        float sanity = get(player);
        if (watched) {
            sanity -= 0.8F;
        } else if (night && underSky && lamplight < 8) {
            sanity -= 0.05F;
        }
        if (!underSky && lamplight >= 10) {
            sanity += 0.3F;
        }
        player.setData(ModAttachments.SANITY, Mth.clamp(sanity, 0.0F, MAX));
        sync(player, false);

        RandomSource random = player.getRandom();
        if (watched) {
            if (player.tickCount % 40 == 0) {
                for (Monster monster : level.getEntitiesOfClass(Monster.class, player.getBoundingBox().inflate(AGGRO_RANGE))) {
                    if (monster.getTarget() == null) {
                        monster.setTarget(player);
                    }
                }
            }
            if (night && random.nextInt(30) == 0) {
                spawnNear(level, player, 2);
            }
        } else if (sanity < 35.0F && night && random.nextInt(60) == 0) {
            spawnNear(level, player, 1);
        }
        if (sanity <= 0.0F && player.tickCount % 100 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
        }
    }

    /** Something crawls out of the dark nearby (out of the water, if there is water there) and comes for you. */
    private static void spawnNear(ServerLevel level, ServerPlayer player, int count) {
        RandomSource random = player.getRandom();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double distance = 16 + random.nextDouble() * 8;
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            boolean water = level.getFluidState(pos.below()).is(net.minecraft.tags.FluidTags.WATER);
            Mob mob = (water ? EntityType.DROWNED : EntityType.ZOMBIE).create(level);
            if (mob == null) {
                continue;
            }
            mob.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.EVENT, null);
            mob.setTarget(player);
            level.addFreshEntity(mob);
        }
    }

    private static void sync(ServerPlayer player, boolean force) {
        float sanity = get(player);
        Float last = LAST_SENT.get(player.getUUID());
        if (force || last == null || Math.abs(last - sanity) >= 0.5F || player.tickCount % 200 == 0) {
            LAST_SENT.put(player.getUUID(), sanity);
            PacketDistributor.sendToPlayer(player, new ModNetwork.SanityPayload(sanity));
        }
    }

    /** A night's sleep settles the mind. */
    @SubscribeEvent
    public static void onWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && FogHarbor.is(player.level())) {
            set(player, get(player) + 40.0F);
        }
    }

    /** So does a hot meal, a little. */
    @SubscribeEvent
    public static void onEat(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player && FogHarbor.is(player.level())
                && event.getItem().getFoodProperties(player) != null) {
            set(player, get(player) + 3.0F);
        }
    }

    /** After death, and when arriving in a world, start from what the player has (and tell their client). */
    public static void resend(ServerPlayer player) {
        LAST_SENT.remove(player.getUUID());
        EXPOSURE.remove(player.getUUID());
        sync(player, true);
    }
}
