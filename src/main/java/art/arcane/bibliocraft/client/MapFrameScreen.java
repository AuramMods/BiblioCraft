package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.menu.MapFrameMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class MapFrameScreen extends AbstractContainerScreen<MapFrameMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "textures/gui/mapwaypointgui.png");
    private EditBox name;
    private Button colorButton;
    private int colorIndex;

    MapFrameScreen(MapFrameMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 96;
        colorIndex = MapPin.WOOL_COLORS.indexOf(menu.view().pin().color());
    }

    @Override
    protected void init() {
        super.init();
        var view = menu.view();
        String initialText = view.editing() ? view.pin().name() : text("number", view.number()).getString();
        String value = name == null ? initialText : name.getValue();
        name = new EditBox(font, leftPos + 17, topPos + 33, 221, 12, text("name"));
        name.setMaxLength(MapPin.NAME_LENGTH);
        name.setBordered(false);
        name.setTextColor(0x403522);
        name.setValue(value);
        addRenderableWidget(name);
        colorButton = addRenderableWidget(Button.builder(Component.empty(), button -> {
            colorIndex = (colorIndex + 1) % MapPin.WOOL_COLORS.size();
            updateColor();
        }).bounds(leftPos + 118, topPos + 57, 20, 20).build());
        updateColor();
        addRenderableWidget(Button.builder(text("save"), button -> save(false))
                .bounds(leftPos + 161, topPos + 56, 60, 20).build());
        addRenderableWidget(Button.builder(text(view.editing() ? "remove" : "cancel"), button -> {
            if (view.editing()) {
                save(true);
            } else {
                onClose();
            }
        }).bounds(leftPos + 35, topPos + 56, 60, 20).build());
        setInitialFocus(name);
    }

    private void updateColor() {
        var color = MapPin.WOOL_COLORS.get(colorIndex);
        colorButton.setTooltip(Tooltip.create(text("color", Component.translatable("color.minecraft." + color.getName()))));
    }

    private void save(boolean remove) {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.MapPinPacket(menu.containerId, name.getValue(),
                MapPin.WOOL_COLORS.get(colorIndex), remove));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BACKGROUND, leftPos, topPos, imageWidth, imageHeight, 0, 0, 128, 48, 128, 128);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, 128, 10, 0xffffff);
        Component coords = text("coordinates", menu.view().x(), menu.view().z());
        graphics.drawString(font, coords, (imageWidth - font.width(coords)) / 2, 81, 0x403522, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        var texture = ResourceLocation.withDefaultNamespace("textures/block/" + MapPin.WOOL_COLORS.get(colorIndex).getName() + "_wool.png");
        graphics.blit(texture, leftPos + 120, topPos + 59, 0, 0, 16, 16, 16, 16);
    }

    @Override
    protected void containerTick() {
        name.tick();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (getFocused() == name && minecraft.options.keyInventory.matches(key, scanCode)) {
            name.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private static Component text(String name, Object... args) {
        return Component.translatable("gui.bibliocraft.map_pin." + name, args);
    }
}
