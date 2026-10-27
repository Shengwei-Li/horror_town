package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import com.horrortown.network.FogPayload;
import com.horrortown.network.ModNetwork;
import com.horrortown.town.TownSite;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = HorrorTownMod.MODID)
public final class WorldEvents {
    private WorldEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && FogHarbor.is(level)) {
            FogWeather.get(level).tick(level);
            TideNight.get(level).tick(level);
        }
    }

    /** New Fog Harbor worlds start on the road outside the town gate, on the island's coast. */
    @SubscribeEvent
    public static void onCreateSpawn(LevelEvent.CreateSpawnPosition event) {
        if (event.getLevel() instanceof ServerLevel level && FogHarbor.is(level)) {
            ChunkGenerator generator = level.getChunkSource().getGenerator();
            RandomState random = level.getChunkSource().randomState();
            TownSite.get(generator, random, level, level.getSeed()).ifPresent(site -> {
                BlockPos spawn = site.spawn();
                int y = Math.max(generator.getSeaLevel() + 1, generator.getBaseHeight(spawn.getX(), spawn.getZ(),
                        Heightmap.Types.OCEAN_FLOOR_WG, level, random));
                event.getSettings().setSpawn(spawn.atY(y), 0.0F);
                event.setCanceled(true);
            });
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        syncFog(event.getEntity());
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        syncFog(event.getEntity());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        syncFog(event.getEntity());
    }

    private static void syncFog(Player entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        if (FogHarbor.is(player.serverLevel())) {
            FogWeather.get(player.serverLevel()).sendTo(player);
            TideNight.get(player.serverLevel()).sendTo(player);
        } else {
            PacketDistributor.sendToPlayer(player, new FogPayload(0.0F));
            PacketDistributor.sendToPlayer(player, new ModNetwork.TidePayload(false));
        }
        Sanity.resend(player);
    }

    /**
     * /horrortown fog <level> [seconds] sets the fog for a while; /horrortown fog auto hands it back to the cycle.
     * /horrortown tide start|stop starts or ends a tide night; /horrortown sanity <value> sets your sanity.
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("horrortown")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("fog")
                        .then(Commands.literal("auto").executes(ctx -> setFog(ctx.getSource(), 0.0F, 1)))
                        .then(Commands.argument("level", FloatArgumentType.floatArg(0.0F, 1.0F))
                                .executes(ctx -> setFog(ctx.getSource(),
                                        FloatArgumentType.getFloat(ctx, "level"), 300))
                                .then(Commands.argument("seconds", IntegerArgumentType.integer(1))
                                        .executes(ctx -> setFog(ctx.getSource(),
                                                FloatArgumentType.getFloat(ctx, "level"),
                                                IntegerArgumentType.getInteger(ctx, "seconds"))))))
                .then(Commands.literal("tide")
                        .then(Commands.literal("start").executes(ctx -> setTide(ctx.getSource(), true)))
                        .then(Commands.literal("stop").executes(ctx -> setTide(ctx.getSource(), false))))
                .then(Commands.literal("sanity")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, Sanity.MAX))
                                .executes(ctx -> {
                                    Sanity.set(ctx.getSource().getPlayerOrException(),
                                            FloatArgumentType.getFloat(ctx, "value"));
                                    return 1;
                                }))));
    }

    private static int setTide(CommandSourceStack source, boolean start) {
        ServerLevel world = source.getLevel();
        if (!FogHarbor.is(world)) {
            source.sendFailure(Component.translatable("commands.horror_town.fog.wrong_world"));
            return 0;
        }
        TideNight tide = TideNight.get(world);
        if (start) {
            tide.start(world);
        } else {
            tide.end(world, world.getDayTime() / 24000L);
        }
        return 1;
    }

    private static int setFog(CommandSourceStack source, float level, int seconds) {
        ServerLevel world = source.getLevel();
        if (!FogHarbor.is(world)) {
            source.sendFailure(Component.translatable("commands.horror_town.fog.wrong_world"));
            return 0;
        }
        FogWeather.get(world).set(world, level, seconds * 20);
        source.sendSuccess(() -> Component.translatable("commands.horror_town.fog.set", level), true);
        return 1;
    }
}
