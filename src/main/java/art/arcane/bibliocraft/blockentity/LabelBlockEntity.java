package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class LabelBlockEntity extends FurnitureInventoryBlockEntity {
    public LabelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LABEL.get(), pos, state, 3);
    }
}
