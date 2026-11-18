package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.RackMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class RackScreen extends FurnitureInventoryScreen<RackMenu> {
    RackScreen(RackMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, menu.type().screenTexture());
    }
}
