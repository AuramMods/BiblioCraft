package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import art.arcane.bibliocraft.menu.PrintingPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class PrintingPressScreen extends AbstractContainerScreen<PrintingPressMenu> {
    PrintingPressScreen(PrintingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 176;
        inventoryLabelY = 82;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF37322B);
        graphics.fill(leftPos + 2, topPos + 2, leftPos + imageWidth - 2, topPos + imageHeight - 2, 0xFFD5CBB8);
        for (var slot : menu.slots) {
            int x = leftPos + slot.x;
            int y = topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF7E7464);
            graphics.fill(x, y, x + 17, y + 17, 0xFFEEE4D2);
            graphics.fill(x, y, x + 16, y + 16, 0xFFB2A68F);
        }
        int left = menu.ticksLeft();
        if (left > 0) {
            int fill = (PrintingPressBlockEntity.PRINT_TICKS - left) * 56 / PrintingPressBlockEntity.PRINT_TICKS;
            graphics.fill(leftPos + 60, topPos + 69, leftPos + 116, topPos + 73, 0xFF8D7E64);
            graphics.fill(leftPos + 60, topPos + 69, leftPos + 60 + fill, topPos + 73, 0xFF4F3B25);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        drawLabel(graphics, "ink", 102, 27);
        drawLabel(graphics, "plate", 102, 45);
        drawLabel(graphics, "blank", 20, 40);
        drawLabel(graphics, "result", 112, 70);
    }

    private void drawLabel(GuiGraphics graphics, String name, int x, int y) {
        graphics.drawString(font, Component.translatable("gui.bibliocraft.printing." + name), x, y, 0x40372A, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
