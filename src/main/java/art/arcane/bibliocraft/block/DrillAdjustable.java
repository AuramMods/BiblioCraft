package art.arcane.bibliocraft.block;

import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

public interface DrillAdjustable {
    default BlockState adjustWithDrill(BlockState state, boolean sneaking) {
        return sneaking ? state.rotate(Rotation.CLOCKWISE_90) : state;
    }
}
