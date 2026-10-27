package com.horrortown.network;

import com.horrortown.HorrorTownMod;
import com.horrortown.client.ClientSanity;
import com.horrortown.client.ClientTide;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** All of the mod's packets (server to client only). */
public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(FogPayload.TYPE, FogPayload.STREAM_CODEC, FogPayload::handle)
                .playToClient(SanityPayload.TYPE, SanityPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientSanity.set(payload.sanity())))
                .playToClient(TidePayload.TYPE, TidePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> ClientTide.set(payload.active())));
    }

    /** The player's current sanity. */
    public record SanityPayload(float sanity) implements CustomPacketPayload {
        public static final Type<SanityPayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "sanity"));
        public static final StreamCodec<ByteBuf, SanityPayload> STREAM_CODEC =
                ByteBufCodecs.FLOAT.map(SanityPayload::new, SanityPayload::sanity);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Whether a tide night (潮汛夜) is on in the player's world. */
    public record TidePayload(boolean active) implements CustomPacketPayload {
        public static final Type<TidePayload> TYPE =
                new Type<>(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "tide"));
        public static final StreamCodec<ByteBuf, TidePayload> STREAM_CODEC =
                ByteBufCodecs.BOOL.map(TidePayload::new, TidePayload::active);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
