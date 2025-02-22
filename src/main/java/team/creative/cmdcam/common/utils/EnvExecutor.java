package team.creative.cmdcam.common.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import java.util.function.Supplier;

public final class EnvExecutor {

    public static void safeRunWhenOn(EnvType type, Supplier<Runnable> supplier){
        if(FabricLoader.getInstance().getEnvironmentType() == type){
            supplier.get().run();
        }
    }
    public static <T> T safeCallWhenOn(Supplier<Supplier<T>> client, Supplier<Supplier<T>> server){
        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT){
            return client.get().get();
        }

        if(FabricLoader.getInstance().getEnvironmentType() == EnvType.SERVER){
            return server.get().get();
        }

        return null;
    }
    public static <T> T safeCallWhenOn(EnvType type, Supplier<Supplier<T>> supplier){
        if(FabricLoader.getInstance().getEnvironmentType() == type){
            return supplier.get().get();
        }

        return null;
    }
}
