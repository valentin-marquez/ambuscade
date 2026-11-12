package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.BlockHitResult;

/** A door that ignores redstone and hands: it opens for a few seconds when someone types its code on the keypad. */
public class KeypadDoorBlock extends DoorBlock implements EntityBlock {
    // the codec always builds an iron-sounding door; locked doors are created in code, never from data
    public static final MapCodec<KeypadDoorBlock> CODEC = simpleCodec(p -> new KeypadDoorBlock(BlockSetType.IRON, p));
    static final int OPEN_TICKS = 60;

    /** The door keeps its own sounds, but nothing opens it by hand or wind charge, so villagers and zombies leave it alone. */
    public KeypadDoorBlock(BlockSetType type, Properties properties) {
        super(locked(type), properties);
    }

    private static BlockSetType locked(BlockSetType t) {
        return new BlockSetType(t.name(), false, false, t.canButtonBeActivatedByArrows(), t.pressurePlateSensitivity(), t.soundType(),
                t.doorClose(), t.doorOpen(), t.trapdoorClose(), t.trapdoorOpen(), t.pressurePlateClickOff(), t.pressurePlateClickOn(),
                t.buttonClickOff(), t.buttonClickOn());
    }

    @Override
    public MapCodec<KeypadDoorBlock> codec() {
        return CODEC;
    }

    static BlockPos lower(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new KeypadDoorBlockEntity(pos, state) : null;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState state = super.getStateForPlacement(ctx);
        return state == null ? null : state.setValue(OPEN, false).setValue(POWERED, false);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof KeypadDoorBlockEntity door) {
            door.keypad().setOwner(player);
            door.keypadChanged();
        }
    }

    /** Mining the floor under it must not knock it loose. */
    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(lower(state, pos)) instanceof KeypadDoorBlockEntity door && !door.keypad().ownedBy(player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moved) {
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(lower(state, pos)) instanceof KeypadDoorBlockEntity door) {
            Keypad.open(sp, door);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Swaps a vanilla door for its locked twin in place, keeping how it hangs. Returns the lower half's position. */
    public static BlockPos lock(Level level, BlockPos pos, BlockState vanilla, KeypadDoorBlock locked) {
        BlockPos lower = vanilla.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        BlockState state = locked.defaultBlockState().setValue(FACING, vanilla.getValue(FACING)).setValue(HINGE, vanilla.getValue(HINGE));
        // skip shape updates, or each half would pop the other as soon as it stops matching
        level.setBlock(lower, state.setValue(HALF, DoubleBlockHalf.LOWER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        level.setBlock(lower.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        return lower;
    }

    void unlock(ServerLevel level, BlockPos lower, Player player) {
        BlockState state = level.getBlockState(lower);
        if (!state.is(this)) return;
        setOpen(player, level, state, lower, true);
        level.scheduleTick(lower, this, OPEN_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isOpen(state)) setOpen(null, level, state, pos, false);
    }
}
