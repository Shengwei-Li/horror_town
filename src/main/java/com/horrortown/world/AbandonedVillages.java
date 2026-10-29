package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.ZombieVillager;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingConversionEvent;

/**
 * 荒村: the villages inland were left long ago. In a Fog Harbor world the villagers a village is built with never
 * arrive; a few of them are still there, turned, and the iron golems have gone too. A villager a player cures
 * (or breeds) is the player's own and stays.
 */
@EventBusSubscriber(modid = HorrorTownMod.MODID)
public final class AbandonedVillages {
    /** Set on every villager that has been looked at, so each is decided once. */
    private static final String SEEN = "horror_town.seen";
    private static final float TURNED = 0.3F;

    private AbandonedVillages() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !FogHarbor.is(level)) {
            return;
        }
        if (event.getEntity() instanceof IronGolem golem && !golem.isPlayerCreated()) {
            event.setCanceled(true);
            return;
        }
        if (!(event.getEntity() instanceof Villager villager) || villager.getTags().contains(SEEN)) {
            return;
        }
        if (villager.isBaby() || villager.getVillagerXp() > 0) {
            // born here, or already traded with: someone's
            villager.addTag(SEEN);
            return;
        }
        event.setCanceled(true);
        if (level.getRandom().nextFloat() < TURNED) {
            double x = villager.getX();
            double y = villager.getY();
            double z = villager.getZ();
            var data = villager.getVillagerData();
            // not while the chunk is still being loaded
            level.getServer().tell(new TickTask(level.getServer().getTickCount() + 1, () -> {
                ZombieVillager zombie = EntityType.ZOMBIE_VILLAGER.create(level);
                if (zombie != null) {
                    zombie.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0F, 0.0F);
                    zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.STRUCTURE, null);
                    zombie.setVillagerData(data);
                    zombie.setPersistenceRequired();
                    level.addFreshEntity(zombie);
                }
            }));
        }
    }

    /** A villager cured from a zombie is the player's doing, and stays. */
    @SubscribeEvent
    public static void onConverted(LivingConversionEvent.Post event) {
        if (event.getOutcome() instanceof Villager villager) {
            villager.addTag(SEEN);
        }
    }
}
