package com.horrortown.block;

import com.horrortown.registry.ModGear;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 腐泥: black, rotten mud. You sink half a block into it and wade through it slowly, unless you have mud boots
 * on (or boots with Mud Walker); now and then it lets out a bubble of gas.
 */
public class RotMudBlock extends Block {
    public static final MapCodec<RotMudBlock> CODEC = simpleCodec(RotMudBlock::new);
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);
    private static final Vec3 STUCK = new Vec3(0.5, 0.85, 0.5);

    public RotMudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 0.2F;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (!entity.isSpectator() && !(entity.isPassenger()) && entity.getY() < pos.getY() + 0.75
                && !(entity instanceof LivingEntity living && ModGear.walksOnMud(living))) {
            entity.makeStuckInBlock(state, STUCK);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(60) != 0 || !level.getBlockState(pos.above()).isAir()) {
            return;
        }
        double x = pos.getX() + random.nextDouble();
        double z = pos.getZ() + random.nextDouble();
        level.addParticle(ParticleTypes.BUBBLE_POP, x, pos.getY() + 1.0, z, 0.0, 0.02, 0.0);
        if (random.nextInt(4) == 0) {
            level.playLocalSound(x, pos.getY() + 1.0, z, SoundEvents.MUD_STEP, SoundSource.BLOCKS,
                    0.3F, 0.5F + random.nextFloat() * 0.2F, false);
        }
    }
}
