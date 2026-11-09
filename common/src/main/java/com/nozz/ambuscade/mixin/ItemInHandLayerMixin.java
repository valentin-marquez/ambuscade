package com.nozz.ambuscade.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A carried item hangs off the torso, square in front of the chest, instead of off the right hand. */
@Mixin(ItemInHandLayer.class)
abstract class ItemInHandLayerMixin {
    @Inject(method = "submitArmWithItem(Lnet/minecraft/client/renderer/entity/state/ArmedEntityRenderState;Lnet/minecraft/client/renderer/item/ItemStackRenderState;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
            at = @At("HEAD"), cancellable = true)
    private void ambuscade$carryOnChest(ArmedEntityRenderState state, ItemStackRenderState item, ItemStack stack, HumanoidArm arm,
                                         PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
        if (!(stack.is(AmbuscadeRegistry.CARRIED))) return;
        ci.cancel();
        if (item.isEmpty() || !(((RenderLayer<?, ?>) (Object) this).getParentModel() instanceof HumanoidModel<?> model)) return;
        pose.pushPose();
        model.body.translateAndRotate(pose);
        pose.translate(0.0F, 0.5F, -0.38F);   // chest height, just in front of it; the model's thirdperson display does the rest
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        item.submit(pose, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
        pose.popPose();
    }
}
