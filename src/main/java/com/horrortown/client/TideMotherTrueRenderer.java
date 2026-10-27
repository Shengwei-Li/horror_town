package com.horrortown.client;

import com.horrortown.entity.boss.TideMotherTrue;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** 潮母真身: drawn at a third of its size and rendered three times as big, about ten blocks tall. */
public class TideMotherTrueRenderer extends HorrorMobRenderer<TideMotherTrue> {
    public TideMotherTrueRenderer(EntityRendererProvider.Context context) {
        super(context, "tide_mother_true", true, true, 3.0F);
        withScale(3.0F);
    }

    @Override
    protected float getDeathMaxRotation(TideMotherTrue mother) {
        return 0.0F; // she sinks back into the sea instead of keeling over
    }
}
