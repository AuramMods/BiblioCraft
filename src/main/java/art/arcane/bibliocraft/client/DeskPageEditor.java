package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.DeskBookMenu;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractScrollWidget;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

final class DeskPageEditor extends AbstractScrollWidget {
    private static final int INK = 0xff282018;
    private final Font font;
    private final PageText input;

    DeskPageEditor(Font font, int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("book.editTitle"));
        this.font = font;
        input = new PageText(font, width - totalInnerPadding());
        input.setCharacterLimit(DeskBookMenu.MAX_PAGE_LENGTH);
        input.setCursorListener(this::scrollToCaret);
    }

    void setValue(String value) {
        input.setValue(value);
    }

    void setValueListener(Consumer<String> listener) {
        input.setValueListener(listener);
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("gui.narrate.editBox", getMessage(), input.value()));
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        return visible && isFocused() && input.keyPressed(key);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!visible || !isFocused() || !SharedConstants.isAllowedChatCharacter(character)) {
            return false;
        }
        input.insertText(Character.toString(character));
        return true;
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) {
            return true;
        }
        if (!visible || button != 0 || !withinContentAreaPoint(x, y)) {
            return false;
        }
        seek(x, y, Screen.hasShiftDown());
        return true;
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double deltaX, double deltaY) {
        if (super.mouseDragged(x, y, button, deltaX, deltaY)) {
            return true;
        }
        if (!isFocused() || button != 0 || !withinContentAreaPoint(x, y)) {
            return false;
        }
        seek(x, y, true);
        input.setSelecting(Screen.hasShiftDown());
        return true;
    }

    private void seek(double x, double y, boolean selecting) {
        input.setSelecting(selecting);
        input.seekCursorToPoint(x - getX() - innerPadding(), y - getY() - innerPadding() + scrollAmount());
    }

    @Override
    protected void renderBackground(GuiGraphics graphics) {
        // The page texture is drawn by the book screen.
    }

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        input.renderText(graphics);
    }

    private void scrollToCaret() {
        int top = input.getLineAtCursor() * font.lineHeight;
        int bottom = top + font.lineHeight;
        int visibleHeight = getHeight() - totalInnerPadding();
        if (top < scrollAmount()) {
            setScrollAmount(top);
        } else if (bottom > scrollAmount() + visibleHeight) {
            setScrollAmount(bottom - visibleHeight);
        }
    }

    @Override
    public int getInnerHeight() {
        return input.getLineCount() * font.lineHeight;
    }

    @Override
    protected double scrollRate() {
        return font.lineHeight / 2.0;
    }

    private final class PageText extends MultilineTextField {
        PageText(Font font, int width) {
            super(font, width);
        }

        void renderText(GuiGraphics graphics) {
            String text = value();
            int x = getX() + innerPadding();
            int y = getY() + innerPadding();
            int row = 0;
            int caretRow = getLineAtCursor();
            var selection = getSelected();
            for (var line : iterateLines()) {
                int bottom = y + font.lineHeight;
                if (withinContentAreaTopBottom(y, bottom)) {
                    if (hasSelection() && selection.endIndex() >= line.beginIndex() && selection.beginIndex() <= line.endIndex()) {
                        int start = Math.max(line.beginIndex(), selection.beginIndex());
                        int end = Math.min(line.endIndex(), selection.endIndex());
                        int from = x + font.width(text.substring(line.beginIndex(), start));
                        int to = x + font.width(text.substring(line.beginIndex(), end));
                        graphics.fill(from, y, to, bottom, 0xffc5d5db);
                    }
                    graphics.drawString(font, text.substring(line.beginIndex(), line.endIndex()), x, y, INK, false);
                    if (row == caretRow && isFocused() && Util.getMillis() / 500 % 2 == 0) {
                        int offset = Math.max(line.beginIndex(), Math.min(cursor(), line.endIndex()));
                        int caret = x + font.width(text.substring(line.beginIndex(), offset));
                        graphics.fill(caret, y - 1, caret + 1, bottom, INK);
                    }
                }
                y = bottom;
                row++;
            }
        }
    }
}
