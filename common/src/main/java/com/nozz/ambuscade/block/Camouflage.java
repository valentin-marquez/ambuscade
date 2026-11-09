package com.nozz.ambuscade.block;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A camouflage net over a trap: it is drawn as a one-pixel layer of the block beneath and outlined like a carpet.
 *  The trap works as before; the raised pixel is the only tell. */
public final class Camouflage {
    public static final BooleanProperty CAMOUFLAGED = BooleanProperty.create("camouflaged");
    static final VoxelShape COVER = Block.box(0, 0, 0, 16, 1, 16);

    private Camouflage() {
    }

    public static boolean covered(BlockState state) {
        return state.hasProperty(CAMOUFLAGED) && state.getValue(CAMOUFLAGED);
    }

    public static boolean cover(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(CAMOUFLAGED) || state.getValue(CAMOUFLAGED)) return false;
        if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP)) return false;
        level.setBlock(pos, state.setValue(CAMOUFLAGED, true), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_PLACE, SoundSource.BLOCKS, 1.0F, 0.9F);
        return true;
    }

    /** Sneak-right-click with an empty hand pulls the net off and hands it back. */
    static boolean tryUncover(BlockState state, Level level, BlockPos pos, Player player) {
        if (!covered(state) || !player.isSecondaryUseActive()) return false;
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(CAMOUFLAGED, false), Block.UPDATE_ALL);
            Block.popResource(level, pos, new ItemStack(AmbuscadeRegistry.CAMOUFLAGE_NET.get()));
            level.playSound(null, pos, SoundEvents.AZALEA_LEAVES_BREAK, SoundSource.BLOCKS, 1.0F, 1.1F);
        }
        return true;
    }
}
