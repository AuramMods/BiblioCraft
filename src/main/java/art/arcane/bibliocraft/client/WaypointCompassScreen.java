package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.menu.WaypointCompassMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

final class WaypointCompassScreen extends AbstractContainerScreen<WaypointCompassMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/compasswaypointgui.png");
    private EditBox name;
    private EditBox x;
    private EditBox z;
    private Button save;
    private boolean currentDimension;

    WaypointCompassScreen(WaypointCompassMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 96;
    }

    @Override
    protected void init() {
        super.init();
        var target = menu.target();
        name = field(name, "name", 17, 27, 222, WaypointCompassItem.MAX_NAME_LENGTH, target.label().getString());
        x = field(x, "x", 57, 51, 43, 9, Integer.toString(target.x()));
        z = field(z, "z", 155, 51, 43, 9, Integer.toString(target.z()));
        x.setFilter(WaypointCompassScreen::coordinateText);
        z.setFilter(WaypointCompassScreen::coordinateText);
        addRenderableWidget(Button.builder(text("here"), button -> {
            if (minecraft.player != null) {
                currentDimension = true;
                x.setValue(Integer.toString(minecraft.player.getBlockX()));
                z.setValue(Integer.toString(minecraft.player.getBlockZ()));
            }
        }).bounds(leftPos + 46, topPos + 69, 86, 19).build());
        save = addRenderableWidget(Button.builder(text("save"), button -> save())
                .bounds(leftPos + 145, topPos + 69, 64, 19).build());
        setInitialFocus(name);
    }

    private EditBox field(EditBox old, String key, int x, int y, int width, int limit, String initial) {
        EditBox field = new EditBox(font, leftPos + x, topPos + y, width, 12, text(key));
        field.setBordered(false);
        field.setTextColor(0x404040);
        field.setMaxLength(limit);
        field.setValue(old == null ? initial : old.getValue());
        return addRenderableWidget(field);
    }

    private static boolean coordinateText(String input) {
        return input.matches("-?[0-9]*");
    }

    private static Integer coordinate(EditBox field) {
        try {
            int coordinate = Integer.parseInt(field.getValue());
            return WaypointCompassItem.validCoordinate(coordinate) ? coordinate : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void save() {
        Integer xCoordinate = coordinate(x);
        Integer zCoordinate = coordinate(z);
        if (xCoordinate != null && zCoordinate != null) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.WaypointEditPacket(menu.containerId, xCoordinate,
                    zCoordinate, name.getValue(), currentDimension));
            onClose();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawCenteredString(font, title, 128, 10, 0xffffff);
        graphics.drawString(font, text("x"), 42, 51, 0x403522, false);
        graphics.drawString(font, text("z"), 140, 51, 0x403522, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        save.active = coordinate(x) != null && coordinate(z) != null;
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (getFocused() instanceof EditBox field && minecraft.options.keyInventory.matches(key, scanCode)) {
            field.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        name.tick();
        x.tick();
        z.tick();
    }

    private static Component text(String key) {
        return Component.translatable("gui.bibliocraft.waypoint." + key);
    }
}
