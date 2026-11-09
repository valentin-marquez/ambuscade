package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.entity.MortarShell;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArrowRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;

/** Crossed flat planes like an arrow, so it noses over along its arc, drawn about twice an arrow's size. */
public class MortarShellRenderer extends ArrowRenderer<MortarShell, ArrowRenderState> {
    private static final Identifier TEXTURE = Ambuscade.id("textures/entity/mortar_shell.png");

    public MortarShellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void submit(ArrowRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.scale(1.8F, 1.8F, 1.8F);
        super.submit(state, pose, collector, camera);
        pose.popPose();
    }

    @Override
    protected Identifier getTextureLocation(ArrowRenderState state) {
        return TEXTURE;
    }

    @Override
    public ArrowRenderState createRenderState() {
        return new ArrowRenderState();
    }
}
