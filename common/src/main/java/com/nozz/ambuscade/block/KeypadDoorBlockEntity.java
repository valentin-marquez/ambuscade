package com.nozz.ambuscade.block;

import com.nozz.ambuscade.AmbuscadeRegistry;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The code and its owner. Only the owner reaches clients; only the owner may set the code or break the door. */
public class KeypadDoorBlockEntity extends BlockEntity {
    static final int MAX_TRIES = 3, LOCKOUT_TICKS = 200, MAX_LOCKOUT_TICKS = 72000;

    private UUID owner;
    private String code = "";
    private int failures, lockouts;
    private long lockedUntil;

    public KeypadDoorBlockEntity(BlockPos pos, BlockState state) {
        super(AmbuscadeRegistry.KEYPAD_DOOR_ENTITY.get(), pos, state);
    }

    void setOwner(Player player) {
        owner = player.getUUID();
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    /** An ownerless door (e.g. from /setblock) is claimed by the first player who uses it. */
    boolean isOwner(Player player) {
        if (owner == null) setOwner(player);
        return owner.equals(player.getUUID());
    }

    /** Only the owner can break the door; an ownerless one (from /setblock) is anyone's. */
    boolean ownedBy(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();   // the owner only, so the client knows who may mine it; the code stays on the server
        if (owner != null) tag.store("owner", UUIDUtil.CODEC, owner);
        return tag;
    }

    boolean hasCode() {
        return !code.isEmpty();
    }

    void setCode(String code) {
        this.code = code;
        failures = 0;
        setChanged();
    }

    long lockedFor(long now) {
        return Math.max(0, lockedUntil - now);
    }

    /** True on a match; every three misses in a row lock the keypad, ten seconds at first and doubling up to an hour. */
    boolean tryCode(String attempt, long now) {
        if (attempt.equals(code)) {
            failures = lockouts = 0;
            setChanged();
            return true;
        }
        if (++failures >= MAX_TRIES) {
            failures = 0;
            lockedUntil = now + Math.min((long) LOCKOUT_TICKS << Math.min(lockouts++, 20), MAX_LOCKOUT_TICKS);
        }
        setChanged();
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("code", code);
        output.putInt("failures", failures);
        output.putInt("lockouts", lockouts);
        output.putLong("locked_until", lockedUntil);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        code = input.getStringOr("code", "");
        failures = input.getIntOr("failures", 0);
        lockouts = input.getIntOr("lockouts", 0);
        lockedUntil = input.getLongOr("locked_until", 0L);
    }
}
