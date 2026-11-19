package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.RedstoneBookMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class RedstoneBookScreen extends AbstractContainerScreen<RedstoneBookMenu> {
    private static final ResourceLocation BOOK = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int INK = 0x35312C;
    private EditBox titleField;

    RedstoneBookScreen(RedstoneBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = imageHeight = 192;
    }

    @Override
    protected void init() {
        String draft = titleField == null ? menu.initialTitle() : titleField.getValue();
        super.init();
        titleField = addRenderableWidget(new EditBox(font, leftPos + 32, topPos + 32, 120, 15,
                Component.translatable("gui.bibliocraft.utility_book.title")));
        titleField.setMaxLength(RedstoneBookMenu.TITLE_LENGTH);
        titleField.setHint(Component.translatable("item.bibliocraft.biblio_red_book"));
        titleField.setValue(draft);
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(leftPos + 36, topPos + 150, 42, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.bibliocraft.utility_book.save"), button -> {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.RedstoneBookRename(menu.containerId, titleField.getValue()));
            onClose();
        }).bounds(leftPos + 106, topPos + 150, 42, 20).build());
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (titleField.isFocused() && minecraft.options.keyInventory.matches(key, scanCode)) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        titleField.tick();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BOOK, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        graphics.drawString(font, Component.translatable("gui.bibliocraft.utility_book.title"),
                leftPos + 35, topPos + 20, INK, false);
        paragraph(graphics, "direction", 54);
        paragraph(graphics, "strength", 91);
    }

    private void paragraph(GuiGraphics graphics, String key, int top) {
        graphics.pose().pushPose();
        graphics.pose().translate(leftPos + 35, topPos + top, 0);
        graphics.pose().scale(0.65F, 0.65F, 1);
        graphics.drawWordWrap(font, Component.translatable("gui.bibliocraft.redstone_book." + key), 0, 0, 176, INK);
        graphics.pose().popPose();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}
}
