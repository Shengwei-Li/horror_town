package com.horrortown.entity.mob;

import com.horrortown.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.EventHooks;

import java.util.EnumSet;

/**
 * When a monster cannot reach its target, it tears through whatever soft block is in its way: wood, glass,
 * leaves, earth, mud, wool. It takes a while (the block cracks as it goes) and stone stops it cold — so only a
 * house of stone or better keeps them out. Obeys the mobGriefing game rule.
 */
public class BreakSoftBlockGoal extends Goal {
    private static final int MIN_TICKS = 40;

    private final Mob mob;
    private BlockPos block;
    private int progress;
    private int needed;
    private int lastStage = -1;

    public BreakSoftBlockGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public static boolean isSoft(BlockState state) {
        return state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_FENCES)
                || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_TRAPDOORS) || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.WOOL)
                || state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)
                || state.is(Blocks.HAY_BLOCK) || state.is(ModBlocks.SALT_CRUST.get());
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.mob.getTarget();
        if (target == null || this.mob.tickCount % 10 != 0 || this.mob.distanceToSqr(target) < 4.0
                || !EventHooks.canEntityGrief(this.mob.level(), this.mob)) {
            return false;
        }
        if (!this.mob.getNavigation().isDone() && !this.mob.getNavigation().isStuck()) {
            return false; // still has a way through
        }
        Direction towards = Direction.getNearest(target.getX() - this.mob.getX(), 0.0, target.getZ() - this.mob.getZ());
        for (int dy : new int[]{1, 0}) {
            BlockPos pos = this.mob.blockPosition().relative(towards).above(dy);
            if (isSoft(this.mob.level().getBlockState(pos))) {
                this.block = pos;
                return true;
            }
        }
        return false;
    }

    @Override
    public void start() {
        Level level = this.mob.level();
        float hardness = level.getBlockState(this.block).getDestroySpeed(level, this.block);
        this.needed = Math.max(MIN_TICKS, (int) (hardness * 60.0F));
        this.progress = 0;
        this.lastStage = -1;
        this.mob.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return this.block != null && this.progress < this.needed && this.mob.getTarget() != null
                && isSoft(this.mob.level().getBlockState(this.block))
                && this.mob.distanceToSqr(this.block.getCenter()) < 9.0;
    }

    @Override
    public void tick() {
        Level level = this.mob.level();
        this.mob.getLookControl().setLookAt(this.block.getCenter());
        if (++this.progress % 10 == 0) {
            this.mob.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, this.block, SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR, this.mob.getSoundSource(),
                    0.6F, 0.7F + this.mob.getRandom().nextFloat() * 0.3F);
        }
        int stage = this.progress * 10 / this.needed;
        if (stage != this.lastStage) {
            level.destroyBlockProgress(this.mob.getId(), this.block, stage);
            this.lastStage = stage;
        }
        if (this.progress >= this.needed) {
            level.destroyBlock(this.block, true, this.mob);
        }
    }

    @Override
    public void stop() {
        if (this.block != null) {
            this.mob.level().destroyBlockProgress(this.mob.getId(), this.block, -1);
        }
        this.block = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
