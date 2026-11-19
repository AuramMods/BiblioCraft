package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.book.BigBookContent;
import art.arcane.bibliocraft.book.BigBookContent.Chapter;
import art.arcane.bibliocraft.book.BigBookContent.Header;
import art.arcane.bibliocraft.book.BigBookContent.Line;
import art.arcane.bibliocraft.book.BigBookContent.Page;
import art.arcane.bibliocraft.menu.BigBookMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BigBookScreen extends AbstractContainerScreen<BigBookMenu> {
    private static final ResourceLocation BOOK =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/bigbookgui.png");
    private static final ResourceLocation BUTTONS =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/bigbookbuttons.png");
    private static final int BOOK_WIDTH = 220;
    private static final int BOOK_HEIGHT = 256;
    private static final int TEXT_X = 15;
    private static final int TEXT_TOP = 15;
    private static final int TEXT_END = 233;
    private static final int TEXT_WIDTH = 193;
    private static final int INK = 0x21170c;
    private static final int[] ARROW_X = {15, 45, 150, 179};
    private static final String[] ARROW_NAMES = {"first", "previous", "next", "last"};
    private static final ChatFormatting[] FORMATS = {ChatFormatting.BOLD, ChatFormatting.ITALIC,
            ChatFormatting.UNDERLINE, ChatFormatting.STRIKETHROUGH, ChatFormatting.OBFUSCATED};

    private final Header header;
    private final Map<Integer, List<Line>> drafts = new HashMap<>();
    private final List<Chapter> chapters;
    private final List<Button> editingControls = new ArrayList<>();
    private final EditBox[][] labels = new EditBox[BigBookContent.CHAPTERS][2];
    private List<Line> lines;
    private EditBox input;
    private EditBox titleInput;
    private Button signConfirm;
    private Button signCancel;
    private float zoom;
    private int bookX;
    private int bookY;
    private int page;
    private int pages;
    private int firstLine;
    private int selectedLine;
    private boolean settingInput;
    private boolean signing;
    private boolean submitted;

    BigBookScreen(BigBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        header = menu.initialHeader();
        chapters = new ArrayList<>(header.chapters());
        pages = header.pages();
        page = header.bookmark();
        lines = draft(page);
    }

    public static void receive(ModNetwork.BigBookPagePacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof BigBookMenu menu && menu.containerId == packet.menuId()) {
            menu.acceptPage(packet.index(), packet.page());
        }
    }

    @Override
    protected void init() {
        int cursor = input == null ? 0 : input.getCursorPosition();
        boolean editingLine = input == null || input.isFocused();
        String pendingTitle = titleInput == null ? "" : titleInput.getValue();
        super.init();
        zoom = Math.min(1F, Math.min((width - 6) / 374F, (height - 6) / (float) BOOK_HEIGHT));
        bookX = (width - Math.round(374 * zoom)) / 2 + Math.round(61 * zoom);
        bookY = (height - Math.round(BOOK_HEIGHT * zoom)) / 2;
        editingControls.clear();
        input = new EditBox(font, 0, 0, TEXT_WIDTH, 10, text("line"));
        input.setBordered(false);
        input.setTextColor(INK);
        input.setMaxLength(BigBookContent.LINE_LENGTH);
        input.setFormatter((segment, start) -> sink -> StringDecomposer.iterateFormatted(input.getValue(), Style.EMPTY,
                (index, style, codePoint) -> index < start || index >= start + segment.length()
                        || sink.accept(index - start, style, codePoint)));
        input.setResponder(value -> {
            if (!settingInput && lines != null) {
                lines.set(selectedLine, new Line(value, lines.get(selectedLine).size()));
            }
        });
        titleInput = addRenderableWidget(new EditBox(font, tx(22), ty(80), size(175), size(17), text("title")));
        titleInput.setMaxLength(BigBookContent.TITLE_LENGTH);
        titleInput.setValue(pendingTitle);
        signCancel = button(20, 204, 80, 20, text("cancel"), "cancel", () -> {
            signing = false;
            activateLine(selectedLine, false);
        });
        signConfirm = button(120, 204, 80, 20, text("sign_confirm"), "sign", () ->
                finish(Optional.of(BigBookContent.validTitle(titleInput.getValue()))));
        if (!header.signed()) {
            String[] names = {"bold", "italic", "underline", "strikethrough", "obfuscated"};
            String[] glyphs = {"B", "I", "U", "S", "K"};
            for (int index = 0; index < FORMATS.length; index++) {
                ChatFormatting format = FORMATS[index];
                int x = -60 + (index < 3 ? index : index - 3) * 20;
                int y = index < 3 ? 25 : 43;
                editingControls.add(button(x, y, 20, 17, Component.literal(glyphs[index]).withStyle(format),
                        names[index], () -> applyFormat(format)));
            }
            editingControls.add(button(-60, 118, 60, 18, text("reset"), "reset", () -> applyFormat(ChatFormatting.RESET)));
            editingControls.add(button(-60, 154, 18, 17, Component.literal("−"), "smaller", () -> changeSize(-1)));
            editingControls.add(button(-18, 154, 18, 17, Component.literal("+"), "larger", () -> changeSize(1)));
            editingControls.add(button(-60, 204, 59, 19, text("sign"), "sign", () -> {
                signing = true;
                refreshControls();
                input.setFocused(false);
                setFocused(titleInput);
            }));
            editingControls.add(button(-60, 227, 59, 19, text("save"), "save", this::onClose));
            for (int index = 0; index < chapters.size(); index++) {
                final int tab = index;
                for (int row = 0; row < 2; row++) {
                    final int labelRow = row;
                    EditBox label = new EditBox(font, tx(225), ty(10 + 30 * index + 10 * row),
                            size(73), size(10), text("chapter_label", index + 1));
                    label.setBordered(false);
                    label.setTextColor(INK);
                    label.setMaxLength(BigBookContent.LABEL_LENGTH);
                    label.setValue(row == 0 ? chapters.get(index).first() : chapters.get(index).second());
                    label.setResponder(value -> {
                        Chapter current = chapters.get(tab);
                        chapters.set(tab, new Chapter(current.enabled(), current.page(),
                                labelRow == 0 ? value : current.first(), labelRow == 1 ? value : current.second()));
                    });
                    labels[index][row] = addRenderableWidget(label);
                }
            }
            if (!signing && lines != null) {
                activateLine(selectedLine, false);
                moveCursor(cursor);
                input.setFocused(editingLine);
            }
        }
        refreshControls();
        if (signing) {
            setFocused(titleInput);
        }
    }

    private Button button(int x, int y, int w, int h, Component glyph, String tooltipKey, Runnable action) {
        return addRenderableWidget(Button.builder(glyph, button -> action.run()).bounds(tx(x), ty(y), size(w), size(h))
                .tooltip(Tooltip.create(text(tooltipKey))).build());
    }

    private void refreshControls() {
        boolean editing = !header.signed() && !signing && lines != null;
        editingControls.forEach(button -> {
            button.visible = !signing;
            button.active = editing;
        });
        for (int index = 0; index < chapters.size(); index++) {
            for (EditBox label : labels[index]) {
                if (label != null) {
                    label.setVisible(editing && chapters.get(index).enabled());
                }
            }
        }
        signCancel.visible = signing;
        signConfirm.visible = signing;
        signConfirm.active = signing && !BigBookContent.validTitle(titleInput.getValue()).isEmpty();
        titleInput.setVisible(signing);
    }

    private List<Line> draft(int index) {
        List<Line> saved = drafts.get(index);
        if (saved != null) {
            return saved;
        }
        Page data = menu.pageForScreen(index);
        if (data == null) {
            return null;
        }
        List<Line> copy = new ArrayList<>(data.lines());
        drafts.put(index, copy);
        return copy;
    }

    private void moveTo(int target) {
        if (lines == null || target == page || target < 0 || target >= BigBookContent.MAX_PAGES
                || target > pages || (target == pages && header.signed())) {
            return;
        }
        pages = Math.max(pages, target + 1);
        page = target;
        firstLine = 0;
        selectedLine = 0;
        lines = draft(target);
        input.setFocused(false);
        setFocused(null);
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.BigBookVisitPacket(menu.containerId, target));
        if (lines != null && !header.signed()) {
            activateLine(0, false);
        }
    }

    private void activateLine(int index, boolean end) {
        if (lines == null || header.signed() || signing) {
            return;
        }
        setFocused(null);
        selectedLine = Mth.clamp(index, 0, BigBookContent.LINES - 1);
        firstLine = Math.min(firstLine, selectedLine);
        while (selectedLine >= lastVisible()) {
            firstLine++;
        }
        Line line = lines.get(selectedLine);
        settingInput = true;
        input.setWidth((int) (TEXT_WIDTH / line.scale()));
        input.setValue(line.text());
        settingInput = false;
        moveCursor(end ? line.text().length() : 0);
        input.setFocused(true);
    }

    private void moveCursor(int index) {
        input.setCursorPosition(index);
        input.setHighlightPos(index);
    }

    private int lastVisible() {
        int y = TEXT_TOP;
        int row = firstLine;
        while (row < BigBookContent.LINES) {
            y += lines.get(row).height();
            if (y > TEXT_END) {
                break;
            }
            row++;
        }
        return row;
    }

    private void applyFormat(ChatFormatting format) {
        if (lines == null) {
            return;
        }
        int cursor = input.getCursorPosition();
        String value = input.getValue();
        if (value.length() <= BigBookContent.LINE_LENGTH - 2) {
            input.setValue(value.substring(0, cursor) + format + value.substring(cursor));
            moveCursor(cursor + 2);
        }
        setFocused(null);
        input.setFocused(true);
    }

    private void changeSize(int movement) {
        if (lines == null) {
            return;
        }
        int cursor = input.getCursorPosition();
        Line line = lines.get(selectedLine);
        lines.set(selectedLine, new Line(line.text(), Math.floorMod(line.size() + movement, 8)));
        activateLine(selectedLine, false);
        moveCursor(cursor);
    }

    private boolean arrowEnabled(int id) {
        return lines != null && !signing && switch (id) {
            case 0, 1 -> page > 0;
            case 2 -> page + 1 < pages || (!header.signed() && page + 1 < BigBookContent.MAX_PAGES);
            default -> page + 1 < pages;
        };
    }

    private void useArrow(int id) {
        if (arrowEnabled(id)) {
            moveTo(switch (id) {
                case 0 -> 0;
                case 1 -> page - 1;
                case 2 -> page + 1;
                default -> pages - 1;
            });
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        double x = (mouseX - bookX) / zoom;
        double y = (mouseY - bookY) / zoom;
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(bookX, bookY, 0);
        pose.scale(zoom, zoom, 1);
        graphics.blit(BOOK, 0, 0, 0, 0, BOOK_WIDTH, BOOK_HEIGHT);
        if (signing) {
            graphics.drawString(font, text("title"), 22, 65, INK, false);
            graphics.drawCenteredString(font, text("author", minecraft.player.getGameProfile().getName()), 110, 106, INK);
            graphics.drawWordWrap(font, text("sign_warning"), 22, 149, 176, INK);
        } else {
            if (!header.signed()) {
                drawEditor(graphics, x, y);
            } else if (!header.title().isEmpty()) {
                graphics.drawString(font, font.plainSubstrByWidth(header.title(), TEXT_WIDTH), TEXT_X, 5, INK, false);
            }
            drawChapters(graphics, x, y);
            drawArrows(graphics, x, y);
            if (lines == null) {
                graphics.drawString(font, text("loading"), TEXT_X, TEXT_TOP, INK, false);
            } else {
                drawLines(graphics, partialTick);
            }
        }
        pose.popPose();
    }

    private void drawEditor(GuiGraphics graphics, double x, double y) {
        graphics.blit(BUTTONS, -60, 65, 0, 0, 60, 53);
        graphics.blit(BUTTONS, -60, 137, 0, 200, 60, 54);
        if (over(x, y, -60, 66, 60, 52)) {
            int column = (int) ((x + 60) / 15);
            int row = (int) ((y - 66) / 13);
            graphics.blit(BUTTONS, -59 + column * 15, 66 + row * 13, 62, 0, 13, 12);
        }
        if (lines != null) {
            graphics.drawCenteredString(font, lines.get(selectedLine).scale() + "×", -30, 158, 0xffffff);
            graphics.drawCenteredString(font, text("line_number", selectedLine + 1), -30, 177, 0xffffff);
        }
    }

    private void drawChapters(GuiGraphics graphics, double x, double y) {
        for (int index = 0; index < chapters.size(); index++) {
            Chapter chapter = chapters.get(index);
            int top = 6 + index * 30;
            if (chapter.enabled()) {
                int textureY = header.signed() ? 171 : over(x, y, 298, top, 15, 28) ? 84 : 55;
                graphics.blit(BUTTONS, 212, top, 0, textureY, 100, 28);
                int arrowY = over(x, y, 205, top + 5, 18, 18) ? 24 : chapter.page() == page ? 35 : 13;
                graphics.blit(BUTTONS, 205, top + 9, 62, arrowY, 17, 10);
                if (header.signed()) {
                    graphics.drawString(font, font.plainSubstrByWidth(chapter.first(), 76), 225, top + 4, INK, false);
                    graphics.drawString(font, font.plainSubstrByWidth(chapter.second(), 76), 225, top + 14, INK, false);
                }
            } else if (!header.signed()) {
                boolean hovered = over(x, y, 212, top + 5, 16, 18);
                graphics.blit(BUTTONS, 212, top, 0, hovered ? 142 : 113, hovered ? 95 : 16, 28);
            }
        }
    }

    private void drawArrows(GuiGraphics graphics, double x, double y) {
        for (int arrow = 0; arrow < ARROW_X.length; arrow++) {
            if (!arrowEnabled(arrow)) {
                continue;
            }
            boolean hovered = over(x, y, ARROW_X[arrow], 233, 19, 13);
            int u = (hovered ? 103 : 80) + (arrow == 3 ? 23 : 0);
            int v = arrow == 0 || arrow == 3 ? 26 : arrow == 1 ? 13 : 0;
            if (arrow == 0 || arrow == 3) {
                u = arrow == 0 ? 80 : 103;
                v = hovered ? 37 : 26;
            }
            graphics.blit(BUTTONS, ARROW_X[arrow], 235, u, v, 18, 10);
        }
        Component indicator = text("page", page + 1, pages);
        graphics.drawString(font, indicator, 110 - font.width(indicator) / 2, 237, INK, false);
    }

    private void drawLines(GuiGraphics graphics, float tick) {
        int y = TEXT_TOP;
        int last = lastVisible();
        graphics.enableScissor(tx(TEXT_X - 1), ty(TEXT_TOP), tx(TEXT_X + TEXT_WIDTH), ty(TEXT_END));
        for (int row = firstLine; row < last; row++) {
            Line line = lines.get(row);
            boolean editing = !header.signed() && row == selectedLine && input.isFocused();
            float scale = line.scale();
            if (header.signed()) {
                scale = Math.min(scale, TEXT_WIDTH / (float) Math.max(1, font.width(line.text())));
            }
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(TEXT_X, y, 1);
            pose.scale(scale, scale, 1);
            if (editing) {
                input.render(graphics, -1, -1, tick);
            } else {
                graphics.drawString(font, line.text(), 0, 0, INK, false);
            }
            pose.popPose();
            y += line.height();
        }
        graphics.disableScissor();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (lines == null) {
            lines = draft(page);
            if (lines != null && !header.signed()) {
                activateLine(0, false);
            }
        }
        refreshControls();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        double x = (mouseX - bookX) / zoom;
        double y = (mouseY - bookY) / zoom;
        for (int arrow = 0; arrow < ARROW_X.length; arrow++) {
            if (arrowEnabled(arrow) && over(x, y, ARROW_X[arrow], 233, 19, 13)) {
                graphics.renderTooltip(font, text(ARROW_NAMES[arrow]), mouseX, mouseY);
                return;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || signing || lines == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        double x = (mouseX - bookX) / zoom;
        double y = (mouseY - bookY) / zoom;
        for (int arrow = 0; arrow < ARROW_X.length; arrow++) {
            if (over(x, y, ARROW_X[arrow], 233, 19, 13) && arrowEnabled(arrow)) {
                useArrow(arrow);
                return true;
            }
        }
        if (clickChapter(x, y)) {
            return true;
        }
        if (!header.signed()) {
            if (over(x, y, -60, 66, 60, 52)) {
                applyFormat(ChatFormatting.getById((int) ((y - 66) / 13) * 4 + (int) ((x + 60) / 15)));
                return true;
            }
            if (over(x, y, TEXT_X, TEXT_TOP, TEXT_WIDTH - 1, TEXT_END - TEXT_TOP)) {
                int top = TEXT_TOP;
                for (int row = firstLine; row < lastVisible(); row++) {
                    Line line = lines.get(row);
                    if (y < top + line.height()) {
                        activateLine(row, false);
                        int offset = (int) ((x - TEXT_X) / line.scale());
                        moveCursor(font.getSplitter().formattedIndexByWidth(input.getValue(), offset, Style.EMPTY));
                        return true;
                    }
                    top += line.height();
                }
            }
        }
        if (getChildAt(mouseX, mouseY).orElse(null) instanceof EditBox) {
            input.setFocused(false);
        }
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (signing) {
            setFocused(titleInput);
        }
        return handled;
    }

    private boolean clickChapter(double x, double y) {
        for (int index = 0; index < chapters.size(); index++) {
            Chapter chapter = chapters.get(index);
            int top = 6 + index * 30;
            if (chapter.enabled() && over(x, y, 205, top + 4, header.signed() ? 107 : 18, 20)) {
                moveTo(chapter.page());
                return true;
            }
            boolean clickedToggle = chapter.enabled() ? over(x, y, 298, top, 15, 28)
                    : over(x, y, 212, top + 4, 16, 20);
            if (!header.signed() && clickedToggle) {
                chapters.set(index, new Chapter(!chapter.enabled(), chapter.enabled() ? chapter.page() : page,
                        chapter.first(), chapter.second()));
                setFocused(null);
                refreshControls();
                if (!chapter.enabled()) {
                    input.setFocused(false);
                    setFocused(labels[index][0]);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (lines != null && !signing && over((mouseX - bookX) / zoom, (mouseY - bookY) / zoom,
                TEXT_X, TEXT_TOP, TEXT_WIDTH, TEXT_END - TEXT_TOP)) {
            int direction = delta > 0 ? -1 : 1;
            if (direction < 0 || lastVisible() < BigBookContent.LINES) {
                firstLine = Mth.clamp(firstLine + direction, 0, BigBookContent.LINES - 1);
                input.setFocused(false);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (getFocused() instanceof EditBox field && minecraft.options.keyInventory.matches(key, scanCode)) {
            field.keyPressed(key, scanCode, modifiers);
            return true;
        }
        if (!signing && (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN)) {
            useArrow(key == GLFW.GLFW_KEY_PAGE_UP ? 1 : 2);
            return true;
        }
        if (!signing && !header.signed() && lines != null && input.isFocused()) {
            switch (key) {
                case GLFW.GLFW_KEY_UP -> activateLine(selectedLine - 1, true);
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> activateLine(selectedLine + 1, false);
                case GLFW.GLFW_KEY_TAB -> {
                    input.setFocused(false);
                    return super.keyPressed(key, scanCode, modifiers);
                }
                default -> input.keyPressed(key, scanCode, modifiers);
            }
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return (!header.signed() && !signing && input.charTyped(character, modifiers))
                || super.charTyped(character, modifiers);
    }

    @Override
    protected void containerTick() {
        input.tick();
        titleInput.tick();
        for (EditBox[] tab : labels) {
            for (EditBox label : tab) {
                if (label != null) {
                    label.tick();
                }
            }
        }
    }

    @Override
    public void onClose() {
        finish(Optional.empty());
    }

    private void finish(Optional<String> title) {
        if (!submitted) {
            submitted = true;
            if (!header.signed()) {
                drafts.forEach((index, draft) -> {
                    Page edited = new Page(draft);
                    if (!edited.equals(menu.pageForScreen(index))) {
                        ModNetwork.CHANNEL.sendToServer(new ModNetwork.BigBookEditPacket(menu.containerId, index, edited));
                    }
                });
            }
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.BigBookFinishPacket(menu.containerId, page, List.copyOf(chapters), title));
        }
        super.onClose();
    }

    private int tx(int x) { return bookX + Math.round(x * zoom); }
    private int ty(int y) { return bookY + Math.round(y * zoom); }
    private int size(int length) { return Math.max(1, Math.round(length * zoom)); }

    private static boolean over(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    private static Component text(String key, Object... arguments) {
        return Component.translatable("gui.bibliocraft.big_book." + key, arguments);
    }
}
