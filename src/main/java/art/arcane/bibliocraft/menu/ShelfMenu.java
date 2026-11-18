package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class ShelfMenu extends FurnitureInventoryMenu {
    public ShelfMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, new SimpleContainer(ShelfBlockEntity.SLOT_COUNT));
    }

    public ShelfMenu(int id, Inventory playerInventory, Container shelf) {
        super(ModMenus.SHELF.get(), id, shelf, ShelfBlockEntity.SLOT_COUNT);

        addSlot(new Slot(shelf, 0, 64, 22));
        addSlot(new Slot(shelf, 1, 96, 22));
        addSlot(new Slot(shelf, 2, 64, 51));
        addSlot(new Slot(shelf, 3, 96, 51));
        addPlayerSlots(playerInventory);
    }
}
