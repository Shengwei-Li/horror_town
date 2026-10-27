package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Draws any of the Fog Harbor creatures from its generated model (tools/gen_mobs.py): geo/entity/&lt;name&gt;,
 * textures/entity/&lt;name&gt;, animations/entity/&lt;name&gt;. Humanoids turn their "head" bone to look about.
 */
public class HorrorMobRenderer<T extends Entity & GeoEntity> extends GeoEntityRenderer<T> {
    public HorrorMobRenderer(EntityRendererProvider.Context context, String name, boolean glows, boolean turnsHead,
                             float shadow) {
        super(context, new DefaultedEntityGeoModel<>(ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, name),
                turnsHead));
        if (glows) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
        this.shadowRadius = shadow;
    }
}
