package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.block.LandmineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;

/** Mines are plain block models; this only draws the camouflage cover when one hides them. */
public class LandmineRenderer implements BlockEntityRenderer<LandmineBlockEntity> {
    public LandmineRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public void render(LandmineBlockEntity mine, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        CamoCover.render(mine, pose, buffers);
    }
}
