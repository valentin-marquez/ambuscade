package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.AmbuscadeRegistry;
import com.nozz.ambuscade.block.BearTrapBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** The animated trap, or only its camouflage cover when a net hides it. */
public class BearTrapRenderer extends GeoBlockRenderer<BearTrapBlockEntity> {
    public BearTrapRenderer(BlockEntityRendererProvider.Context ctx) {
        super(AmbuscadeRegistry.BEAR_TRAP_ENTITY.get());
    }

    @Override
    public void render(BearTrapBlockEntity trap, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (!CamoCover.render(trap, pose, buffers)) super.render(trap, partialTick, pose, buffers, light, overlay);
    }
}
