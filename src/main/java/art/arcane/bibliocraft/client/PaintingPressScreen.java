package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.PaintingPressMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.painting.PaintingArt.Source;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

final class PaintingPressScreen extends FurnitureInventoryScreen<PaintingPressMenu> {
    private static final int COLUMNS = 8;
    private static final int PAGE_SIZE = 32;
    private static final int CELL_W = 29;
    private static final int CELL_H = 24;
    private static final int GALLERY_LEFT = 12;
    private static final int GALLERY_TOP = 32;
    private Source tab;
    private List<PaintingArt> artworks = List.of();
    private PaintingArt selected;
    private PaintingArt seen;
    private int page;
    private Button previous;
    private Button next;
    private Button transfer;

    PaintingPressScreen(PaintingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, PaintingArt.texture("gui/paintpressgui.png"));
        imageWidth = 256;
        imageHeight = 241;
        selected = seen = menu.selection();
        changeTab(selected.source());
        page = Math.max(0, artworks.indexOf(selected)) / PAGE_SIZE;
    }

    @Override
    protected void init() {
        super.init();
        for (Source source : Source.values()) {
            addRenderableWidget(Button.builder(label(source.name().toLowerCase(java.util.Locale.ROOT)),
                            button -> changeTab(source))
                    .bounds(leftPos + 14 + source.ordinal() * 76, topPos, 76, 17).build());
        }
        previous = addRenderableWidget(Button.builder(Component.literal("‹"), button -> page--)
                .bounds(leftPos + 8, topPos + 136, 18, 18).build());
        next = addRenderableWidget(Button.builder(Component.literal("›"), button -> page++)
                .bounds(leftPos + 230, topPos + 136, 18, 18).build());
        transfer = addRenderableWidget(Button.builder(label("transfer"), button -> send(selected, true))
                .bounds(leftPos + 36, topPos + 136, 69, 18).build());
    }

    private void changeTab(Source source) {
        tab = source;
        var gallery = ClientPaintingArt.gallery(tab);
        if (tab == Source.BIBLIOCRAFT) {
            var includesBlank = new ArrayList<PaintingArt>(gallery.size() + 1);
            includesBlank.add(PaintingArt.BLANK);
            includesBlank.addAll(gallery);
            artworks = List.copyOf(includesBlank);
        } else {
            artworks = gallery;
        }
        page = 0;
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        var current = menu.selection();
        if (!seen.equals(current)) {
            selected = seen = current;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        previous.active = page > 0;
        next.active = (page + 1) * PAGE_SIZE < artworks.size();
        transfer.active = menu.getSlot(0).hasItem();
        super.render(graphics, mouseX, mouseY, partialTick);
        int hovered = artworkIndex(mouseX, mouseY);
        if (hovered >= 0) {
            graphics.renderTooltip(font, artworks.get(hovered).name(), mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, title, 11, 21, 0x404040, false);
        var title = selected.name().getString();
        graphics.drawString(font, font.plainSubstrByWidth(title, 87), 140, 142, 0x404040, false);
        if (artworks.isEmpty()) {
            graphics.drawString(font, label("empty_gallery"), 13, 48, 0x404040, false);
        }
        int start = page * PAGE_SIZE;
        for (int index = start; index < Math.min(artworks.size(), start + PAGE_SIZE); index++) {
            int slot = index - start;
            int cellX = GALLERY_LEFT + slot % COLUMNS * CELL_W;
            int cellY = GALLERY_TOP + slot / COLUMNS * CELL_H;
            var art = artworks.get(index);
            graphics.fill(cellX, cellY, cellX + CELL_W - 2, cellY + CELL_H - 2,
                    selected.equals(art) ? 0xFFCFA54B : 0xFF7D7466);
            ClientPaintingArt.preview(graphics, art, 0, 0, 0, cellX + 2, cellY + 2, CELL_W - 6, CELL_H - 6);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int index = artworkIndex(mouseX, mouseY);
        if (button == 0 && index >= 0) {
            selected = artworks.get(index);
            send(selected, false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int artworkIndex(double mouseX, double mouseY) {
        int x = (int) Math.floor(mouseX - leftPos - GALLERY_LEFT);
        int y = (int) Math.floor(mouseY - topPos - GALLERY_TOP);
        if (x < 0 || x >= COLUMNS * CELL_W || y < 0 || y >= 4 * CELL_H) {
            return -1;
        }
        int index = page * PAGE_SIZE + y / CELL_H * COLUMNS + x / CELL_W;
        return index < artworks.size() ? index : -1;
    }

    private void send(PaintingArt art, boolean start) {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.PaintPressPacket(menu.containerId, art, start));
    }

    private static Component label(String key) {
        return Component.translatable("gui.bibliocraft.painting." + key);
    }
}
