package com.nozz.ambuscade.item;

import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.Keypad;
import com.nozz.ambuscade.block.KeypadDoorBlock;
import com.nozz.ambuscade.block.KeypadDoorBlockEntity;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Fitted to a vanilla door, it turns it into a locked twin and asks the fitter for a code. */
public class KeypadLockItem extends Item {
    public KeypadLockItem(Properties properties) {
        super(properties);
    }

    /** Doors answer a click before the held item does, so the lock has to step in first or the door just swings. */
    public static void init() {
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) -> {
            ItemStack stack = player.getItemInHand(hand);
            Level level = player.level();
            BlockState state = level.getBlockState(pos);
            RegistrySupplier<KeypadDoorBlock> locked = AmbuscadeRegistry.LOCKED_DOORS.get(state.getBlock());
            if (!(stack.getItem() instanceof KeypadLockItem) || locked == null) return InteractionResult.PASS;
            if (player instanceof ServerPlayer sp) fit(level, sp, pos, state, locked.get(), stack);
            return InteractionResult.SUCCESS;
        });
    }

    private static void fit(Level level, ServerPlayer player, BlockPos pos, BlockState state, KeypadDoorBlock locked, ItemStack stack) {
        BlockPos lower = KeypadDoorBlock.lock(level, pos, state, locked);
        level.playSound(null, lower, SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.BLOCKS, 1.0F, 1.4F);
        stack.consume(1, player);
        if (level.getBlockEntity(lower) instanceof KeypadDoorBlockEntity door) Keypad.claim(player, door);
    }
}
