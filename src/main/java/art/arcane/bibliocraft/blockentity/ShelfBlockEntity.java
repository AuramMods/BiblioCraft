package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class ShelfBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SLOT_COUNT = 4;

    public ShelfBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SHELF.get(), pos, state, SLOT_COUNT);
    }
}
