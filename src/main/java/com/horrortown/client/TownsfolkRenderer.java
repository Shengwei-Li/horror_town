package com.horrortown.client;

import com.horrortown.HorrorTownMod;
import com.horrortown.entity.TownRole;
import com.horrortown.entity.Townsfolk;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Townsfolk use the player body; their skin depends on their trade and on how far they have turned. */
public class TownsfolkRenderer extends HumanoidMobRenderer<Townsfolk, PlayerModel<Townsfolk>> {
    private static final ResourceLocation[][] TEXTURES = new ResourceLocation[TownRole.values().length][3];

    static {
        for (TownRole role : TownRole.values()) {
            for (int stage = 0; stage < 3; stage++) {
                TEXTURES[role.ordinal()][stage] = ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID,
                        "textures/entity/townsfolk/" + role.getSerializedName() + "_" + stage + ".png");
            }
        }
    }

    public TownsfolkRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(Townsfolk entity) {
        return TEXTURES[entity.role().ordinal()][entity.stage()];
    }
}
