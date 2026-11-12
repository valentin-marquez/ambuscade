package com.nozz.ambuscade.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nozz.ambuscade.Ambuscade;
import com.nozz.ambuscade.block.LockedChestBlockEntity;
import java.util.Calendar;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.BrightnessCombiner;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;

/** The vanilla chest with its latch swapped for the keypad box. A double chest gets one keypad, centred on the seam. */
public class LockedChestRenderer implements BlockEntityRenderer<LockedChestBlockEntity> {
    private static final Material KEYPAD = new Material(TextureAtlas.LOCATION_BLOCKS, Ambuscade.id("block/chest_keypad"));
    private final ModelPart[] single, left, right;   // bottom and lid of each vanilla chest layer; the latch is left out
    private final ModelPart keypadSingle, keypadSeam;
    private final boolean xmas;

    public LockedChestRenderer(BlockEntityRendererProvider.Context ctx) {
        single = body(ctx, ModelLayers.CHEST);
        left = body(ctx, ModelLayers.DOUBLE_CHEST_LEFT);
        right = body(ctx, ModelLayers.DOUBLE_CHEST_RIGHT);
        keypadSingle = keypad(6.5F);
        keypadSeam = keypad(14.5F);   // the right half's model: its seam is at x = 16
        Calendar c = Calendar.getInstance();   // the same Christmas check as the vanilla chest renderer
        xmas = c.get(Calendar.MONTH) + 1 == 12 && c.get(Calendar.DAY_OF_MONTH) >= 24 && c.get(Calendar.DAY_OF_MONTH) <= 26;
    }

    private static ModelPart[] body(BlockEntityRendererProvider.Context ctx, ModelLayerLocation layer) {
        ModelPart root = ctx.bakeLayer(layer);
        return new ModelPart[] {root.getChild("bottom"), root.getChild("lid")};
    }

    /** The door keypad's 3x6 steel box, hung where the latch was and swinging with the lid. */
    private static ModelPart keypad(float x) {
        MeshDefinition mesh = new MeshDefinition();
        // same pivot as the vanilla lid and latch; the box sits 1 px proud of the front, 6 px tall across the lid seam
        mesh.getRoot().addOrReplaceChild("keypad", CubeListBuilder.create().texOffs(0, 0).addBox(x, -3.0F, 14.0F, 3.0F, 6.0F, 1.0F),
                PartPose.offset(0.0F, 9.0F, 1.0F));
        return LayerDefinition.create(mesh, 16, 16).bakeRoot().getChild("keypad");
    }

    @Override
    public void render(LockedChestBlockEntity chest, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState block = chest.getBlockState();
        if (!(block.getBlock() instanceof ChestBlock cb)) return;
        ChestType type = block.getValue(ChestBlock.TYPE);
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-block.getValue(ChestBlock.FACING).toYRot()));
        pose.translate(-0.5F, -0.5F, -0.5F);
        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combined = chest.getLevel() != null
                ? cb.combine(block, chest.getLevel(), chest.getBlockPos(), true) : DoubleBlockCombiner.Combiner::acceptNone;
        float open = 1.0F - combined.apply(ChestBlock.opennessCombiner(chest)).get(partialTick);
        open = 1.0F - open * open * open;
        int lit = combined.apply(new BrightnessCombiner<>()).applyAsInt(light);
        float lidRot = -(open * (float) (Math.PI / 2));

        ModelPart[] body = type == ChestType.LEFT ? left : type == ChestType.RIGHT ? right : single;
        VertexConsumer wood = Sheets.chooseMaterial(chest, type, xmas).buffer(buffers, RenderType::entityCutout);
        body[1].xRot = lidRot;
        body[0].render(pose, wood, lit, overlay);
        body[1].render(pose, wood, lit, overlay);
        ModelPart keypad = type == ChestType.SINGLE ? keypadSingle : type == ChestType.RIGHT ? keypadSeam : null;
        if (keypad != null) {
            keypad.xRot = lidRot;
            keypad.render(pose, KEYPAD.buffer(buffers, RenderType::entityCutout), lit, overlay);
        }
        pose.popPose();
    }
}
