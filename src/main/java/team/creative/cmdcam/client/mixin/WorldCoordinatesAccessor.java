package team.creative.cmdcam.client.mixin;

import net.minecraft.commands.arguments.coordinates.WorldCoordinate;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(WorldCoordinates.class)
public interface WorldCoordinatesAccessor {
    @Accessor("x")
    WorldCoordinate getX();

    @Accessor("y")
    WorldCoordinate getY();

    @Accessor("z")
    WorldCoordinate getZ();
}