package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;

public final class TableMenu extends FurnitureInventoryMenu {
    public TableMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(1));
    }

    public TableMenu(int id, Inventory inventory, Container table) {
        super(ModMenus.TABLE.get(), id, table, 1);
        addSlot(new Slot(table, TableBlockEntity.DISPLAY, 80, 36));
        addPlayerSlots(inventory);
    }

    @Override
    public boolean stillValid(Player player) {
        return BiblioConfig.COMMON.enableTable.get() && super.stillValid(player);
    }
}
