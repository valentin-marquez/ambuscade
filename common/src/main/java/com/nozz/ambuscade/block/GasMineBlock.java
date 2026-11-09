package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Instead of exploding, vents a gas cloud for a while; whitelisted entities breathe freely. */
public class GasMineBlock extends LandmineBlock {
    public static final MapCodec<GasMineBlock> CODEC = simpleCodec(GasMineBlock::new);

    public GasMineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void detonate(ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) {
            mine.startVenting();
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.6F);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, AmbuscadeRegistry.LANDMINE_ENTITY.get(), LandmineBlockEntity::serverTick);
    }
}
