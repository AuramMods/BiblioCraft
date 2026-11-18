package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.ShelfMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ShelfScreen extends FurnitureInventoryScreen<ShelfMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/genericshelfgui.png");

    public ShelfScreen(ShelfMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
