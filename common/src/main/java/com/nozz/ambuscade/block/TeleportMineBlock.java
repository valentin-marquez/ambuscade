package com.nozz.ambuscade.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Sends whoever steps on it to the coordinates its owner typed in. Single use. */
public class TeleportMineBlock extends LandmineBlock {
    public static final MapCodec<TeleportMineBlock> CODEC = simpleCodec(TeleportMineBlock::new);

    public TeleportMineBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) {
            mine.setDestination(player.blockPosition());   // default: wherever the owner stood
            MineNetwork.open(player, pos, mine.destination());
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (Camouflage.tryUncover(state, level, pos, player)) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) || !mine.mayConfigure(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) MineNetwork.open(sp, pos, mine.destination() == null ? pos : mine.destination());
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean armed(LandmineBlockEntity mine) {
        return mine.destination() != null;
    }

    @Override
    protected void detonate(ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof LandmineBlockEntity mine) || mine.destination() == null) return;
        LivingEntity victim = mine.victim();
        BlockPos dest = mine.destination();
        level.removeBlock(pos, false);
        if (victim == null || !MineNetwork.valid(level, pos, dest)) return;
        Vec3 from = victim.position(), to = Vec3.atBottomCenterOf(dest);
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1, from.z, 60, 0.4, 0.8, 0.4, 0.4);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 0.8F);
        victim.teleportTo(to.x, to.y, to.z);
        level.sendParticles(ParticleTypes.PORTAL, to.x, to.y + 1, to.z, 60, 0.4, 0.8, 0.4, 0.4);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0F, 1.2F);
    }
}
