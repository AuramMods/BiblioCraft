package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public final class FancySignBlockEntity extends FurnitureInventoryBlockEntity {
    private FancySignLayout layout = FancySignLayout.DEFAULT;

    public FancySignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FANCY_SIGN.get(), pos, state, 2);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    public FancySignLayout getLayout() {
        return layout;
    }

    public void setLayout(FancySignLayout layout) {
        if (!this.layout.equals(layout)) {
            this.layout = layout;
            setChanged();
        }
    }

    public boolean hasContents() {
        return !isEmpty() || !layout.equals(FancySignLayout.DEFAULT);
    }

    public ItemStack packInto(ItemStack stack) {
        if (hasContents()) {
            CompoundTag data = stack.getOrCreateTagElement("BlockEntityTag");
            if (!isEmpty()) {
                data.put("Items", saveWithoutMetadata().getList("Items", Tag.TAG_COMPOUND));
            }
            if (!layout.equals(FancySignLayout.DEFAULT)) {
                data.put(FancySignLayout.TAG, layout.save());
            }
        }
        return stack;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        layout = tag.contains(FancySignLayout.TAG, Tag.TAG_COMPOUND)
                ? FancySignLayout.load(tag.getCompound(FancySignLayout.TAG)) : FancySignLayout.DEFAULT;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!layout.equals(FancySignLayout.DEFAULT)) {
            tag.put(FancySignLayout.TAG, layout.save());
        }
    }
}
