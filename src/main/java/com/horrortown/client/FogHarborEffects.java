package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * The Fog Harbor sky: a flat overcast lid the colour of the fog, no stars, no sunrise glow. The sun and moon
 * are only a soft smudge of light behind the cloud. Clouds hang lower than in the overworld.
 */
public class FogHarborEffects extends DimensionSpecialEffects {
    private static final ResourceLocation HALO =
            ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "textures/environment/sky_halo.png");
    private static final float CLOUD_LEVEL = 150.0F;
    private static final float SUN_SIZE = 70.0F;
    private static final float SUN_ALPHA = 0.32F;
    private static final float MOON_SIZE = 40.0F;
    private static final float MOON_ALPHA = 0.10F;

    public FogHarborEffects() {
        super(CLOUD_LEVEL, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float brightness) {
        return fogColor.multiply(brightness * 0.94F + 0.06F, brightness * 0.94F + 0.06F, brightness * 0.91F + 0.09F);
    }

    @Override
    public boolean isFoggyAt(int x, int z) {
        return false;
    }

    @Override
    @Nullable
    public float[] getSunriseColor(float timeOfDay, float partialTicks) {
        return null;
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float partialTick, Matrix4f modelViewMatrix, Camera camera,
                             Matrix4f projectionMatrix, boolean isFoggy, Runnable setupFog) {
        // the cleared background is already the fog colour: that is the whole overcast sky
        if (isFoggy || camera.getFluidInCamera() != FogType.NONE) {
            return true;
        }
        float sunHeight = Mth.cos(level.getSunAngle(partialTick));
        float veil = (1.0F - level.getRainLevel(partialTick) * 0.7F) * (1.0F - ClientFog.thickness() * 0.85F);
        float sun = SUN_ALPHA * veil * Mth.clamp(sunHeight * 3.0F + 0.3F, 0.0F, 1.0F);
        float moon = MOON_ALPHA * veil * Mth.clamp(-sunHeight * 3.0F + 0.3F, 0.0F, 1.0F);
        if (sun <= 0.005F && moon <= 0.005F) {
            return true;
        }

        PoseStack pose = new PoseStack();
        pose.mulPose(modelViewMatrix);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(partialTick) * 360.0F));
        Matrix4f matrix = pose.last().pose();

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HALO);
        if (sun > 0.005F) {
            RenderSystem.setShaderColor(1.0F, 0.98F, 0.92F, sun);
            quad(matrix, SUN_SIZE, 100.0F);
        }
        if (moon > 0.005F) {
            RenderSystem.setShaderColor(0.85F, 0.9F, 1.0F, moon);
            quad(matrix, MOON_SIZE, -100.0F);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void quad(Matrix4f matrix, float size, float y) {
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, -size, y, -size).setUv(0.0F, 0.0F);
        buffer.addVertex(matrix, size, y, -size).setUv(1.0F, 0.0F);
        buffer.addVertex(matrix, size, y, size).setUv(1.0F, 1.0F);
        buffer.addVertex(matrix, -size, y, size).setUv(0.0F, 1.0F);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }
}
