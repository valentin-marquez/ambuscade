package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import com.nozz.ambuscade.AmbuscadeRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;

/** A chest that only opens through its keypad. Hoppers see no slots, and only the owner can break it. */
public class LockedChestBlock extends ChestBlock implements WorldlyContainerHolder {
    public static final MapCodec<LockedChestBlock> CODEC = simpleCodec(LockedChestBlock::new);

    public LockedChestBlock(Properties properties) {
        super(properties, AmbuscadeRegistry.LOCKED_CHEST_ENTITY::get);
    }

    @Override
    public MapCodec<LockedChestBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LockedChestBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest) Keypad.open(sp, chest);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** The right code opens the inventory for whoever typed it, once; the next visit asks again. */
    void openFor(BlockState state, ServerLevel level, BlockPos pos, ServerPlayer player) {
        MenuProvider menu = getMenuProvider(state, level, pos);
        if (menu != null) {
            player.openMenu(menu);
            player.awardStat(Stats.CUSTOM.get(Stats.OPEN_CHEST));
        }
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof LockedChestBlockEntity chest && !chest.keypad().ownedBy(player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    /** Hoppers, hopper minecarts and automation that respects sided inventories see a chest with no slots. */
    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        return new Sealed();
    }

    private static final class Sealed extends SimpleContainer implements WorldlyContainer {
        Sealed() {
            super(0);
        }

        @Override
        public int[] getSlotsForFace(Direction direction) {
            return new int[0];
        }

        @Override
        public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction direction) {
            return false;
        }

        @Override
        public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction direction) {
            return false;
        }
    }

    /** Swaps a vanilla chest (both halves if double) for the locked one in place, keeping its items and name. Returns the halves. */
    public static List<BlockPos> lock(ServerLevel level, BlockPos pos, BlockState vanilla, LockedChestBlock locked) {
        List<BlockPos> halves = new ArrayList<>(List.of(pos));
        if (vanilla.getValue(TYPE) != ChestType.SINGLE) halves.add(pos.relative(getConnectedDirection(vanilla)));
        for (BlockPos p : halves) {
            BlockState old = level.getBlockState(p);
            if (!(old.getBlock() instanceof ChestBlock) || !(level.getBlockEntity(p) instanceof Container vanillaChest)) continue;
            CompoundTag data = level.getBlockEntity(p).saveWithoutMetadata(level.registryAccess());
            vanillaChest.clearContent();   // the old chest spills whatever it still holds when replaced; its items are in data now
            BlockState state = locked.defaultBlockState().setValue(FACING, old.getValue(FACING)).setValue(TYPE, old.getValue(TYPE))
                    .setValue(WATERLOGGED, old.getValue(WATERLOGGED));
            // no shape updates: the halves must not split while the other is still vanilla
            level.setBlock(p, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            if (level.getBlockEntity(p) instanceof LockedChestBlockEntity chest) {
                chest.loadWithComponents(data, level.registryAccess());
                chest.setChanged();
            }
        }
        return halves;
    }
}
