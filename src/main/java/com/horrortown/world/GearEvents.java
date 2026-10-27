package com.horrortown.world;

import com.horrortown.HorrorTownMod;
import com.horrortown.item.TideStaffItem;
import com.horrortown.registry.ModEffects;
import com.horrortown.registry.ModGear;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** What the gear does that its items cannot do for themselves. */
@EventBusSubscriber(modid = HorrorTownMod.MODID)
public final class GearEvents {
    private static final ResourceLocation FISH_SWIM = ResourceLocation.fromNamespaceAndPath(HorrorTownMod.MODID, "fish_scale_swim");
    /** Each piece of fish-scale armour: this much faster in the water. */
    private static final double SWIM_PER_PIECE = 0.15;
    /** With the staff in hand your breath runs out a fifth as fast. */
    private static final float BREATH_KEPT = 0.8F;

    private GearEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (TideStaffItem.carried(player) && player.isEyeInFluid(FluidTags.WATER)
                && player.getAirSupply() < player.getMaxAirSupply() && player.getRandom().nextFloat() < BREATH_KEPT) {
            player.setAirSupply(player.getAirSupply() + 1);
        }
        if (player.tickCount % 20 == 0) {
            AttributeInstance swim = player.getAttribute(NeoForgeMod.SWIM_SPEED);
            int pieces = ModGear.fishScalePieces(player);
            if (swim != null) {
                if (pieces > 0) {
                    swim.addOrUpdateTransientModifier(new AttributeModifier(FISH_SWIM, SWIM_PER_PIECE * pieces,
                            AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
                } else {
                    swim.removeModifier(FISH_SWIM);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        if (attacker.hasEffect(ModEffects.BOUND) && event.getSource().getDirectEntity() == attacker) {
            event.setCanceled(true); // pinned by a talisman, its blows have nothing behind them
            return;
        }
        if (event.getSource().getDirectEntity() == attacker && attacker.getMainHandItem().is(ModGear.COIN_SWORD.get())
                && event.getEntity().getType().is(ModGear.YIN)) {
            event.setAmount(event.getAmount() * 2.0F);
        }
    }
}
