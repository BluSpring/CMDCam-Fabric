package team.creative.cmdcam.client.extensions;

public interface CameraExtension {
    default void cMDCam_Fabric_new$setAnglesInternal(float x, float y) {
        throw new RuntimeException("Should be implemented by the mixin");
    }
}
