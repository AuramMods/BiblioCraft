package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public final class LabelMenu extends FurnitureInventoryMenu {
    public LabelMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(3));
    }

    public LabelMenu(int id, Inventory inventory, Container label) {
        super(ModMenus.LABEL.get(), id, label, 3);
        addSlot(new Slot(label, 0, 80, 45));
        addSlot(new Slot(label, 1, 35, 26));
        addSlot(new Slot(label, 2, 125, 26));
        addPlayerSlots(inventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enableWoodLabel.get() && super.stillValid(player);
    }
}
