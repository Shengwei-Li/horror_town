package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.WaterSword;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The water sword, drawn like an arrow: two crossed quads along its flight, each carrying the sword's icon turned
 * on its corner so the blade runs straight ahead and the hilt trails behind. Lit from within.
 */
public class WaterSwordRenderer extends EntityRenderer<WaterSword> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "textures/item/water_sword.png");
    /** Half its length, tip to pommel, in blocks. */
    private static final float HALF = 1.1F;

    public WaterSwordRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(WaterSword sword, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int packedLight) {
        pose.pushPose();
        // as for arrows: after these, +X points along its flight
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, sword.yRotO, sword.getYRot()) - 90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, sword.xRotO, sword.getXRot())));
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        for (int plane = 0; plane < 2; plane++) { // one flat, one on edge
            PoseStack.Pose at = pose.last();
            // the icon's tip is its top right corner (u 1, v 0), its pommel the bottom left (u 0, v 1)
            float[][] corners = {{HALF, 0, 1, 0}, {0, HALF, 0, 0}, {-HALF, 0, 0, 1}, {0, -HALF, 1, 1}};
            for (int side = 0; side < 2; side++) { // both faces, so it shows from either side
                for (int i = 0; i < 4; i++) {
                    float[] c = corners[side == 0 ? i : 3 - i];
                    vertex(buffer, at, c[0], c[1], c[2], c[3]);
                }
            }
            pose.mulPose(Axis.XP.rotationDegrees(90.0F));
        }
        pose.popPose();
        super.render(sword, yaw, partialTick, pose, buffers, packedLight);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose at, float x, float y, float u, float v) {
        buffer.addVertex(at, x, y, 0.0F).setColor(255, 255, 255, 235).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(at, 0.0F, 0.0F, 1.0F);
    }

    @Override
    public ResourceLocation getTextureLocation(WaterSword sword) {
        return TEXTURE;
    }
}
