package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.TideMotherIdol;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** The idol is modelled at half size and drawn at 2x; its eyes glow (tide_mother_glowmask.png). */
public class TideMotherRenderer extends GeoEntityRenderer<TideMotherIdol> {
    private static final float MODEL_SCALE = 2.0F;
    /** The hall is pitch dark; the idol is always lit at least this much, as if by the candles at its feet. */
    private static final int MIN_BLOCK_LIGHT = 10;

    public TideMotherRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "tide_mother")));
        withScale(MODEL_SCALE);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(TideMotherIdol entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        int light = LightTexture.pack(Math.max(MIN_BLOCK_LIGHT, LightTexture.block(packedLight)),
                LightTexture.sky(packedLight));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, light);
    }
}
