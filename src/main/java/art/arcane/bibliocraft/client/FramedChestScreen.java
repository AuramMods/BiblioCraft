package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.FramedChestMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class FramedChestScreen extends AbstractContainerScreen<FramedChestMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    FramedChestScreen(FramedChestMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 114 + menu.rows() * 18;
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int storageHeight = menu.rows() * 18 + 17;
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, storageHeight);
        graphics.blit(TEXTURE, leftPos, topPos + storageHeight, 0, 126, imageWidth, 96);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
