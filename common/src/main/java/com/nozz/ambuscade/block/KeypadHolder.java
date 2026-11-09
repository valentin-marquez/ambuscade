package com.nozz.ambuscade.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** A block entity with a keypad: a locked door or a locked chest. */
public interface KeypadHolder {
    KeypadLock keypad();

    BlockPos getBlockPos();

    /** Called after every change to the lock: save it, send the owner to clients, mirror it to a chest's other half. */
    void keypadChanged();

    /** The right code was typed. */
    void unlock(ServerLevel level, ServerPlayer player);
}
