package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.DisplayCaseBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class DisplayCaseMenu extends FurnitureInventoryMenu {
    public DisplayCaseMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1));
    }

    public DisplayCaseMenu(int id, Inventory inventory, Container displayCase) {
        super(ModMenus.CASE.get(), id, displayCase, 1);
        addSlot(new Slot(displayCase, DisplayCaseBlockEntity.DISPLAY_SLOT, 80, 36));
        addPlayerSlots(inventory);
    }
}
