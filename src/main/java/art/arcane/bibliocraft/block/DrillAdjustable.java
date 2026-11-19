package art.arcane.bibliocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public interface DrillAdjustable {
    default BlockState adjustWithDrill(BlockState state, boolean sneaking) {
        return sneaking ? state.rotate(Rotation.CLOCKWISE_90) : state;
    }

    default void afterDrill(Level level, BlockPos pos, BlockState previous, BlockState adjusted) {}
}
