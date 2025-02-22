package io.github.fabricators_of_create.porting_lib.features;

import net.minecraft.client.Camera;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public interface BlockStateExtensions {
    /**
     * Used to determine the state 'viewed' by an entity (see
     * {@link Camera#getBlockAtCamera()}).
     * Can be used by fluid blocks to determine if the viewpoint is within the fluid or not.
     *
     * @param level     the level
     * @param pos       the position
     * @param viewpoint the viewpoint
     * @return the block state that should be 'seen'
     */
    default BlockState getStateAtViewpoint(BlockGetter level, BlockPos pos, Vec3 viewpoint) {
        return ((BlockExtensions) ((BlockState) this).getBlock()).getStateAtViewpoint(((BlockState) this), level, pos, viewpoint);
    }

}
