package com.nozz.ambuscade.fabric;

import java.util.function.BiFunction;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class PlatformImpl {
    public static <T extends BlockEntity> BlockEntityType<T> blockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block... blocks) {
        return FabricBlockEntityTypeBuilder.<T>create(factory::apply, blocks).build();
    }
}
