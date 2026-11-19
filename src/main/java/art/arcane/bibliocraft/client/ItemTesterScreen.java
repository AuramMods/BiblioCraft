package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.ItemTesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.registries.ForgeRegistries;

final class ItemTesterScreen extends AbstractContainerScreen<ItemTesterMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "textures/gui/slottedbookgui.png");
    private static final int FIRST_ROW = 52;
    private static final int ROW_HEIGHT = 16;
    private static final int VALUE_WIDTH = 115;
    private static final float MIN_SCALE = 0.8F;
    private static final int MAX_TEXT_WIDTH = Mth.floor(VALUE_WIDTH / MIN_SCALE);
    private static final String[] LABELS = {"name", "translation_key", "registry_id", "damage"};

    ItemTesterScreen(ItemTesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 174;
        imageHeight = 215;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (!menu.storedItem().isEmpty() && mouseX >= leftPos + 31 && mouseX < leftPos + 151) {
            int y = mouseY - topPos - FIRST_ROW;
            if (y >= 0 && y < ROW_HEIGHT * LABELS.length) {
                String value = values()[y / ROW_HEIGHT];
                if (font.width(value) > MAX_TEXT_WIDTH) {
                    graphics.renderTooltip(font, Component.literal(value), mouseX, mouseY);
                }
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 31, 15, 0x343029, false);
        String[] values = menu.storedItem().isEmpty() ? null : values();
        for (int row = 0; row < LABELS.length; row++) {
            int y = FIRST_ROW + row * ROW_HEIGHT;
            graphics.drawString(font, Component.translatable("gui.bibliocraft.tester." + LABELS[row]),
                    31, y, 0x64564A, false);
            if (values != null) {
                drawValue(graphics, values[row], y + 8);
            }
        }
    }

    private void drawValue(GuiGraphics graphics, String value, int y) {
        int width = font.width(value);
        float scale = Mth.clamp(VALUE_WIDTH / (float) Math.max(width, 1), MIN_SCALE, 1);
        String display = width <= MAX_TEXT_WIDTH ? value
                : font.plainSubstrByWidth(value, MAX_TEXT_WIDTH - font.width("…")) + "…";
        graphics.pose().pushPose();
        graphics.pose().translate(36, y, 0);
        graphics.pose().scale(scale, scale, 1);
        graphics.drawString(font, display, 0, 0, 0x236126, false);
        graphics.pose().popPose();
    }

    private String[] values() {
        var stack = menu.storedItem();
        return new String[]{stack.getHoverName().getString(), stack.getDescriptionId(),
                ForgeRegistries.ITEMS.getKey(stack.getItem()).toString(), Integer.toString(stack.getDamageValue())};
    }
}
