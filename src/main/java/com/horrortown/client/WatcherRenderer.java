package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.Watcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * The Watcher is modelled at 1/6 size and drawn 9x: a head some thirty blocks tall. Its eyes glow, and its
 * pupils turn to find you (it already faces you; the pupils follow you up and down).
 */
public class WatcherRenderer extends GeoEntityRenderer<Watcher> {
    private static final float MODEL_SCALE = 9.0F;
    /** Height of the eyes above the model's base, in blocks, and how far the pupils can roll (model units). */
    private static final double EYE_HEIGHT = 33.0 * MODEL_SCALE / 16.0;
    private static final float PUPIL_TRAVEL = 2.5F;

    public WatcherRenderer(EntityRendererProvider.Context context) {
        super(context, new Model());
        withScale(MODEL_SCALE);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        this.shadowRadius = 0.0F;
    }

    private static class Model extends DefaultedEntityGeoModel<Watcher> {
        Model() {
            super(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "watcher"));
        }

        @Override
        public void setCustomAnimations(Watcher watcher, long instanceId, AnimationState<Watcher> state) {
            super.setCustomAnimations(watcher, instanceId, state);
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            Vec3 eyes = watcher.position().add(0.0, EYE_HEIGHT, 0.0);
            Vec3 to = camera.subtract(eyes);
            float down = (float) Mth.clamp(to.y / Math.max(1.0, to.horizontalDistance()) * 6.0, -PUPIL_TRAVEL, 1.0);
            for (String pupil : new String[]{"right_pupil", "left_pupil"}) {
                getBone(pupil).ifPresent(b -> b.setPosY(down));
            }
        }
    }
}
