package com.nozz.ambuscade.block;

import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** A keypad's owner, code and lockout. Shared by every block that carries a keypad; only the owner ever reaches clients. */
public final class KeypadLock {
    static final int MAX_TRIES = 3, LOCKOUT_TICKS = 200, MAX_LOCKOUT_TICKS = 72000;

    private UUID owner;
    private String code = "";
    private int failures, lockouts;
    private long lockedUntil;

    void setOwner(Player player) {
        owner = player.getUUID();
    }

    boolean hasOwner() {
        return owner != null;
    }

    boolean isOwner(Player player) {
        return player.getUUID().equals(owner);
    }

    /** Only the owner can break it; an ownerless one (from /setblock) is anyone's. */
    boolean ownedBy(Player player) {
        return owner == null || owner.equals(player.getUUID());
    }

    boolean hasCode() {
        return !code.isEmpty();
    }

    void setCode(String code) {
        this.code = code;
        failures = 0;
    }

    long lockedFor(long now) {
        return Math.max(0, lockedUntil - now);
    }

    /** True on a match; every three misses in a row lock the keypad, ten seconds at first and doubling up to an hour. */
    boolean tryCode(String attempt, long now) {
        if (attempt.equals(code)) {
            failures = lockouts = 0;
            return true;
        }
        if (++failures >= MAX_TRIES) {
            failures = 0;
            lockedUntil = now + Math.min((long) LOCKOUT_TICKS << Math.min(lockouts++, 20), MAX_LOCKOUT_TICKS);
        }
        return false;
    }

    void copyFrom(KeypadLock other) {
        owner = other.owner;
        code = other.code;
        failures = other.failures;
        lockouts = other.lockouts;
        lockedUntil = other.lockedUntil;
    }

    /** The owner only, so the client knows who may mine it; the code stays on the server. */
    void writeClient(CompoundTag tag) {
        if (owner != null) tag.store("owner", UUIDUtil.CODEC, owner);
    }

    void save(ValueOutput output) {
        if (owner != null) output.store("owner", UUIDUtil.CODEC, owner);
        output.putString("code", code);
        output.putInt("failures", failures);
        output.putInt("lockouts", lockouts);
        output.putLong("locked_until", lockedUntil);
    }

    void load(ValueInput input) {
        owner = input.read("owner", UUIDUtil.CODEC).orElse(null);
        code = input.getStringOr("code", "");
        failures = input.getIntOr("failures", 0);
        lockouts = input.getIntOr("lockouts", 0);
        lockedUntil = input.getLongOr("locked_until", 0L);
    }
}
