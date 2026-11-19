package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.BookcaseBlock;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;
import java.util.Locale;

public class BookcaseBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int COLUMNS = 8;
    public static final int SLOT_COUNT = 2 * COLUMNS;
    public static final ModelProperty<Integer> OCCUPIED_SLOTS = new ModelProperty<>();
    private static final List<String> COMPATIBLE_BOOK_NAMES = List.of(
            "ub@", "vv@", "vw@", "manual", "catalogue", "railcraft.routing.table", "compendium", "guide",
            "atlas", "item.itemeldritchobject.1", "diary", "rites", "stockroomcatalog", "stockroom_catalog",
            "herbarium", "portfolio", "folder", "on_the_dynamics_of_integration");
    private int lastSignal;

    public BookcaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BOOKCASE.get(), pos, state, SLOT_COUNT);
    }

    public static boolean acceptsBook(ItemStack stack) {
        if (stack.is(ModItems.BIBLIO_CLIPBOARD.get())) {
            return true;
        }
        if (stack.isEmpty() || stack.getItem() instanceof BlockItem) {
            return false;
        }
        String id = stack.getDescriptionId().toLowerCase(Locale.ROOT);
        if (COMPATIBLE_BOOK_NAMES.stream().anyMatch(id::contains) || id.contains("clipboard")) {
            return true;
        }
        if (id.contains("shelf") || id.contains("case") || id.contains("stand") || id.contains("planks")) {
            return false;
        }
        String name = stack.getItem().getName(stack).getString().toLowerCase(Locale.ROOT);
        for (String allowed : BiblioConfig.COMMON.allowedBooks.get().split(",")) {
            String keyword = allowed.strip().toLowerCase(Locale.ROOT);
            if (!keyword.isEmpty() && (id.contains(keyword) || name.contains(keyword))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return acceptsBook(stack);
    }

    public int getOccupiedSlots() {
        int slots = 0;
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (!getItem(slot).isEmpty()) {
                slots |= 1 << slot;
            }
        }
        return slots;
    }

    public int getRedstoneSignal() {
        return BiblioConfig.COMMON.enableBookcase.get() && BiblioConfig.COMMON.enableRedstonebook.get()
                ? storedRedstoneSignal() : 0;
    }

    private int storedRedstoneSignal() {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            if (getItem(slot).is(ModItems.BIBLIO_RED_BOOK.get())) {
                return slot;
            }
        }
        return 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && storedRedstoneSignal() > 0) {
            BookcaseBlock.updateSignalNeighbors(level, worldPosition, getBlockState());
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            int signal = getRedstoneSignal();
            if (signal != lastSignal) {
                lastSignal = signal;
                BookcaseBlock.updateSignalNeighbors(level, worldPosition, getBlockState());
            }
        }
    }

    @Override
    public void load(CompoundTag tag) {
        int priorSlots = getOccupiedSlots();
        super.load(tag);
        if (level == null) {
            lastSignal = getRedstoneSignal();
        }
        if (priorSlots != getOccupiedSlots()) {
            refreshModel();
        }
    }

    @Override
    public ModelData getModelData() {
        return super.getModelData().derive().with(OCCUPIED_SLOTS, getOccupiedSlots()).build();
    }
}
