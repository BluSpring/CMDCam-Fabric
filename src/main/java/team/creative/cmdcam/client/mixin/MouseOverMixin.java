package team.creative.cmdcam.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.cmdcam.client.CamEventHandlerClient;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class) // Fabric: wouldn't this be broken in upstream??
public abstract class MouseOverMixin {
    
    @Inject(at = @At("HEAD"), method = "pick(F)V")
    public void pickBefore(CallbackInfo info) {
        CamEventHandlerClient.setupMouseHandlerBefore();
    }
    
    @Inject(at = @At("TAIL"), method = "pick(F)V")
    public void pickAfter(CallbackInfo info) {
        CamEventHandlerClient.setupMouseHandlerAfter();
    }
    
}
