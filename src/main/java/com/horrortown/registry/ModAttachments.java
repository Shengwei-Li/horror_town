package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import com.horrortown.world.Sanity;
import com.mojang.serialization.Codec;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, HorrorTownMod.MODID);

    /** 理智: a player's sanity, 0..100. Not kept through death. */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Float>> SANITY = ATTACHMENTS.register("sanity",
            () -> AttachmentType.builder(() -> Sanity.MAX).serialize(Codec.FLOAT).build());

    private ModAttachments() {
    }
}
