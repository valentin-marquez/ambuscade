package com.nozz.ambuscade.mixin;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carrying a heavy item: both arms reach forward and down to the sides of the load, like hugging a crate. */
@Mixin(PlayerModel.class)
abstract class PlayerModelMixin {
    private static final float REACH = -0.85F, CLOSE = -0.22F;

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void ambuscade$carry(LivingEntity entity, float limbSwing, float limbSwingAmount, float age, float headYaw, float headPitch, CallbackInfo ci) {
        if (!entity.getMainHandItem().is(AmbuscadeRegistry.CARRIED)) return;
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        pose(model.rightArm, -CLOSE);
        pose(model.leftArm, CLOSE);
        // the sleeves were copied from the arms before this ran; on 1.21.1 they are not children of the arms
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
    }

    private static void pose(ModelPart arm, float yaw) {
        arm.xRot = REACH;
        arm.yRot = yaw;
        arm.zRot = 0.0F;
    }
}
