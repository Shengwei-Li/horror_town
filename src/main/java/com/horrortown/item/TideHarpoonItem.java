package com.horrortown.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.phys.Vec3;

/** 潮心鱼叉: the fishman's harpoon with the Tide Mother's heart bound into it. What it hits is reeled in. */
public class TideHarpoonItem extends SwordItem {
    public TideHarpoonItem(Properties properties) {
        super(Tiers.NETHERITE, properties);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        Vec3 pull = attacker.position().subtract(target.position()).multiply(1.0, 0.0, 1.0).normalize().scale(0.9);
        target.push(pull.x, 0.25, pull.z);
        target.hurtMarked = true;
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), attacker);
        return super.hurtEnemy(stack, target, attacker);
    }
}
