package io.github.fabricators_of_create.porting_lib.mixins.client;

import io.github.fabricators_of_create.porting_lib.features.EntityEvents;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Inject(method = "addEntity", at = @At("HEAD"), cancellable = true)
    public void port_lib$addEntityEvent(Entity entity, CallbackInfo ci) {
        if (EntityEvents.ON_JOIN_WORLD.invoker().onJoinWorld(entity, (Level) (Object) this, false))
            ci.cancel();
    }
}
