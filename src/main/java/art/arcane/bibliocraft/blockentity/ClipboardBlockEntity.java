package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.book.ClipboardContent;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class ClipboardBlockEntity extends FurnitureBlockEntity {
    public static final ModelProperty<Integer> VISIBLE_PARTS = new ModelProperty<>();
    public static final String STACK_TAG = "Clipboard";
    private ItemStack stack = ItemStack.EMPTY;
    private ClipboardContent.View view = ClipboardContent.View.of(ItemStack.EMPTY);

    public ClipboardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLIPBOARD.get(), pos, state);
    }

    public ItemStack clipboard() {
        return stack;
    }

    public void setClipboard(ItemStack clipboard) {
        stack = clipboard.copyWithCount(1);
        setChanged();
    }

    public ClipboardContent.View display() {
        return view;
    }

    public void toggleTask(int index) {
        if (stack.isEmpty()) {
            stack = new ItemStack(ModItems.BIBLIO_CLIPBOARD.get());
        }
        ClipboardContent.save(stack, view.current(), view.page().cycle(index));
        setChanged();
    }

    public void turnPage(int increment) {
        int index = view.current() + increment;
        if (index >= 0 && index < view.count()) {
            ClipboardContent.select(stack, index);
            setChanged();
        }
    }

    @Override
    public void setChanged() {
        updateDisplay();
        super.setChanged();
        if (level != null && !level.isClientSide) {
            var state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void updateDisplay() {
        var previous = view;
        view = ClipboardContent.View.of(stack);
        if (previous.page().visibleParts() != view.page().visibleParts()) {
            refreshModel();
        }
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(VISIBLE_PARTS, view.page().visibleParts()).build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ItemStack saved = ItemStack.of(tag.getCompound(STACK_TAG));
        stack = saved.is(ModItems.BIBLIO_CLIPBOARD.get()) ? saved.copyWithCount(1) : ItemStack.EMPTY;
        updateDisplay();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!stack.isEmpty()) {
            tag.put(STACK_TAG, stack.save(new CompoundTag()));
        }
    }
}
