package com.nozz.ambuscade.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.BearTrapBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;

/** The animated trap, or only its camouflage cover when a net hides it. */
public class BearTrapRenderer extends GeoBlockRenderer<BearTrapBlockEntity, BlockEntityRenderState> {
    private record Cover(MovingBlockRenderState state) {
    }

    private static final DataTicket<Cover> COVER = DataTicket.create("ambuscade_camo_cover", Cover.class);

    public BearTrapRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, AmbuscadeRegistry.BEAR_TRAP_ENTITY.get());
    }

    @Override
    public void extractRenderState(BearTrapBlockEntity trap, BlockEntityRenderState state, float partialTick, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay crumbling) {
        super.extractRenderState(trap, state, partialTick, camera, crumbling);
        ((GeoRenderState) state).addGeckolibData(COVER, new Cover(CamoCover.capture(trap)));   // always set: render states are reused
    }

    @Override
    public void submit(BlockEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Cover cover = ((GeoRenderState) state).getOrDefaultGeckolibData(COVER, new Cover(null));
        if (cover.state() != null) CamoCover.submit(pose, collector, cover.state());
        else super.submit(state, pose, collector, camera);
    }
}
