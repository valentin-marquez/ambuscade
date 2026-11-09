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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The lower half of a locked door: holds its keypad. */
public class KeypadDoorBlockEntity extends BlockEntity implements KeypadHolder {
    private final KeypadLock lock = new KeypadLock();

    public KeypadDoorBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.KEYPAD_DOOR_ENTITY.get(), pos, state);
    }

    @Override
    public KeypadLock keypad() {
        return lock;
    }

    @Override
    public void keypadChanged() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void unlock(ServerLevel level, ServerPlayer player) {
        if (getBlockState().getBlock() instanceof KeypadDoorBlock block) block.unlock(level, worldPosition, player);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        lock.writeClient(tag);
        return tag;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        lock.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        lock.load(input);
    }
}
