package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.TownRole;
import com.horrortown.entity.Townsfolk;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Townsfolk: one tall, thin model for all of them (tools/gen_townsfolk.py), with a texture per trade and stage.
 * As the town turns they deform: the neck stretches, the back hunches, the head sits crooked; at the last
 * stage their fish eyes glow in the dark. Only the fisher wears the straw hat, only the keeper the robe and hood.
 */
public class TownsfolkRenderer extends GeoEntityRenderer<Townsfolk> {
    private static final ResourceLocation[][] TEXTURES = new ResourceLocation[TownRole.values().length][3];
    /** Neck length, forward hunch (degrees) and resting head tilt (degrees) for each stage. */
    private static final float[] NECK = {1.0F, 1.35F, 1.8F};
    private static final float[] HUNCH = {0.0F, 6.0F, 16.0F};
    private static final float[] TILT = {0.0F, 6.0F, 14.0F};
    /** Extra head tilt while staring at someone. */
    private static final float STARE_TILT = 28.0F;

    static {
        for (TownRole role : TownRole.values()) {
            for (int stage = 0; stage < 3; stage++) {
                TEXTURES[role.ordinal()][stage] = ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID,
                        "textures/entity/townsfolk/" + role.getSerializedName() + "_" + stage + ".png");
            }
        }
    }

    public TownsfolkRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(PoseStack poseStack, Townsfolk folk, BakedGeoModel bakedModel,
                               @Nullable RenderType renderType, MultiBufferSource bufferSource,
                               @Nullable VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
                if (folk.stage() == 2) { // only the fish eyes of the last stage glow
                    super.render(poseStack, folk, bakedModel, renderType, bufferSource, buffer, partialTick,
                            packedLight, packedOverlay);
                }
            }
        });
        this.shadowRadius = 0.4F;
    }

    private static class Model extends DefaultedEntityGeoModel<Townsfolk> {
        Model() {
            super(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "townsfolk"), true);
        }

        @Override
        public ResourceLocation getTextureResource(Townsfolk folk) {
            return TEXTURES[folk.role().ordinal()][folk.stage()];
        }

        @Override
        public void setCustomAnimations(Townsfolk folk, long instanceId, AnimationState<Townsfolk> state) {
            super.setCustomAnimations(folk, instanceId, state);
            int stage = folk.stage();
            TownRole role = folk.role();
            getBone("hat").ifPresent(b -> b.setHidden(role != TownRole.FISHER));
            getBone("hood").ifPresent(b -> b.setHidden(role != TownRole.KEEPER));
            getBone("robe").ifPresent(b -> b.setHidden(role != TownRole.KEEPER));
            getBone("neck").ifPresent(b -> b.setScaleY(NECK[stage]));
            getBone("torso").ifPresent(b -> b.setRotX(b.getRotX() - HUNCH[stage] * (float) Math.PI / 180.0F));
            float tilt = TILT[stage] + (folk.isStaring() ? STARE_TILT : 0.0F);
            getBone("head").ifPresent(b -> b.setRotZ(tilt * (float) Math.PI / 180.0F));
        }
    }
}
