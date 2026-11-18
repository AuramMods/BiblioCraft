package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ShelfMenu extends AbstractContainerMenu {
    private static final int SHELF_END = ShelfBlockEntity.SLOT_COUNT;
    private final Container shelf;

    public ShelfMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(SHELF_END));
    }

    public ShelfMenu(int id, Inventory playerInventory, Container shelf) {
        super(ModMenus.SHELF.get(), id);
        checkContainerSize(shelf, SHELF_END);
        this.shelf = shelf;

        addSlot(new Slot(shelf, 0, 64, 22));
        addSlot(new Slot(shelf, 1, 96, 22));
        addSlot(new Slot(shelf, 2, 64, 51));
        addSlot(new Slot(shelf, 3, 96, 51));
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < SHELF_END
                ? moveItemStackTo(stack, SHELF_END, slots.size(), true)
                : moveItemStackTo(stack, 0, SHELF_END, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return shelf.stillValid(player);
    }
}
