package com.horrortown.item;

import com.horrortown.entity.WaterSword;
import com.horrortown.registry.ModGear;
import com.horrortown.world.Sanity;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 潮母法杖: held up, it draws blades of sea water out of the air one by one, up to five, hung behind your
 * shoulders; let go and they all fly at what you are looking at. Under water they come quicker and fly faster.
 * Every blade borrows from her, and costs a little of your mind. Just carrying it, you can stay under far longer
 * and the dark gets to you more slowly (see Sanity and GearEvents).
 */
public class TideStaffItem extends Item {
    private static final int DRAW_TICKS = 10;
    private static final int DRAW_TICKS_UNDERWATER = 5;
    private static final double AIM_RANGE = 48.0;
    private static final float SANITY_PER_SWORD = 0.5F;

    public TideStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        int held = getUseDuration(stack, entity) - remaining;
        int every = player.isEyeInFluid(FluidTags.WATER) ? DRAW_TICKS_UNDERWATER : DRAW_TICKS;
        if (held % every != every - 1) {
            return;
        }
        int drawn = swords(player).size();
        if (drawn >= WaterSword.MAX_SWORDS) {
            return;
        }
        level.addFreshEntity(new WaterSword(level, player, drawn));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE,
                SoundSource.PLAYERS, 1.0F, 0.8F + drawn * 0.15F);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        List<WaterSword> swords = swords(player);
        if (swords.isEmpty()) {
            return;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 far = eye.add(player.getLookAngle().scale(AIM_RANGE));
        HitResult sight = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 target = sight.getType() == HitResult.Type.MISS ? far : sight.getLocation();
        boolean underwater = player.isEyeInFluid(FluidTags.WATER);
        for (WaterSword sword : swords) {
            sword.launch(target, underwater);
        }
        stack.hurtAndBreak(swords.size(), player, LivingEntity.getSlotForHand(player.getUsedItemHand()));
        player.getCooldowns().addCooldown(this, 10);
        if (player instanceof ServerPlayer server && !player.isCreative()) {
            Sanity.set(server, Sanity.get(server) - SANITY_PER_SWORD * swords.size());
        }
    }

    private static List<WaterSword> swords(Player player) {
        return player.level().getEntitiesOfClass(WaterSword.class, player.getBoundingBox().inflate(4.0),
                sword -> sword.hovering() && sword.getOwner() == player);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return repair.is(ModGear.MOTHER_SCALE.get());
    }

    /** Whether this one is carrying the staff, in either hand. */
    public static boolean carried(LivingEntity entity) {
        return entity.getMainHandItem().getItem() instanceof TideStaffItem
                || entity.getOffhandItem().getItem() instanceof TideStaffItem;
    }
}
