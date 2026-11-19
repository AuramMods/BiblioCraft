package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.FancySignLayout;
import art.arcane.bibliocraft.blockentity.FancySignLayout.Icon;
import art.arcane.bibliocraft.blockentity.FancySignLayout.Line;
import art.arcane.bibliocraft.menu.FancySignMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static art.arcane.bibliocraft.client.SignDisplayRenderer.INK;
import static art.arcane.bibliocraft.client.SignDisplayRenderer.PIXEL;
import static art.arcane.bibliocraft.client.SignDisplayRenderer.TEXT_HEIGHT;
import static art.arcane.bibliocraft.client.SignDisplayRenderer.TEXT_LEFT;
import static art.arcane.bibliocraft.client.SignDisplayRenderer.TEXT_TOP;
import static art.arcane.bibliocraft.client.SignDisplayRenderer.TEXT_WIDTH;

final class FancySignScreen extends FurnitureInventoryScreen<FancySignMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/fancysigngui.png");
    private static final int CANVAS_X = 18;
    private static final int CANVAS_Y = 12;
    private static final int TEXT = 2;
    private final List<Line> lines;
    private final Icon[] icons;
    private final List<Button> iconControls = new ArrayList<>();
    private EditBox input;
    private Button format;
    private int editor = TEXT;
    private int selectedLine;

    FancySignScreen(FancySignMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE);
        imageWidth = 256;
        imageHeight = 246;
        FancySignLayout layout = menu.openedLayout();
        lines = new ArrayList<>(layout.lines());
        icons = new Icon[]{layout.left(), layout.right()};
    }

    @Override
    protected void init() {
        super.init();
        iconControls.clear();
        input = new EditBox(font, 0, 0, TEXT_WIDTH, 10, text("edit_text"));
        input.setBordered(false);
        input.setTextColor(INK);
        input.setMaxLength(FancySignLayout.MAX_TEXT_LENGTH);
        input.setFormatter(this::formatInput);
        input.setResponder(value -> lines.set(selectedLine, new Line(value, lines.get(selectedLine).size())));
        selectLine(selectedLine);
        button(193, 177, "1", "edit_left", () -> selectEditor(0));
        button(208, 177, "T", "edit_text", () -> selectEditor(TEXT));
        button(223, 177, "2", "edit_right", () -> selectEditor(1));
        button(193, 194, "-", "smaller", () -> changeSize(-1));
        button(225, 194, "+", "larger", () -> changeSize(1));
        iconControls.add(button(193, 210, "<", "rotate_left", () -> rotateIcon(-1)));
        iconControls.add(button(225, 210, ">", "rotate_right", () -> rotateIcon(1)));
        iconControls.add(button(209, 210, "↑", "move_up", () -> moveIcon(0, -1)));
        iconControls.add(button(193, 225, "←", "move_left", () -> moveIcon(-1, 0)));
        iconControls.add(button(209, 225, "↓", "move_down", () -> moveIcon(0, 1)));
        iconControls.add(button(225, 225, "→", "move_right", () -> moveIcon(1, 0)));
        format = button(209, 210, "A", "format", this::insertFormatCode);
        selectEditor(editor);
    }

    private Button button(int x, int y, String glyph, String tooltip, Runnable action) {
        return addRenderableWidget(Button.builder(Component.literal(glyph), button -> action.run())
                .bounds(leftPos + x, topPos + y, 14, 13)
                .tooltip(Tooltip.create(text(tooltip))).build());
    }

    private static Component text(String key) {
        return Component.translatable("screen.bibliocraft.fancy_sign." + key);
    }

    private void selectEditor(int editor) {
        this.editor = editor;
        boolean enteringText = editor == TEXT;
        iconControls.forEach(button -> button.visible = !enteringText);
        format.visible = enteringText;
        input.setFocused(enteringText);
    }

    private void selectLine(int line) {
        selectedLine = Mth.clamp(line, 0, visibleLines() - 1);
        Line selected = lines.get(selectedLine);
        input.setWidth((int) (TEXT_WIDTH / selected.scale()));
        input.setValue(selected.text());
    }

    private int visibleLines() {
        int height = 0;
        int count = 0;
        for (Line line : lines) {
            height += line.lineHeight();
            if (height > TEXT_HEIGHT) {
                break;
            }
            count++;
        }
        return count;
    }

    private void changeSize(int amount) {
        if (editor == TEXT) {
            Line current = lines.get(selectedLine);
            int cursor = input.getCursorPosition();
            lines.set(selectedLine, new Line(current.text(), current.size() + amount));
            selectLine(selectedLine);
            moveCursor(Math.min(cursor, input.getValue().length()));
        } else {
            editIcon(icon -> new Icon(icon.size() + amount, icon.rotation(), icon.x(), icon.y()));
        }
    }

    private void rotateIcon(int amount) {
        editIcon(icon -> new Icon(icon.size(), icon.rotation() + amount, icon.x(), icon.y()));
    }

    private void moveIcon(int x, int y) {
        editIcon(icon -> new Icon(icon.size(), icon.rotation(), icon.x() + x, icon.y() + y));
    }

    private void editIcon(UnaryOperator<Icon> operation) {
        icons[editor] = operation.apply(icons[editor]);
    }

    private void insertFormatCode() {
        String value = input.getValue();
        int cursor = input.getCursorPosition();
        if (value.length() < FancySignLayout.MAX_TEXT_LENGTH) {
            input.setValue(value.substring(0, cursor) + '§' + value.substring(cursor));
            moveCursor(cursor + 1);
            input.setFocused(true);
        }
    }

    private void moveCursor(int cursor) {
        input.setCursorPosition(cursor);
        input.setHighlightPos(cursor);
    }

    private FormattedCharSequence formatInput(String segment, int start) {
        int end = start + segment.length();
        return sink -> StringDecomposer.iterateFormatted(input.getValue(), Style.EMPTY, (index, style, codePoint) ->
                index < start || index >= end || sink.accept(index - start, style, codePoint));
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        int canvasX = leftPos + CANVAS_X;
        int canvasY = topPos + CANVAS_Y;
        graphics.enableScissor(canvasX - 5, canvasY - 3, canvasX + TEXT_WIDTH + 5, canvasY + TEXT_HEIGHT + 4);
        renderPreviewIcons(graphics, canvasX, canvasY);
        int y = canvasY;
        int visible = visibleLines();
        for (int index = 0; index < visible; index++) {
            Line line = lines.get(index);
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(canvasX, y, 5);
            pose.scale(line.scale(), line.scale(), 1);
            if (editor == TEXT && index == selectedLine && input.isFocused()) {
                input.render(graphics, 0, 0, partialTick);
            } else {
                graphics.drawString(font, SignDisplayRenderer.fitText(font, line), 0, 0, INK, false);
            }
            pose.popPose();
            y += line.lineHeight();
        }
        graphics.disableScissor();
        drawControlLabels(graphics);
    }

    private void renderPreviewIcons(GuiGraphics graphics, int canvasX, int canvasY) {
        var itemRenderer = minecraft.getItemRenderer();
        for (int slot = 0; slot < icons.length; slot++) {
            ItemStack stack = menu.getSlot(slot).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            Icon icon = icons[slot];
            float scale = SignDisplayRenderer.previewIconScale(icon);
            var pose = graphics.pose();
            pose.pushPose();
            pose.translate(canvasX + (SignDisplayRenderer.iconX(icon) - TEXT_LEFT) / PIXEL,
                    canvasY + (TEXT_TOP - SignDisplayRenderer.iconY(icon)) / PIXEL, 2 + slot);
            pose.scale(scale, -scale, 0.02F * scale);
            SignDisplayRenderer.renderIcon(itemRenderer, stack, icon, minecraft.level, slot, pose,
                    graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            graphics.flush();
            pose.popPose();
        }
    }

    private void drawControlLabels(GuiGraphics graphics) {
        int marker = switch (editor) {
            case 0 -> 200;
            case 1 -> 230;
            default -> 215;
        };
        graphics.fill(leftPos + marker - 4, topPos + 190, leftPos + marker + 4, topPos + 191, INK);
        String size = editor == TEXT ? Float.toString(lines.get(selectedLine).scale()) : Integer.toString(icons[editor].size() + 1);
        size = size.replace(".0", "") + "×";
        graphics.drawString(font, size, leftPos + 216 - font.width(size) / 2, topPos + 196, INK, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (button == 0 && x >= leftPos + CANVAS_X && x < leftPos + CANVAS_X + TEXT_WIDTH) {
            int top = topPos + CANVAS_Y;
            int visible = visibleLines();
            for (int index = 0; index < visible; index++) {
                Line line = lines.get(index);
                if (y >= top && y < top + line.lineHeight()) {
                    selectEditor(TEXT);
                    selectLine(index);
                    int offset = (int) ((x - leftPos - CANVAS_X) / line.scale());
                    moveCursor(font.getSplitter().formattedIndexByWidth(input.getValue(), offset, Style.EMPTY));
                    return true;
                }
                top += line.lineHeight();
            }
        }
        return super.mouseClicked(x, y, button);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_TAB) {
            selectEditor(editor == TEXT ? 0 : editor == 0 ? 1 : TEXT);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (editor == TEXT && input.isFocused()) {
            switch (key) {
                case GLFW.GLFW_KEY_UP -> selectLine(selectedLine - 1);
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> selectLine(selectedLine + 1);
                case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT -> insertFormatCode();
                default -> input.keyPressed(key, scanCode, modifiers);
            }
            return true;
        }
        switch (key) {
            case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> moveIcon(0, -1);
            case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> moveIcon(0, 1);
            case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> moveIcon(-1, 0);
            case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> moveIcon(1, 0);
            default -> {
                return super.keyPressed(key, scanCode, modifiers);
            }
        }
        return true;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        return editor == TEXT && input.charTyped(character, modifiers) || super.charTyped(character, modifiers);
    }

    @Override
    public void onClose() {
        FancySignLayout edited = new FancySignLayout(lines, icons[0], icons[1]);
        if (!edited.equals(menu.openedLayout())) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.SignPacket(menu.containerId, edited));
        }
        super.onClose();
    }
}
