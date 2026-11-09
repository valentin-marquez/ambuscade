package com.nozz.ambuscade.block;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A chest with a keypad. Both halves of a double chest carry the same lock, so either half alone stays locked. */
public class LockedChestBlockEntity extends ChestBlockEntity implements KeypadHolder {
    private final KeypadLock lock = new KeypadLock();

    public LockedChestBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.LOCKED_CHEST_ENTITY.get(), pos, state);
    }

    @Override
    public KeypadLock keypad() {
        return lock;
    }

    @Override
    public void keypadChanged() {
        sync();
        if (partner() instanceof LockedChestBlockEntity other) {
            other.lock.copyFrom(lock);
            other.sync();
        }
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    private Object partner() {
        BlockState state = getBlockState();
        if (level == null || state.getValue(ChestBlock.TYPE) == ChestType.SINGLE) return null;
        return level.getBlockEntity(ChestBlock.getConnectedBlockPos(worldPosition, state));
    }

    @Override
    public void unlock(ServerLevel level, ServerPlayer player) {
        if (getBlockState().getBlock() instanceof LockedChestBlock block) block.openFor(getBlockState(), level, worldPosition, player);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag(), keypad = new CompoundTag();   // no items: clients never needed them
        lock.writeClient(keypad);
        tag.put("keypad", keypad);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        lock.save(output.child("keypad"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("keypad").ifPresent(lock::load);
    }
}
