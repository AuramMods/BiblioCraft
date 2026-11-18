package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.CookieJarBlockEntity;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public final class CookieJarMenu extends FurnitureInventoryMenu {
    public CookieJarMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(CookieJarBlockEntity.SLOT_COUNT));
    }

    public CookieJarMenu(int id, Inventory inventory, Container jar) {
        super(ModMenus.COOKIE_JAR.get(), id, jar, CookieJarBlockEntity.SLOT_COUNT);
        for (int slot = 0; slot < CookieJarBlockEntity.SLOT_COUNT; slot++) {
            addSlot(new Slot(jar, slot, 32 + (slot % 4) * 32, 22 + (slot / 4) * 29));
        }
        addPlayerSlots(inventory);
    }
}
