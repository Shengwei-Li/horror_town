package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.Watcher;
import com.horrortown.registry.ModEffects;
import com.horrortown.registry.ModEntities;
import com.horrortown.world.FogHarbor;
import com.horrortown.world.Sanity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Raises the Watcher out of the sea on a tide night: off the coast from wherever you stand, at the edge of the
 * fog, turned to face you. It rises slowly when the tide night begins and sinks again at dawn. Inland, out of
 * sight of the sea, there is nothing there.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID, value = Dist.CLIENT)
public final class WatcherApparition {
    private static final int ENTITY_ID = -424_242;
    /** How far out to sea it stands, how far under the surface its base is, how long it takes to rise. */
    private static final double DISTANCE = 75.0;
    private static final double SUNK = 1.5;
    private static final double RISE = 24.0;
    private static final float RISE_TICKS = 600.0F;
    /** If you move this far along the coast, it reappears off the new stretch. */
    private static final double FOLLOW = 70.0;
    private static final double SIDEWAYS = Math.toRadians(40.0);

    private static Watcher watcher;
    private static Vec3 anchor;
    private static float risen;

    private WatcherApparition() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        if (level == null || player == null) {
            watcher = null;
            return;
        }
        double r = Math.sqrt(player.getX() * player.getX() + player.getZ() * player.getZ());
        boolean wanted = ClientTide.active() && FogHarbor.is(level) && r > Sanity.COAST_RADIUS;
        risen = Mth.clamp(risen + (wanted ? 1.0F : -1.0F) / RISE_TICKS, 0.0F, 1.0F);
        if (risen <= 0.0F) {
            remove(level);
            return;
        }
        // out to sea from where you stand, a little off to one side or the other (changes night to night)
        double angle = Math.atan2(player.getZ(), player.getX())
                + ((level.getDayTime() / 24000L) % 2 == 0 ? SIDEWAYS : -SIDEWAYS);
        Vec3 target = new Vec3(player.getX() + Math.cos(angle) * DISTANCE, level.getSeaLevel() - SUNK,
                player.getZ() + Math.sin(angle) * DISTANCE);
        if (watcher == null || watcher.level() != level) {
            watcher = ModEntities.WATCHER.get().create(level);
            if (watcher == null) {
                return;
            }
            watcher.setId(ENTITY_ID);
            anchor = target;
            watcher.moveTo(target.x, target.y, target.z, 0.0F, 0.0F);
            level.addEntity(watcher);
        } else if (wanted && target.distanceTo(anchor) > FOLLOW) {
            anchor = target;
        }
        float ease = risen * risen * (3.0F - 2.0F * risen);
        watcher.setPos(anchor.x, anchor.y - RISE * (1.0F - ease), anchor.z);
        // always turned towards you
        double dx = player.getX() - anchor.x;
        double dz = player.getZ() - anchor.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) + 90.0F; // the model's face is on its -Z side
        watcher.setYRot(yaw);
        watcher.setYBodyRot(yaw);
        watcher.setYHeadRot(yaw);
        watcher.setGaping(player.hasEffect(ModEffects.WATCHED));
        if (player.getRandom().nextInt(140) == 0) {
            watcher.blink();
        }
    }

    /** Where it stands now, if it is up. */
    static Vec3 position() {
        return watcher == null ? null : watcher.position();
    }

    /** Development only: skip the slow rise (for the screenshot run). */
    static void riseNow() {
        risen = 1.0F;
    }

    private static void remove(ClientLevel level) {
        if (watcher != null) {
            level.removeEntity(ENTITY_ID, Entity.RemovalReason.DISCARDED);
            watcher = null;
        }
    }
}
