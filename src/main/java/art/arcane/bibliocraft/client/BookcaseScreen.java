package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.BookcaseMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class BookcaseScreen extends FurnitureInventoryScreen<BookcaseMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/bookshelfgui.png");

    public BookcaseScreen(BookcaseMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
