package com.nozz.ambuscade;

import dev.architectury.injectables.annotations.ExpectPlatform;
import java.util.function.BiFunction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class Platform {
    @ExpectPlatform
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        throw new AssertionError();
    }
}
