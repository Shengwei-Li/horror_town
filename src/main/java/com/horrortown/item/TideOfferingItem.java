package com.horrortown.item;

import com.horrortown.entity.TideMotherIdol;
import com.horrortown.entity.boss.TideMotherTrue;
import com.horrortown.registry.ModEntities;
import com.horrortown.world.FogHarbor;
import com.horrortown.world.TideNight;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 潮母祭品: held up before the idol in the temple at night, it is answered. The tide comes in, and out in the sea
 * in front of the temple the Tide Mother's true body rises.
 */
public class TideOfferingItem extends Item {
    private static final double IDOL_RANGE = 16.0;

    public TideOfferingItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResultHolder.success(stack);
        }
        TideMotherIdol idol = level.getEntitiesOfClass(TideMotherIdol.class, player.getBoundingBox().inflate(IDOL_RANGE))
                .stream().findFirst().orElse(null);
        String refusal = idol == null ? "message.horror_town.offering.no_idol"
                : !level.isNight() ? "message.horror_town.offering.day"
                : !level.getEntitiesOfClass(TideMotherTrue.class, player.getBoundingBox().inflate(256)).isEmpty()
                ? "message.horror_town.offering.risen" : null;
        if (refusal != null) {
            player.displayClientMessage(Component.translatable(refusal), true);
            return InteractionResultHolder.fail(stack);
        }
        Vec3 at = riseFrom(server, idol);
        TideMotherTrue mother = ModEntities.TIDE_MOTHER_TRUE.get().create(server);
        if (mother == null) {
            return InteractionResultHolder.fail(stack);
        }
        mother.moveTo(at.x, at.y, at.z, idol.getYRot() + 180.0F, 0.0F);
        mother.finalizeSpawn(server, server.getCurrentDifficultyAt(BlockPos.containing(at)), MobSpawnType.EVENT, null);
        mother.emerge();
        server.addFreshEntity(mother);
        if (FogHarbor.is(server) && !TideNight.get(server).active()) {
            TideNight.get(server).start(server);
        }
        server.playSound(null, idol.blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3.0F, 0.5F);
        server.playSound(null, BlockPos.containing(at), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 64.0F, 0.4F);
        player.displayClientMessage(Component.translatable("message.horror_town.offering.answered"), true);
        stack.consume(1, player);
        return InteractionResultHolder.consume(stack);
    }

    /** The nearest stretch of open water out in front of the idol, or failing that a spot twenty blocks out. */
    private static Vec3 riseFrom(ServerLevel level, TideMotherIdol idol) {
        Vec3 out = Vec3.directionFromRotation(0.0F, idol.getYRot());
        for (int distance = 24; distance <= 96; distance += 4) {
            int x = Mth.floor(idol.getX() + out.x * distance);
            int z = Mth.floor(idol.getZ() + out.z * distance);
            int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (level.getFluidState(new BlockPos(x, surface - 1, z)).is(FluidTags.WATER)) {
                return new Vec3(x + 0.5, level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z), z + 0.5);
            }
        }
        int x = Mth.floor(idol.getX() + out.x * 20);
        int z = Mth.floor(idol.getZ() + out.z * 20);
        return new Vec3(x + 0.5, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5);
    }
}
