package com.horrortown.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 窥潮镜: a spyglass with a murky eye for a lens. Whatever is out there in the fog in front of you, or lying under
 * the mud waiting, shows up through it as an outline.
 */
public class TideGlassItem extends Item {
    private static final double RANGE = 64.0;
    /** How wide a cone it sees (cosine of the half-angle). */
    private static final double CONE = Math.cos(Math.toRadians(25.0));

    public TideGlassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.playSound(SoundEvents.SPYGLASS_USE, 1.0F, 0.7F);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 1200;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPYGLASS;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || remaining % 10 != 0) {
            return;
        }
        Vec3 eye = entity.getEyePosition();
        Vec3 look = entity.getLookAngle();
        for (LivingEntity seen : level.getEntitiesOfClass(LivingEntity.class, entity.getBoundingBox().inflate(RANGE),
                e -> e instanceof Enemy && e.isAlive())) {
            Vec3 to = seen.getBoundingBox().getCenter().subtract(eye);
            if (to.length() < RANGE && to.normalize().dot(look) > CONE) {
                seen.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false));
            }
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        entity.playSound(SoundEvents.SPYGLASS_STOP_USING, 1.0F, 0.7F);
    }
}
