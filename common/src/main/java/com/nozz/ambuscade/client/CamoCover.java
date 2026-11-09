package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.block.Camouflage;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Draws a camouflaged trap as the block beneath it squashed to one pixel, with the trap's own light and tint. */
public final class CamoCover {
    private CamoCover() {
    }

    /** Null when the trap is uncovered or there is nothing to mimic. */
    public static MovingBlockRenderState capture(BlockEntity trap) {
        if (!Camouflage.covered(trap.getBlockState()) || !(trap.getLevel() instanceof ClientLevel level)) return null;
        BlockPos pos = trap.getBlockPos();
        BlockState ground = level.getBlockState(pos.below());
        if (ground.isAir()) return null;
        MovingBlockRenderState cover = new MovingBlockRenderState();
        cover.randomSeedPos = pos.below();   // same texture variant as the real ground block
        cover.blockPos = pos;                // lit and tinted where the trap is
        cover.blockState = ground;
        cover.biome = level.getBiome(pos);
        cover.cardinalLighting = level.cardinalLighting();
        cover.lightEngine = level.getLightEngine();
        return cover;
    }

    public static void submit(PoseStack pose, SubmitNodeCollector collector, MovingBlockRenderState cover) {
        pose.pushPose();
        pose.scale(1, 1 / 16F, 1);
        collector.submitMovingBlock(pose, cover);
        pose.popPose();
    }
}
