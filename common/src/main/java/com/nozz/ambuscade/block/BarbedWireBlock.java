package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import com.nozz.ambuscade.Ambuscade;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BarbedWireBlock extends Block {
    public static final MapCodec<BarbedWireBlock> CODEC = simpleCodec(BarbedWireBlock::new);
    public static final BooleanProperty POST = BooleanProperty.create("post");
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;
    private static final Map<Direction, BooleanProperty> SIDES = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST, BlockStateProperties.WEST);

    private static final VoxelShape POST_SHAPE = Block.box(6, 0, 6, 10, 16, 10);
    private static final Map<Direction, VoxelShape> SIDE_SHAPES = Map.of(
            Direction.NORTH, Block.box(7, 0, 0, 9, 15, 8), Direction.SOUTH, Block.box(7, 0, 8, 9, 15, 16),
            Direction.WEST, Block.box(0, 0, 7, 8, 15, 9), Direction.EAST, Block.box(8, 0, 7, 16, 15, 9));

    public BarbedWireBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any().setValue(POST, true).setValue(UP, false).setValue(DOWN, false);
        for (BooleanProperty side : SIDES.values()) state = state.setValue(side, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POST, UP, DOWN).add(SIDES.values().toArray(BooleanProperty[]::new));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connect(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        return connect(state, level, pos);
    }

    private BlockState connect(BlockState state, LevelReader level, BlockPos pos) {
        for (Map.Entry<Direction, BooleanProperty> side : SIDES.entrySet()) {
            boolean attach = level.getBlockState(pos.relative(side.getKey())).getBlock() instanceof BarbedWireBlock;
            state = state.setValue(side.getValue(), attach);
        }
        state = state.setValue(UP, level.getBlockState(pos.above()).getBlock() instanceof BarbedWireBlock)
                .setValue(DOWN, level.getBlockState(pos.below()).getBlock() instanceof BarbedWireBlock);
        return isStraight(state) ? state : state.setValue(POST, true);
    }

    // only straight runs may drop the post: corners, tees and ends always keep it
    private static boolean isStraight(BlockState s) {
        boolean ns = s.getValue(BlockStateProperties.NORTH) && s.getValue(BlockStateProperties.SOUTH);
        boolean ew = s.getValue(BlockStateProperties.EAST) && s.getValue(BlockStateProperties.WEST);
        boolean anyNs = s.getValue(BlockStateProperties.NORTH) || s.getValue(BlockStateProperties.SOUTH);
        boolean anyEw = s.getValue(BlockStateProperties.EAST) || s.getValue(BlockStateProperties.WEST);
        return (ns && !anyEw) || (ew && !anyNs);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!isStraight(state)) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.cycle(POST), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        VoxelShape shape = state.getValue(POST) ? POST_SHAPE : Shapes.empty();
        for (Map.Entry<Direction, BooleanProperty> side : SIDES.entrySet()) {
            if (state.getValue(side.getValue())) shape = Shapes.or(shape, SIDE_SHAPES.get(side.getKey()));
        }
        return shape.isEmpty() ? POST_SHAPE : shape;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (!(entity instanceof LivingEntity living)) return;
        entity.makeStuckInBlock(state, new Vec3(0.45, 0.75, 0.45));
        if (level instanceof ServerLevel server && entity.getDeltaMovement().horizontalDistanceSqr() > 1.0E-6) {
            living.hurtServer(server, Ambuscade.damage(server, Ambuscade.BARBED_WIRE_DAMAGE), 1.0F);
        }
    }
}
