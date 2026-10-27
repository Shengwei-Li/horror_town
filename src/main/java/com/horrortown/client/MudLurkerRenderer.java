package com.horrortown.client;

import com.horrortown.entity.mob.MudLurker;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.cache.object.BakedGeoModel;

/**
 * 泥人: its model is drawn at half size and rendered twice as big, six blocks tall. The mud mound it waits in is
 * only shown while it is still down there.
 */
public class MudLurkerRenderer extends HorrorMobRenderer<MudLurker> {
    public MudLurkerRenderer(EntityRendererProvider.Context context) {
        super(context, "mud_lurker", true, true, 1.6F);
        withScale(2.0F);
    }

    @Override
    public void preRender(PoseStack poseStack, MudLurker lurker, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight,
                          int packedOverlay, int colour) {
        model.getBone("mound").ifPresent(bone -> bone.setHidden(!lurker.isUnderground()));
        super.preRender(poseStack, lurker, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
    }
}
