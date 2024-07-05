package team.creative.cmdcam.fabric;

import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import team.creative.creativecore.mixin.ArgumentTypeInfosAccessor;

public interface ArgumentTypeInfosHelper {
    static <A extends ArgumentType<?>, T extends ArgumentTypeInfo.Template<A>, I extends ArgumentTypeInfo<A, T>> I registerByClass(Class<A> infoClass, I argumentTypeInfo) {
        ArgumentTypeInfosAccessor.getByClass().put(infoClass, argumentTypeInfo);

        return argumentTypeInfo;
    }
}
