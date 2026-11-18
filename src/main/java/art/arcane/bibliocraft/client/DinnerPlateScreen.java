package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.DinnerPlateMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class DinnerPlateScreen extends FurnitureInventoryScreen<DinnerPlateMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/woodlabelgui.png");

    DinnerPlateScreen(DinnerPlateMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
    }
}
