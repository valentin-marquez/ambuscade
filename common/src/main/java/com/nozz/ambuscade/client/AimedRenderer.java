package com.nozz.ambuscade.client;

import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.block.Aimed;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Sentries and mortars: model and texture chosen by the block entity, yaw and pitch bones turned to its aim. */
public class AimedRenderer<T extends BlockEntity & GeoBlockEntity & Aimed> extends GeoBlockRenderer<T, BlockEntityRenderState> {
    private static final DataTicket<Identifier> MODEL = DataTicket.create("ambuscade_aimed_model", Identifier.class);
    private static final DataTicket<Identifier> TEXTURE = DataTicket.create("ambuscade_aimed_texture", Identifier.class);
    private static final DataTicket<Float> YAW = DataTicket.create("ambuscade_aimed_yaw", Float.class);
    private static final DataTicket<Float> PITCH = DataTicket.create("ambuscade_aimed_pitch", Float.class);

    public AimedRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new Model<>());
    }

    // BlockEntityRenderState only becomes a GeoRenderState through GeckoLib's injected interface, which the
    // common module cannot see; hence the casts and raw RenderPassInfo below
    @Override
    public void addRenderData(T aimed, Void relatedObject, BlockEntityRenderState state, float partialTick) {
        GeoRenderState geo = (GeoRenderState) state;
        geo.addGeckolibData(MODEL, aimed.model());
        geo.addGeckolibData(TEXTURE, aimed.texture());
        geo.addGeckolibData(YAW, aimed.renderYaw(partialTick));
        geo.addGeckolibData(PITCH, aimed.renderPitch(partialTick));
    }

    @Override
    @SuppressWarnings("rawtypes")
    protected void tryRotateByBlockstate(RenderPassInfo info, PoseStack poseStack) {
        // aim is computed in world space, so the base is never turned by FACING
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void adjustModelBonesForRender(RenderPassInfo info, BoneSnapshots snapshots) {
        float yaw = (Float) info.getOrDefaultGeckolibData(YAW, 0F) * Mth.DEG_TO_RAD;
        float pitch = (Float) info.getOrDefaultGeckolibData(PITCH, 0F) * Mth.DEG_TO_RAD;
        // the model faces north at rest, and a bone's Y rotation reads the same way as our yaw (atan2(-dx, -dz)):
        // checked against targets on all eight compass points; "PI - yaw" only agreed with it due east and west
        snapshots.ifPresent("yaw", bone -> bone.setRotY(bone.getRotY() + yaw));
        snapshots.ifPresent("pitch", bone -> bone.setRotX(bone.getRotX() + pitch));
    }

    private static final class Model<T extends BlockEntity & GeoBlockEntity & Aimed> extends GeoModel<T> {
        @Override
        public Identifier getModelResource(GeoRenderState state) {
            return state.getGeckolibData(MODEL);
        }

        @Override
        public Identifier getTextureResource(GeoRenderState state) {
            return state.getGeckolibData(TEXTURE);
        }

        @Override
        public Identifier getAnimationResource(T aimed) {
            return aimed.animation();
        }
    }
}
