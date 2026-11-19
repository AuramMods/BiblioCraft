package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.PaintingFrameMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.painting.PaintingSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

final class PaintingFrameScreen extends FurnitureInventoryScreen<PaintingFrameMenu> {
    private final Button[] artworkControls = new Button[12];
    private Button hide;

    PaintingFrameScreen(PaintingFrameMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PaintingArt.texture("gui/paintinggui.png"));
        imageHeight = 192;
    }

    @Override
    protected void init() {
        super.init();
        for (int field = 0; field < 3; field++) {
            int row = field;
            int y = 12 + field * 26;
            artworkControls[2 * field] = button("−", 6, y, 13, 16, () -> step(row, -1));
            artworkControls[2 * field + 1] = button("+", 69, y, 13, 16, () -> step(row, 1));
        }
        artworkControls[6] = button("↶", 104, 87, 16, 17, () -> step(3, -1));
        artworkControls[7] = button("↷", 147, 87, 16, 17, () -> step(3, 1));
        for (int corner = 0; corner < 4; corner++) {
            int value = corner;
            Button anchor = button("•", corner < 2 ? 104 : 147, corner == 1 || corner == 2 ? 15 : 58,
                    17, 17, () -> setCorner(value));
            anchor.setTooltip(Tooltip.create(label("corner")));
            artworkControls[8 + corner] = anchor;
        }
        if (menu.borderless()) {
            hide = addRenderableWidget(Button.builder(label("hide"), button -> {
                var s = menu.settings();
                send(s, new PaintingSettings(s.corner(), s.scale(), s.resolution(), s.aspect(), s.rotation(),
                        s.customWidth(), s.customHeight(), !s.hidden()));
            }).bounds(leftPos + 42, topPos + 193, 92, 19).build());
        }
    }

    private Button button(String text, int x, int y, int w, int h, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(text), button -> action.run())
                .bounds(leftPos + x, topPos + y, w, h).build());
    }

    private void setCorner(int corner) {
        var s = menu.settings();
        send(s, new PaintingSettings(corner, s.scale(), s.resolution(), s.aspect(), s.rotation(),
                s.customWidth(), s.customHeight(), s.hidden()));
    }

    private void step(int index, int delta) {
        var s = menu.settings();
        PaintingArt art = art();
        boolean custom = art.source() == PaintingArt.Source.CUSTOM;
        int res = index == 1 && !custom ? s.resolution() + delta : s.resolution();
        int aspect = index == 2 && !custom ? s.aspect() + delta : s.aspect();
        send(s, new PaintingSettings(s.corner(), index == 0 ? s.scale() + delta : s.scale(), res, aspect,
                index == 3 ? s.rotation() + delta : s.rotation(), index == 1 && custom ? s.customWidth() + delta : s.customWidth(),
                index == 2 && custom ? s.customHeight() + delta : s.customHeight(), s.hidden()).forArt(art, menu.borderless()));
    }

    private void send(PaintingSettings previous, PaintingSettings next) {
        if (!next.equals(previous)) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.PaintingPacket(menu.containerId, previous, next));
        }
    }

    private PaintingArt art() {
        return PaintingArt.from(menu.getSlot(0).getItem());
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, float partialTick) {
        var art = art();
        var settings = menu.settings();
        for (Button button : artworkControls) {
            button.active = !art.isBlank();
        }
        if (art.source() != PaintingArt.Source.CUSTOM) {
            artworkControls[2].active &= art.image(settings.aspect()).textures().size() > 1;
            artworkControls[3].active &= artworkControls[2].active;
            artworkControls[4].active &= art.aspectCount() > 1;
            artworkControls[5].active &= artworkControls[4].active;
        }
        if (hide != null) {
            hide.setMessage(label(settings.hidden() ? "show" : "hide"));
        }
        super.render(graphics, x, y, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var art = art();
        var settings = menu.settings();
        boolean custom = art.source() == PaintingArt.Source.CUSTOM;
        var image = art.image(settings.aspect());
        drawField(graphics, "scale", settings.scale() + "×", 0);
        drawField(graphics, custom ? "width" : "resolution",
                custom ? Integer.toString(settings.customWidth()) : settings.resolution() + 1 + "/" + image.textures().size(), 1);
        drawField(graphics, custom ? "height" : "aspect",
                custom ? Integer.toString(settings.customHeight()) : image.width() + "×" + image.height(), 2);
        graphics.drawString(font, settings.rotation() * 90 + "°", 122, 92, 0x404040, false);
        ClientPaintingArt.preview(graphics, art, settings.aspect(), settings.resolution(), settings.rotation(),
                122, 32, 24, 25);
        int anchorX = settings.corner() < 2 ? 105 : 148;
        int anchorY = settings.corner() == 1 || settings.corner() == 2 ? 16 : 59;
        graphics.fill(anchorX + 2, anchorY + 2, anchorX + 13, anchorY + 13, 0x709E651E);
    }

    private void drawField(GuiGraphics graphics, String key, String value, int row) {
        int y = row * 26;
        graphics.drawString(font, label(key), 19, 3 + y, 0x404040, false);
        int x = 43 - font.width(value) / 2;
        graphics.drawString(font, value, x, 16 + y, 0xF1EBDC, false);
    }

    private static Component label(String key) {
        return Component.translatable("gui.bibliocraft.painting." + key);
    }
}
