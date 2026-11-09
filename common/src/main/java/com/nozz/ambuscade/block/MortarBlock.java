package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MortarBlock extends BaseEntityBlock {
    public static final MapCodec<MortarBlock> CODEC = simpleCodec(MortarBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public MortarBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MortarBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, AmbuscadeRegistry.MORTAR_ENTITY.get(), MortarBlockEntity::clientTick) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    /** One shell per rising edge, like a dispenser. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean moved) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(TRIGGERED)) return;
        level.setBlock(pos, state.setValue(TRIGGERED, powered), Block.UPDATE_CLIENTS);
        if (powered && level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof MortarBlockEntity mortar) mortar.fire(server);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(AmbuscadeRegistry.FIRE_ORDER.get())) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        if (!(level.getBlockEntity(pos) instanceof MortarBlockEntity mortar)) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        GlobalPos order = stack.get(AmbuscadeRegistry.FIRE_TARGET.get());
        if (order == null) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.mortar.blank_order"));
        } else if (!mortar.mayAim(player)) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.ledger.not_yours"));
        } else if (order.dimension() != level.dimension()) {
            player.sendOverlayMessage(Component.translatable("message.ambuscade.mortar.other_dimension"));
        } else {
            mortar.aimAt(order.pos());
            BlockPos t = order.pos();
            String key = switch (mortar.check()) {
                case OUT_OF_RANGE -> "message.ambuscade.mortar.out_of_range";
                case NO_SKY -> "message.ambuscade.mortar.no_sky";
                default -> "message.ambuscade.mortar.aimed";
            };
            player.sendOverlayMessage(Component.translatable(key, t.getX(), t.getY(), t.getZ()));
            level.playSound(null, pos, SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 1.0F, 0.6F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof MortarBlockEntity mortar) mortar.setOwner(player);
    }
}
