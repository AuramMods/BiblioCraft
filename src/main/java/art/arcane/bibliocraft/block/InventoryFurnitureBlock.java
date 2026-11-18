package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

abstract class InventoryFurnitureBlock extends HorizontalFacingEntityBlock {
    protected InventoryFurnitureBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        super(properties, shape, blockEntityPath);
    }

    protected static boolean insertStack(FurnitureInventoryBlockEntity furniture, int slot, ItemStack held) {
        ItemStack stored = furniture.getItem(slot);
        if (held.isEmpty() || (!stored.isEmpty() && !ItemStack.isSameItemSameTags(stored, held))) {
            return false;
        }
        int limit = Math.min(held.getMaxStackSize(), furniture.getMaxStackSize());
        int amount = Math.min(held.getCount(), limit - stored.getCount());
        if (amount <= 0) {
            return false;
        }
        furniture.setItem(slot, held.copyWithCount(stored.getCount() + amount));
        held.shrink(amount);
        return true;
    }

    protected static boolean ejectStack(Level level, BlockPos pos, Direction opening,
                                        FurnitureInventoryBlockEntity furniture, int slot) {
        ItemStack stored = furniture.getItem(slot);
        if (stored.isEmpty()) {
            return false;
        }
        Vec3 drop = Vec3.atCenterOf(pos).relative(opening, 0.25);
        ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z, stored.copy());
        entity.setDefaultPickUpDelay();
        if (!level.addFreshEntity(entity)) {
            return false;
        }
        furniture.setItem(slot, ItemStack.EMPTY);
        return true;
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
