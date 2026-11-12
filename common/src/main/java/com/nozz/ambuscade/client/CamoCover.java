package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.block.Camouflage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Draws a camouflaged trap as the block beneath it squashed to one pixel, with the trap's own light and tint. */
public final class CamoCover {
    private CamoCover() {
    }

    /** Draws the cover and returns true, or returns false when the trap is uncovered or there is nothing to mimic. */
    public static boolean render(BlockEntity trap, PoseStack pose, MultiBufferSource buffers) {
        Level level = trap.getLevel();
        if (!Camouflage.covered(trap.getBlockState()) || level == null) return false;
        BlockPos pos = trap.getBlockPos();
        BlockState ground = level.getBlockState(pos.below());
        if (ground.isAir() || ground.getRenderShape() != RenderShape.MODEL) return false;
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        pose.pushPose();
        pose.scale(1, 1 / 16F, 1);
        // lit and tinted where the trap is, with the same texture variant as the real ground block
        blocks.getModelRenderer().tesselateBlock(level, blocks.getBlockModel(ground), ground, pos, pose,
                buffers.getBuffer(ItemBlockRenderTypes.getMovingBlockRenderType(ground)), false, RandomSource.create(),
                ground.getSeed(pos.below()), OverlayTexture.NO_OVERLAY);
        pose.popPose();
        return true;
    }
}
