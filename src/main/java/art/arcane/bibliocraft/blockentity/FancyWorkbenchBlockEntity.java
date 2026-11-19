package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class FancyWorkbenchBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int ACTIVE_BOOK = 0;
    public static final int STORAGE_SLOTS = 8;
    public static final int SLOT_COUNT = STORAGE_SLOTS + 1;
    public static final ModelProperty<Integer> OCCUPIED_BOOKS = new ModelProperty<>();
    private int clientBooks;

    public FancyWorkbenchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FANCY_WORKBENCH.get(), pos, state, SLOT_COUNT);
    }

    public static boolean acceptsBook(ItemStack stack) {
        return stack.is(Items.BOOK) || stack.is(ModItems.RECIPE_BOOK.get());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return acceptsBook(stack);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == ACTIVE_BOOK ? 1 : super.getSlotLimit(slot);
    }

    public int occupiedBooks() {
        int books = 0;
        for (int slot = 1; slot < SLOT_COUNT; slot++) {
            if (!getItem(slot).isEmpty()) {
                books |= 1 << (slot - 1);
            }
        }
        return books;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(MATERIAL, getMaterial()).with(OCCUPIED_BOOKS, clientBooks).build();
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveFurnitureData(tag);
        tag.putInt("BooksVisible", occupiedBooks());
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        clientBooks = tag.getInt("BooksVisible");
        if (level != null && level.isClientSide) {
            refreshModel();
        }
    }
}
