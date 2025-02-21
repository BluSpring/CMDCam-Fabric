package io.github.fabricators_of_create.porting_lib.mixins.common;

import io.github.fabricators_of_create.porting_lib.features.BlockExtensions;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Block.class)
public abstract class BlockMixin implements BlockExtensions {
}
