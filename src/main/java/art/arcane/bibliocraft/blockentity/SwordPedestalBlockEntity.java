package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.SwordPedestalBlock;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Locale;

public final class SwordPedestalBlockEntity extends FurnitureInventoryBlockEntity {
    private boolean powered;

    public SwordPedestalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWORD_PEDESTAL.get(), pos, state, 1);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot != 0 || stack.isEmpty() || stack.is(ModBlocks.SWORD_PEDESTAL.get().asItem())) {
            return false;
        }
        String descriptionId = stack.getDescriptionId().toLowerCase(Locale.ROOT);
        return stack.getItem() instanceof SwordItem || stack.is(ItemTags.SWORDS)
                || descriptionId.contains("sword") || descriptionId.contains("gt.metatool.01.0");
    }

    @Override
    public void setChanged() {
        super.setChanged();
        boolean hasSword = !isEmpty();
        if (level != null && !level.isClientSide && powered != hasSword) {
            powered = hasSword;
            SwordPedestalBlock.updateSignalNeighbors(level, worldPosition, getBlockState());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (level == null) {
            powered = !isEmpty();
        }
    }
}
