package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.inventory.AbstractContainerMenu;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.Containers;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SentryBlock extends BaseEntityBlock {
    public static final MapCodec<SentryBlock> CODEC = simpleCodec(SentryBlock::new);
    public static final EnumProperty<SentryTier> TIER = EnumProperty.create("tier", SentryTier.class);
    /** Endless ammo: fires plain arrows forever and has no magazine. */
    public static final BooleanProperty ENDLESS = BooleanProperty.create("endless");
    /** The direction it patrols when nobody is in range; it still turns all the way round to shoot. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 15, 14);

    public SentryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TIER, SentryTier.IRON).setValue(ENDLESS, false).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TIER, ENDLESS, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SentryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, AmbuscadeRegistry.SENTRY_ENTITY.get(), level.isClientSide() ? SentryBlockEntity::clientTick : SentryBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof SentryBlockEntity sentry) sentry.setOwner(player);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(ENDLESS)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.ambuscade.sentry.no_magazine"));
        } else if (!level.isClientSide() && level.getBlockEntity(pos) instanceof SentryBlockEntity sentry) {
            player.openMenu(sentry);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction side) {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(AmbuscadeRegistry.PERIMETER_LEDGER.get())) return InteractionResult.PASS;   // let the ledger link it
        if (!stack.is(AmbuscadeRegistry.UPGRADE_MODULE.get())) return super.useItemOn(stack, state, level, pos, player, hand, hit);
        SentryUpgrade module = stack.getOrDefault(AmbuscadeRegistry.UPGRADE.get(), SentryUpgrade.GOLD);
        if (module == SentryUpgrade.ENDLESS) return installEndless(stack, state, level, pos, player);
        SentryTier next = state.getValue(TIER).next();
        if (next == null || module.tier != next) {
            if (level.isClientSide()) return InteractionResult.FAIL;
            player.sendOverlayMessage(next == null ? Component.translatable("message.ambuscade.sentry.max")
                    : Component.translatable("message.ambuscade.sentry.needs", Component.translatable("item.ambuscade.upgrade_module." + next.getSerializedName())));
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(TIER, next), Block.UPDATE_ALL);
            stack.consume(1, player);
            server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            server.sendParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.4, 0.4, 0.4, 0.1);
        }
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult installEndless(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (state.getValue(ENDLESS)) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.ambuscade.sentry.already_endless"));
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            if (server.getBlockEntity(pos) instanceof SentryBlockEntity sentry) {   // the magazine goes away: hand back what was in it
                Containers.dropContents(server, pos, sentry);
                sentry.clearContent();
            }
            server.setBlock(pos, state.setValue(ENDLESS, true), Block.UPDATE_ALL);
            stack.consume(1, player);
            server.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.7F);
            server.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 16, 0.4, 0.4, 0.4, 0.05);
        }
        return InteractionResult.SUCCESS;
    }
}
