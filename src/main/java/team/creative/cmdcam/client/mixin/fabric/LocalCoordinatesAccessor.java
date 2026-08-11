package team.creative.cmdcam.client.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.commands.arguments.coordinates.LocalCoordinates;

@Mixin(LocalCoordinates.class)
public interface LocalCoordinatesAccessor {
    @Accessor
    double getLeft();

    @Accessor
    double getUp();

    @Accessor
    double getForwards();
}
