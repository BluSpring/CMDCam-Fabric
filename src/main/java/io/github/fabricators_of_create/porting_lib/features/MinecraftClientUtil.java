package io.github.fabricators_of_create.porting_lib.features;

import io.github.fabricators_of_create.porting_lib.mixins.client.MinecraftAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public final class MinecraftClientUtil {
    private MinecraftClientUtil() {
    }

    public static float getRenderPartialTicksPaused(Minecraft minecraft) {
        return get(minecraft).port_lib$pausePartialTick();
    }

    private static MinecraftAccessor get(Minecraft minecraft) {
        return (MinecraftAccessor) minecraft;
    }
}
