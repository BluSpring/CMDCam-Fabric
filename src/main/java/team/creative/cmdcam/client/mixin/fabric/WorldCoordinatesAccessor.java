package team.creative.cmdcam.client.mixin.fabric;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;

@Mixin(WorldCoordinates.class)
public interface WorldCoordinatesAccessor {
    @Accessor
    WorldCoordinate getX();

    @Accessor
    WorldCoordinate getY();

    @Accessor
    WorldCoordinate getZ();
}
