package team.creative.cmdcam.client.mixin.fabric;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import team.creative.cmdcam.client.CamEventHandlerClient;

import net.minecraft.client.Camera;
import net.minecraft.util.Mth;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @ModifyReturnValue(method = {"calculateHudFov", "modifyFovBasedOnDeathOrFluid"}, at = @At("RETURN"))
    private float handleFovModificationEvent(float original) {
        return CamEventHandlerClient.INSTANCE.fov(original);
    }

    @ModifyReturnValue(method = {"calculateFov"}, at = @At(value = "RETURN", ordinal = 1))
    private float handleFovModificationEvent2(float original) {
        return CamEventHandlerClient.INSTANCE.fov(original);
    }

    @Unique private float parcool$roll;

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", ordinal = 1))
    private void setupRotationFromEvent(Camera instance, float yRot, float xRot, Operation<Void> original) {
        original.call(instance, yRot, xRot);
        this.parcool$roll = CamEventHandlerClient.INSTANCE.cameraRoll();
    }

    @ModifyArg(method = "setRotation", at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"), index = 2)
    private float useCustomRoll(float angleY) {
        return angleY + (-this.parcool$roll * Mth.DEG_TO_RAD);
    }
}
