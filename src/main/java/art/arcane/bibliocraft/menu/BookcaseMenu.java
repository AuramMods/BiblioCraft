package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class BookcaseMenu extends FurnitureInventoryMenu {
    public BookcaseMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(BookcaseBlockEntity.SLOT_COUNT));
    }

    public BookcaseMenu(int id, Inventory inventory, Container bookcase) {
        super(ModMenus.BOOKCASE.get(), id, bookcase, BookcaseBlockEntity.SLOT_COUNT);
        for (int row = 0; row < 2; row++) {
            for (int column = 0; column < BookcaseBlockEntity.COLUMNS; column++) {
                addSlot(new Slot(bookcase, row * BookcaseBlockEntity.COLUMNS + column,
                        17 + column * 18, 17 + row * 25) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return BookcaseBlockEntity.acceptsBook(stack);
                    }
                });
            }
        }
        addPlayerSlots(inventory);
    }

    @Override
    protected boolean canMoveIntoFurniture(ItemStack stack) {
        return BookcaseBlockEntity.acceptsBook(stack);
    }
}
