package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.entity.MortarShell;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Crossed flat planes like an arrow, so it noses over along its arc, drawn about twice an arrow's size. */
public class MortarShellRenderer extends ArrowRenderer<MortarShell> {
    private static final ResourceLocation TEXTURE = Ambuscade.id("textures/entity/mortar_shell.png");

    public MortarShellRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MortarShell shell, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(1.8F, 1.8F, 1.8F);
        super.render(shell, yaw, partialTick, pose, buffers, light);
        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(MortarShell shell) {
        return TEXTURE;
    }
}
