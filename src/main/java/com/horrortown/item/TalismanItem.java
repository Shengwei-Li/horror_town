package com.horrortown.item;

import com.horrortown.entity.ThrownTalisman;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 镇魂符: a yellow paper talisman, thrown like a dart. */
public class TalismanItem extends Item {
    public TalismanItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        if (!level.isClientSide) {
            ThrownTalisman talisman = new ThrownTalisman(level, player);
            talisman.setItem(stack);
            talisman.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.6F, 0.5F);
            level.addFreshEntity(talisman);
        }
        player.getCooldowns().addCooldown(this, 20);
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
