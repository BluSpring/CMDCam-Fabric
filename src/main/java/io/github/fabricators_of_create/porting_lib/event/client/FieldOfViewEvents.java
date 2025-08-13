package io.github.fabricators_of_create.porting_lib.event.client;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;

public class FieldOfViewEvents {
    public static final Event<Compute> COMPUTE = EventFactory.createArrayBacked(Compute.class, (callbacks) -> (renderer, camera, partialTicks, usedFovSetting, fov) -> {
        for (Compute callback : callbacks) {
            fov = callback.getFov(renderer, camera, partialTicks, usedFovSetting, fov);
        }

        return fov;
    });

    public interface Compute {
        double getFov(GameRenderer var1, Camera var2, double var3, boolean var5, double var6);
    }
}
