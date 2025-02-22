package io.github.fabricators_of_create.porting_lib.mixins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.fabricators_of_create.porting_lib.features.CameraExtensions;
import io.github.fabricators_of_create.porting_lib.features.CameraSetupCallback;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow
    @Final
    private Camera mainCamera;

    @Inject(method = "renderLevel", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/client/Camera;setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V"))
    private void port_lib$modifyCameraInfo(float partialTicks, long l, PoseStack poseStack, CallbackInfo ci) {
        Camera cam = this.mainCamera;
        CameraSetupCallback.CameraInfo info = new CameraSetupCallback.CameraInfo((GameRenderer) (Object) this, cam, partialTicks, cam.getYRot(), cam.getXRot(), 0);
        CameraSetupCallback.EVENT.invoker().onCameraSetup(info);
        ((CameraExtensions) cam).setAnglesInternal(info.yaw, info.pitch);
        poseStack.mulPose(Axis.ZP.rotationDegrees(info.roll));
    }
}
