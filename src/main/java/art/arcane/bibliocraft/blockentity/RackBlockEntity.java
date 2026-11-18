package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.RackType;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RackBlockEntity extends FurnitureInventoryBlockEntity {
    private final RackType type;

    private RackBlockEntity(BlockEntityType<RackBlockEntity> entityType, BlockPos pos, BlockState state, RackType type) {
        super(entityType, pos, state, type.size());
        this.type = type;
    }

    public static RackBlockEntity potionShelf(BlockPos pos, BlockState state) {
        return new RackBlockEntity(ModBlockEntities.POTION_SHELF.get(), pos, state, RackType.POTIONS);
    }

    public static RackBlockEntity toolRack(BlockPos pos, BlockState state) {
        return new RackBlockEntity(ModBlockEntities.TOOL_RACK.get(), pos, state, RackType.TOOLS);
    }

    public RackType type() {
        return type;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return type.accepts(stack);
    }
}
