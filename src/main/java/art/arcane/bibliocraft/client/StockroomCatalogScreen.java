package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.menu.StockroomCatalogMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class StockroomCatalogScreen extends AbstractContainerScreen<StockroomCatalogMenu> {
    private static final ResourceLocation CATALOG =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/stockcataloggui.png");
    private static final ResourceLocation DETAILS =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/stockcatalogsubgui.png");
    private static final int ITEM_ROWS = 22;
    private static final int DETAIL_ROWS = 5;
    private static final int INK = 0x3c392e;
    private static final int SELECTED = 0x226522;
    private final Inventory inventory;
    private List<CatalogContents.Entry> items = List.of();
    private List<Integer> compassSlots = List.of();
    private CatalogContents.Entry selected;
    private EditBox name;
    private Button previous;
    private Button next;
    private Button refresh;
    private Button close;
    private int revision;
    private int mainPage;
    private int detailPage;
    private int compassIndex;
    private boolean sortByCount = true;
    private boolean reverse;

    StockroomCatalogScreen(StockroomCatalogMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.inventory = inventory;
        imageWidth = 220;
        imageHeight = 256;
    }

    public static void receive(ModNetwork.StockLogPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof StockroomCatalogMenu menu
                && menu.containerId == packet.menuId()) {
            menu.accept(packet);
        }
    }

    @Override
    protected void init() {
        super.init();
        String initial = name == null ? title.getString() : name.getValue();
        name = new EditBox(font, leftPos + 41, topPos + 13, 139, 11, text("name"));
        name.setBordered(false);
        name.setTextColor(INK);
        name.setMaxLength(CatalogContents.TITLE_LENGTH);
        name.setValue(initial);
        addRenderableWidget(name);
        previous = button("<", 55, 18, -1);
        next = button(">", 152, 18, 1);
        refresh = addRenderableWidget(Button.builder(text("refresh"),
                        button -> ModNetwork.CHANNEL.sendToServer(new ModNetwork.StockRefreshPacket(menu.containerId)))
                .bounds(leftPos + 7, topPos + 233, 46, 15).build());
        close = addRenderableWidget(Button.builder(text("close"), button -> {
            if (selected == null) {
                onClose();
            } else {
                selected = null;
            }
        }).bounds(leftPos + 172, topPos + 233, 42, 15).build());
    }

    private Button button(String label, int x, int width, int direction) {
        return addRenderableWidget(Button.builder(Component.literal(label), button -> {
            if (selected == null) {
                mainPage += direction;
            } else {
                detailPage += direction;
            }
        }).bounds(leftPos + x, topPos + 233, width, 15).build());
    }

    private void sort() {
        Collator alphabet = Collator.getInstance(Locale.forLanguageTag(minecraft.options.languageCode.replace('_', '-')));
        Comparator<CatalogContents.Entry> byName = Comparator.comparing(entry -> I18n.get(entry.item()), alphabet);
        Comparator<CatalogContents.Entry> order = sortByCount
                ? Comparator.comparingLong(CatalogContents.Entry::count).reversed() : byName;
        items = menu.entries().stream().sorted((reverse ? order.reversed() : order)
                .thenComparing(byName).thenComparing(CatalogContents.Entry::item)).toList();
    }

    private void sort(boolean count) {
        reverse = sortByCount == count && !reverse;
        sortByCount = count;
        mainPage = 0;
        sort();
    }

    private static <T> List<T> page(List<T> list, int page, int size) {
        int first = page * size;
        return list.subList(first, Math.min(first + size, list.size()));
    }

    private int pageCount() {
        int count = selected == null ? items.size() : selected.details().size();
        int rows = selected == null ? ITEM_ROWS : DETAIL_ROWS;
        return Math.max(1, (count + rows - 1) / rows);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        graphics.blit(CATALOG, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (selected != null) {
            graphics.blit(DETAILS, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (selected == null) {
            drawSmall(graphics, sortLabel(true), 18, 27, 37, sortByCount ? SELECTED : INK);
            drawSmall(graphics, sortLabel(false), 61, 27, 120, sortByCount ? INK : SELECTED);
        }
        var rows = page(items, mainPage, ITEM_ROWS);
        for (int row = 0; row < rows.size(); row++) {
            var entry = rows.get(row);
            int y = 36 + row * 9;
            if (selected == null) {
                if (isOver(mouseX - leftPos, mouseY - topPos, 15, y - 1, 191, 9)) {
                    graphics.fill(15, y - 1, 206, y + 8, 0x28266d25);
                }
                drawSmall(graphics, Component.literal(Long.toString(entry.count())), 18, y, 37, INK);
                drawSmall(graphics, Component.translatable(entry.item()), 61, y, 124, INK);
            }
            drawSmall(graphics, Component.literal(entry == selected ? "◀" : "▶"), 193, y, 10, SELECTED);
        }
        if (selected == null && items.isEmpty()) {
            drawSmall(graphics, text(revision == 0 ? "loading" : "empty"), 20, 47, 178, INK);
        }
        if (selected != null) {
            drawDetails(graphics);
        }
        int currentPage = selected == null ? mainPage : detailPage;
        String pageLabel = text("page", currentPage + 1, pageCount()).getString();
        drawSmall(graphics, Component.literal(pageLabel), 110 - (int) (0.4 * font.width(pageLabel)),
                238, 86, INK);
    }

    private Component sortLabel(boolean count) {
        String label = text(count ? "count" : "items").getString();
        return Component.literal(label + (sortByCount == count ? (reverse ? " ↑" : " ↓") : ""));
    }

    private void drawDetails(GuiGraphics graphics) {
        for (int index = 0; index < compassSlots.size(); index++) {
            int x = 22 + index * 20;
            if (index == compassIndex) {
                graphics.renderOutline(x - 1, 30, 18, 20, 0xff266d25);
            }
            graphics.renderItem(inventory.getItem(compassSlots.get(index)), x, 32);
        }
        drawSmall(graphics, Component.translatable(selected.item()), 33, 56, 136, SELECTED);
        var details = page(selected.details(), detailPage, DETAIL_ROWS);
        for (int row = 0; row < details.size(); row++) {
            var detail = details.get(row);
            int y = 71 + row * 30;
            drawSmall(graphics, Component.literal(Long.toString(detail.count())), 33, y, 29, INK);
            drawSmall(graphics, detail.name().component(), 70, y, 101, INK);
            if (!compassSlots.isEmpty()) {
                drawSmall(graphics, Component.literal("+"), 175, y + 5, 9, SELECTED);
            }
            var pos = detail.location().pos();
            drawSmall(graphics, Component.literal("X " + pos.getX()), 33, y + 13, 45, INK);
            drawSmall(graphics, Component.literal("Y " + pos.getY()), 80, y + 13, 45, INK);
            drawSmall(graphics, Component.literal("Z " + pos.getZ()), 128, y + 13, 45, INK);
        }
    }

    private void drawSmall(GuiGraphics graphics, Component value, int x, int y, int width, int color) {
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(0.8F, 0.8F, 1);
        String text = value.getString();
        int max = (int) (width / 0.8F);
        graphics.drawString(font, font.width(text) <= max ? text : font.plainSubstrByWidth(text, max - 6) + "…",
                0, 0, color, false);
        pose.popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (revision != menu.revision()) {
            revision = menu.revision();
            String detailKey = selected == null ? null : selected.item();
            sort();
            selected = items.stream().filter(entry -> entry.item().equals(detailKey)).findFirst().orElse(null);
            mainPage = Math.min(mainPage, Math.max(0, (items.size() - 1) / ITEM_ROWS));
            detailPage = selected == null ? 0 : Math.min(detailPage, pageCount() - 1);
        }
        compassSlots = StockroomCatalogMenu.compassSlots(inventory);
        compassIndex = Math.min(compassIndex, Math.max(0, compassSlots.size() - 1));
        int currentPage = selected == null ? mainPage : detailPage;
        previous.active = currentPage > 0;
        next.active = currentPage + 1 < pageCount();
        close.setMessage(text(selected == null ? "close" : "back"));
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        drawHover(graphics, mouseX, mouseY);
    }

    private void drawHover(GuiGraphics graphics, int mouseX, int mouseY) {
        if (refresh.isMouseOver(mouseX, mouseY) && revision > 0) {
            graphics.renderTooltip(font, text("source_status", menu.sourceCount(), menu.unavailable()), mouseX, mouseY);
        }
        if (selected == null) {
            return;
        }
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        for (int index = 0; index < compassSlots.size(); index++) {
            if (isOver(x, y, 22 + index * 20, 30, 18, 20)) {
                graphics.renderTooltip(font, inventory.getItem(compassSlots.get(index)), mouseX, mouseY);
                return;
            }
        }
        var rows = page(selected.details(), detailPage, DETAIL_ROWS);
        for (int row = 0; row < rows.size(); row++) {
            int top = 69 + row * 30;
            if (!compassSlots.isEmpty() && isOver(x, y, 173, top, 14, 22)) {
                graphics.renderTooltip(font, text("set_compass"), mouseX, mouseY);
            } else if (isOver(x, y, 30, top, 140, 25)) {
                graphics.renderTooltip(font, Component.literal(rows.get(row).location().dimension().location().toString()),
                        mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        double x = mouseX - leftPos;
        double y = mouseY - topPos;
        if (selected == null && isOver(x, y, 15, 25, 170, 10)) {
            setFocused(null);
            sort(x < 57);
            return true;
        }
        if (isOver(x, y, selected == null ? 15 : 187, 35, selected == null ? 191 : 19, ITEM_ROWS * 9)) {
            var rows = page(items, mainPage, ITEM_ROWS);
            int row = (int) (y - 35) / 9;
            if (row < rows.size()) {
                setFocused(null);
                var entry = rows.get(row);
                selected = selected == entry ? null : entry;
                detailPage = 0;
                return true;
            }
        }
        if (selected != null) {
            int compass = (int) (x - 22) / 20;
            if (isOver(x, y, 22, 30, compassSlots.size() * 20, 20)) {
                setFocused(null);
                compassIndex = compass;
                return true;
            }
            int row = (int) (y - 69) / 30;
            var details = page(selected.details(), detailPage, DETAIL_ROWS);
            if (!compassSlots.isEmpty() && isOver(x, y, 173, 69, 14, details.size() * 30)
                    && (y - 69) % 30 < 22) {
                setFocused(null);
                var detail = details.get(row);
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.StockCompassPacket(menu.containerId,
                        compassSlots.get(compassIndex), detail.location(), selected.item()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (name.isFocused() && minecraft.options.keyInventory.matches(key, scanCode)) {
            name.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        name.tick();
    }

    @Override
    public void onClose() {
        if (!name.getValue().equals(title.getString())) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.StockTitlePacket(menu.containerId, name.getValue()));
        }
        super.onClose();
    }

    private static boolean isOver(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.bibliocraft.catalog." + key, args);
    }
}
