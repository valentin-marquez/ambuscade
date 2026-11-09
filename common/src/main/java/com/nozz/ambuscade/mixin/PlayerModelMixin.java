package com.nozz.ambuscade.mixin;

import com.nozz.ambuscade.AmbuscadeRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Carrying a heavy item: both arms reach forward and down to the sides of the load, like hugging a crate. */
@Mixin(PlayerModel.class)
abstract class PlayerModelMixin {
    private static final float REACH = -0.85F, CLOSE = -0.22F;

    @Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
    private void ambuscade$carry(AvatarRenderState state, CallbackInfo ci) {
        if (!(state.getMainHandItemStack().is(AmbuscadeRegistry.CARRIED))) return;
        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;
        pose(model.rightArm, -CLOSE);
        pose(model.leftArm, CLOSE);
    }

    private static void pose(ModelPart arm, float yaw) {
        arm.xRot = REACH;
        arm.yRot = yaw;
        arm.zRot = 0.0F;
    }
}
