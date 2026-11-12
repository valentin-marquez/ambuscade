package com.nozz.ambuscade.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A carried item hangs off the torso, square in front of the chest, instead of off the right hand. */
@Mixin(ItemInHandLayer.class)
abstract class ItemInHandLayerMixin {
    @Shadow @Final private ItemInHandRenderer itemInHandRenderer;

    @Inject(method = "renderArmWithItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lnet/minecraft/world/entity/HumanoidArm;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void ambuscade$carryOnChest(LivingEntity entity, ItemStack stack, ItemDisplayContext context, HumanoidArm arm,
                                         PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!stack.is(AmbuscadeRegistry.CARRIED)) return;
        ci.cancel();
        if (!(((RenderLayer<?, ?>) (Object) this).getParentModel() instanceof HumanoidModel<?> model)) return;
        pose.pushPose();
        model.body.translateAndRotate(pose);
        pose.translate(0.0F, 0.5F, -0.38F);   // chest height, just in front of it; the model's thirdperson display does the rest
        pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        itemInHandRenderer.renderItem(entity, stack, context, arm == HumanoidArm.LEFT, pose, buffers, light);
        pose.popPose();
    }
}
