package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public final class DeskMenu extends FurnitureInventoryMenu {
    private static final int VISIBLE_SLOTS = 9;

    public DeskMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(10));
    }

    public DeskMenu(int id, Inventory inventory, Container desk) {
        super(ModMenus.DESK.get(), id, desk, VISIBLE_SLOTS);
        addBookSlot(desk, DeskBlockEntity.OPEN_BOOK, 80, 36);
        for (int row = 0; row < DeskBlockEntity.BOOKS_PER_STACK; row++) {
            addBookSlot(desk, DeskBlockEntity.LEFT_BOOKS + row, 36, 62 - row * 18);
        }
        for (int row = 0; row < DeskBlockEntity.BOOKS_PER_STACK; row++) {
            addBookSlot(desk, DeskBlockEntity.RIGHT_BOOKS + row, 126, 62 - row * 18);
        }
        addPlayerSlots(inventory);
    }

    private void addBookSlot(Container desk, int index, int x, int y) {
        addSlot(new Slot(desk, index, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return DeskBlockEntity.acceptsBook(stack);
            }
        });
    }

    @Override
    protected boolean canMoveIntoFurniture(ItemStack stack) {
        return DeskBlockEntity.acceptsBook(stack);
    }
}
