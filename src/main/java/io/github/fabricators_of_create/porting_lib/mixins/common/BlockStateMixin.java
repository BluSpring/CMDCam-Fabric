package io.github.fabricators_of_create.porting_lib.mixins.common;


import io.github.fabricators_of_create.porting_lib.features.BlockStateExtensions;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateExtensions {
}
