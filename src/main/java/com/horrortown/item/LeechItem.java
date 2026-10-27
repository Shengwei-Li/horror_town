package com.horrortown.item;

import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;

/** 水蛭: set to your arm, it draws off bad blood, whatever ails you along with it, and a little of your own. */
public class LeechItem extends Item {
    private static final float BLOOD = 4.0F;

    public LeechItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            List<Holder<MobEffect>> ails = player.getActiveEffects().stream().map(MobEffectInstance::getEffect)
                    .filter(e -> e.value().getCategory() == MobEffectCategory.HARMFUL).toList();
            ails.forEach(player::removeEffect);
            player.hurt(level.damageSources().generic(), BLOOD);
            ((ServerLevel) level).sendParticles(new DustParticleOptions(new Vector3f(0.5F, 0.0F, 0.02F), 1.0F),
                    player.getX(), player.getY(0.6), player.getZ(), 20, 0.3, 0.3, 0.3, 0.0);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_SQUISH_SMALL,
                SoundSource.PLAYERS, 1.0F, 0.6F);
        player.getCooldowns().addCooldown(this, 40);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
