package com.nozz.ambuscade.mixin;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carried items take both arms, so the off hand stays out of the way. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;",
            at = @At("HEAD"), cancellable = true)
    private static void ambuscade$carry(Avatar avatar, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        if (stack.is(AmbuscadeRegistry.CARRIED)) cir.setReturnValue(HumanoidModel.ArmPose.CROSSBOW_HOLD);
    }
}
