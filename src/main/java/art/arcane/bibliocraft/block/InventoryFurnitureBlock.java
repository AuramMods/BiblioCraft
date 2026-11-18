package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

abstract class InventoryFurnitureBlock extends HorizontalFacingEntityBlock {
    protected InventoryFurnitureBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        super(properties, shape, blockEntityPath);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof FurnitureInventoryBlockEntity furniture) {
            return furniture.getMaterial().applyTo(stack);
        }
        return stack;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof FurnitureInventoryBlockEntity furniture) {
            Containers.dropContents(level, pos, furniture);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
