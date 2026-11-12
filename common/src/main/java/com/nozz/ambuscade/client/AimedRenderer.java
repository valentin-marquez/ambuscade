package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.nozz.ambuscade.block.Aimed;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/** Sentries and mortars: model and texture chosen by the block entity, yaw and pitch bones turned to its aim. */
public class AimedRenderer<T extends BlockEntity & GeoBlockEntity & Aimed> extends GeoBlockRenderer<T> {
    public AimedRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new Model<>());
    }

    @Override
    protected void rotateBlock(Direction facing, PoseStack poseStack) {
        // aim is computed in world space, so the base is never turned by FACING
    }

    @Override
    public void renderRecursively(PoseStack pose, T aimed, GeoBone bone, RenderType type, MultiBufferSource buffers, VertexConsumer buffer,
                                  boolean isReRender, float partialTick, int light, int overlay, int colour) {
        boolean yaw = bone.getName().equals("yaw");
        if (!yaw && !bone.getName().equals("pitch")) {
            super.renderRecursively(pose, aimed, bone, type, buffers, buffer, isReRender, partialTick, light, overlay, colour);
            return;
        }
        // the model faces north at rest, and a bone's Y rotation reads the same way as our yaw (atan2(-dx, -dz)):
        // checked against targets on all eight compass points; "PI - yaw" only agreed with it due east and west.
        // Added on top of the animated pose for this draw only, then taken off again: bones are shared by every sentry
        float turn = (yaw ? aimed.renderYaw(partialTick) : aimed.renderPitch(partialTick)) * Mth.DEG_TO_RAD;
        if (yaw) bone.setRotY(bone.getRotY() + turn);
        else bone.setRotX(bone.getRotX() + turn);
        super.renderRecursively(pose, aimed, bone, type, buffers, buffer, isReRender, partialTick, light, overlay, colour);
        if (yaw) bone.setRotY(bone.getRotY() - turn);
        else bone.setRotX(bone.getRotX() - turn);
        bone.resetStateChanges();   // or GeckoLib would take the bone for animated and stop easing it back after an animation
    }

    private static final class Model<T extends BlockEntity & GeoBlockEntity & Aimed> extends GeoModel<T> {
        @Override
        public ResourceLocation getModelResource(T aimed) {
            return aimed.model().withPath(p -> "geo/" + p + ".geo.json");
        }

        @Override
        public ResourceLocation getTextureResource(T aimed) {
            return aimed.texture();
        }

        @Override
        public ResourceLocation getAnimationResource(T aimed) {
            return aimed.animation().withPath(p -> "animations/" + p + ".animation.json");
        }
    }
}
