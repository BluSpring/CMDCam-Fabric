package team.creative.cmdcam.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.fabricators_of_create.porting_lib.event.client.FieldOfViewEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import team.creative.cmdcam.client.extensions.CameraExtension;
import team.creative.cmdcam.fabric.ComputeCameraAnglesCallback;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;prepareCullFrustum(Lnet/minecraft/world/phys/Vec3;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V", shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILHARD)
    private void am$applyRoll(DeltaTracker deltaTracker, CallbackInfo ci, float partialTicks,
                              boolean shouldRenderBlockOutline, Camera camera, Entity cameraEntity,
                              float adjustedTick, double fov, @Local(ordinal = 1) Matrix4f viewMatrix) {
        ComputeCameraAnglesCallback event = new ComputeCameraAnglesCallback(
                (GameRenderer) (Object) this,
                camera,
                partialTicks,
                camera.getYRot(),
                camera.getXRot(),
                0
        );

        Vector3f forward = camera.getLookVector();

        ComputeCameraAnglesCallback.EVENT.invoker().onComputeCameraAngles(event);
        ((CameraExtension) camera).cMDCam_Fabric_new$setAnglesInternal(event.getYaw(), event.getPitch());
        viewMatrix.rotate((float) Math.toRadians(event.getRoll()), forward);
    }

    @ModifyReturnValue(
            method = "getFov",
            at = @At(value = "RETURN", ordinal = 1) // skip the early exit
    )
    private double port_lib$modifyFov(double fov,
                                      Camera camera, float partialTicks, boolean usedFovSetting) {
        // returns original if not changed, this is safe
        return FieldOfViewEvents.COMPUTE.invoker().getFov((GameRenderer) (Object) this, camera, partialTicks, usedFovSetting, fov);
    }
}
