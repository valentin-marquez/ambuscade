package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.block.LandmineBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** Mines are plain block models; this only draws the camouflage cover when one hides them. */
public class LandmineRenderer implements BlockEntityRenderer<LandmineBlockEntity, LandmineRenderer.State> {
    public static class State extends BlockEntityRenderState {
        MovingBlockRenderState cover;
    }

    public LandmineRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LandmineBlockEntity mine, State state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(mine, state, partialTick, camera, crumbling);
        state.cover = CamoCover.capture(mine);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.cover != null) CamoCover.submit(pose, collector, state.cover);
    }
}
