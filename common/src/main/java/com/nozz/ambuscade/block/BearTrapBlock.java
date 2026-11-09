package com.nozz.ambuscade.block;

import net.minecraft.world.InteractionHand;
import com.mojang.serialization.MapCodec;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BearTrapBlock extends BaseEntityBlock {
    public static final MapCodec<BearTrapBlock> CODEC = simpleCodec(BearTrapBlock::new);
    public static final BooleanProperty ARMED = BooleanProperty.create("armed");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 2, 14);
    private static final int HOLD_TICKS = 60;

    public BearTrapBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ARMED, true).setValue(FACING, Direction.NORTH).setValue(Camouflage.CAMOUFLAGED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ARMED, FACING, Camouflage.CAMOUFLAGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BearTrapBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, AmbuscadeRegistry.BEAR_TRAP_ENTITY.get(), BearTrapBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Camouflage.covered(state) ? Camouflage.COVER : SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return dir == Direction.DOWN && !canSurvive(state, level, pos) ? Blocks.AIR.defaultBlockState() : state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof BearTrapBlockEntity trap) {
            trap.setOwner(player);
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (!(level instanceof ServerLevel server) || !state.getValue(ARMED) || !(entity instanceof LivingEntity victim)) return;
        if (!entity.blockPosition().equals(pos)) return;
        if (!(level.getBlockEntity(pos) instanceof BearTrapBlockEntity trap) || !trap.canCatch(victim)) return;

        server.setBlock(pos, state.setValue(ARMED, false), Block.UPDATE_ALL);
        float damage = Mth.clamp(victim.getMaxHealth() * 0.5F, 4.0F, 10.0F);
        victim.hurtServer(server, Ambuscade.damage(server, Ambuscade.BEAR_TRAP_DAMAGE), damage);
        trap.hold(victim, HOLD_TICKS);
        server.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.6F);
        trap.triggerAnim(BearTrapBlockEntity.CONTROLLER, "snap");
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(AmbuscadeRegistry.CAMOUFLAGE_NET.get())) return InteractionResult.PASS;   // let the net cover it
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (Camouflage.tryUncover(state, level, pos, player)) return InteractionResult.SUCCESS;
        if (state.getValue(ARMED)) return InteractionResult.PASS;
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof BearTrapBlockEntity trap) {
            level.setBlock(pos, state.setValue(ARMED, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.IRON_TRAPDOOR_OPEN, SoundSource.BLOCKS, 1.0F, 0.8F);
            trap.triggerAnim(BearTrapBlockEntity.CONTROLLER, "arm");
        }
        return InteractionResult.SUCCESS;
    }
}
