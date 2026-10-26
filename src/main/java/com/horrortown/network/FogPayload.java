package com.horrortown.network;

import com.horrortown.HorrorTownMod;
import com.horrortown.client.ClientFog;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Tells a client how thick the fog over the land should be (0 clear, 1 thickest); it rolls towards that slowly. */
public record FogPayload(float level) implements CustomPacketPayload {
    public static final Type<FogPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "fog"));
    public static final StreamCodec<ByteBuf, FogPayload> STREAM_CODEC =
            ByteBufCodecs.FLOAT.map(FogPayload::new, FogPayload::level);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, STREAM_CODEC, FogPayload::handle);
    }

    private static void handle(FogPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientFog.setTarget(payload.level()));
    }
}
