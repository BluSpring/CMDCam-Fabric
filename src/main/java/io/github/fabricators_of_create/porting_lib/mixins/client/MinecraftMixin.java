package io.github.fabricators_of_create.porting_lib.mixins.client;

import io.github.fabricators_of_create.porting_lib.features.RenderTickStartCallback;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(FJZ)V", shift = At.Shift.BEFORE))
    private void port_lib$renderTickStart(CallbackInfo ci) {
        RenderTickStartCallback.EVENT.invoker().tick();
    }

}
