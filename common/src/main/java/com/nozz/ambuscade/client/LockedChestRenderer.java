package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.block.LockedChestBlockEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The vanilla chest with its latch swapped for the keypad box. A double chest gets one keypad, centred on the seam. */
public class LockedChestRenderer implements BlockEntityRenderer<LockedChestBlockEntity, ChestRenderState> {
    private static final SpriteId KEYPAD = new SpriteId(TextureAtlas.LOCATION_BLOCKS, Ambuscade.id("block/chest_keypad"));
    private final SpriteGetter sprites;
    private final MultiblockChestResources<ChestModel> chests;
    private final KeypadModel single, seam;
    private final boolean xmas = ChestRenderer.xmasTextures();

    public LockedChestRenderer(BlockEntityRendererProvider.Context ctx) {
        sprites = ctx.sprites();
        chests = ChestRenderer.LAYERS.map(layer -> {
            ModelPart root = ctx.bakeLayer(layer);
            root.getChild("lock").visible = false;
            return new ChestModel(root);
        });
        single = new KeypadModel(6.5F);
        seam = new KeypadModel(14.5F);   // the right half's model: its seam is at x = 16
    }

    @Override
    public ChestRenderState createRenderState() {
        return new ChestRenderState();
    }

    @Override
    public void extractRenderState(LockedChestBlockEntity chest, ChestRenderState state, float partialTicks, Vec3 camera,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(chest, state, partialTicks, camera, breakProgress);
        BlockState block = chest.getBlockState();
        state.type = block.getValue(ChestBlock.TYPE);
        state.facing = block.getValue(ChestBlock.FACING);
        state.material = xmas ? ChestRenderState.ChestMaterialType.CHRISTMAS : ChestRenderState.ChestMaterialType.REGULAR;
        DoubleBlockCombiner.NeighborCombineResult<? extends net.minecraft.world.level.block.entity.ChestBlockEntity> combined =
                chest.getLevel() != null && block.getBlock() instanceof ChestBlock cb
                        ? cb.combine(block, chest.getLevel(), chest.getBlockPos(), true) : DoubleBlockCombiner.Combiner::acceptNone;
        state.open = combined.apply(ChestBlock.opennessCombiner(chest)).get(partialTicks);
        if (state.type != ChestType.SINGLE) state.lightCoords = combined.apply(new BrightnessCombiner<>()).applyAsInt(state.lightCoords);
    }

    @Override
    public void submit(ChestRenderState state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera) {
        pose.pushPose();
        pose.mulPose(ChestRenderer.modelTransformation(state.facing));
        float open = 1.0F - state.open;
        open = 1.0F - open * open * open;
        out.submitModel(chests.select(state.type), open, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
                Sheets.chooseSprite(state.material, state.type), sprites, 0, state.breakProgress);
        KeypadModel keypad = state.type == ChestType.SINGLE ? single : state.type == ChestType.RIGHT ? seam : null;
        if (keypad != null) {
            out.submitModel(keypad, open, pose, state.lightCoords, OverlayTexture.NO_OVERLAY, -1, KEYPAD, sprites, 0, state.breakProgress);
        }
        pose.popPose();
    }

    /** The door keypad's 3x6 steel box, hung where the latch was and swinging with the lid. */
    private static final class KeypadModel extends Model<Float> {
        private final ModelPart keypad;

        KeypadModel(float x) {
            super(bake(x), RenderTypes::entityCutoutCull);
            keypad = root().getChild("keypad");
        }

        private static ModelPart bake(float x) {
            MeshDefinition mesh = new MeshDefinition();
            // same pivot as the vanilla lid and latch; the box sits 1 px proud of the front, 6 px tall across the lid seam
            mesh.getRoot().addOrReplaceChild("keypad", CubeListBuilder.create().texOffs(0, 0).addBox(x, -3.0F, 14.0F, 3.0F, 6.0F, 1.0F),
                    PartPose.offset(0.0F, 9.0F, 1.0F));
            return LayerDefinition.create(mesh, 16, 16).bakeRoot();
        }

        @Override
        public void setupAnim(Float open) {
            super.setupAnim(open);
            keypad.xRot = -(open * (float) (Math.PI / 2));
        }
    }
}
