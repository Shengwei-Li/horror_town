package com.horrortown.registry;

import com.horrortown.HorrorTownMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, HorrorTownMod.MODID);

    /**
     * 被注视: the Watcher has noticed you. While it lasts your sanity drains, every monster nearby comes for you,
     * and more of them crawl out of the dark. What it does lives in {@link com.horrortown.world.Sanity}.
     */
    public static final DeferredHolder<MobEffect, MobEffect> WATCHED = EFFECTS.register("watched",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0x9AB0A0) {
            });

    /**
     * 镇住: pinned by a talisman. It cannot move a step, and its blows have nothing behind them (see GearEvents).
     */
    public static final DeferredHolder<MobEffect, MobEffect> BOUND = EFFECTS.register("bound",
            () -> new MobEffect(MobEffectCategory.HARMFUL, 0xD8B040) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
                    return true;
                }

                @Override
                public boolean applyEffectTick(LivingEntity entity, int amplifier) {
                    if (entity instanceof Mob mob) {
                        mob.getNavigation().stop();
                    }
                    entity.setDeltaMovement(0.0, Math.min(0.0, entity.getDeltaMovement().y), 0.0);
                    return true;
                }
            }.addAttributeModifier(Attributes.MOVEMENT_SPEED,
                    ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "effect.bound"), -1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    private ModEffects() {
    }
}
