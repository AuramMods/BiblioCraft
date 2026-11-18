package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.menu.DeskBookMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class DeskBookScreen extends AbstractContainerScreen<DeskBookMenu> {
    private static final ResourceLocation BOOK = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private final boolean writable;
    private final List<String> pages = new ArrayList<>();
    private final BookViewScreen.BookAccess reading;
    private int page;
    private boolean signing;
    private boolean dirty;
    private String draftTitle = "";
    private PageButton back;
    private PageButton next;
    private Button done;
    private Button sign;
    private Button finalizeBook;
    private Button cancel;
    private DeskPageEditor editor;
    private EditBox titleBox;

    DeskBookScreen(DeskBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 192;
        imageHeight = 210;
        var stack = menu.getBook();
        writable = stack.is(Items.WRITABLE_BOOK);
        reading = BookViewScreen.BookAccess.fromItem(stack);
        if (writable && stack.hasTag()) {
            var saved = stack.getTag().getList("pages", Tag.TAG_STRING);
            for (int index = 0; index < Math.min(saved.size(), DeskBookMenu.MAX_PAGES); index++) {
                pages.add(saved.getString(index));
            }
        }
        if (writable && pages.isEmpty()) {
            pages.add("");
        }
        page = Mth.clamp(menu.initialPage(), 0, pageCount() - 1);
    }

    @Override
    protected void init() {
        super.init();
        back = addRenderableWidget(new PageButton(leftPos + 43, topPos + 153, false, button -> turn(-1), true));
        next = addRenderableWidget(new PageButton(leftPos + 119, topPos + 153, true, button -> turn(1), true));
        int footer = topPos + 187;
        done = addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> save(false))
                .bounds(leftPos + (writable ? 96 : 38), footer, writable ? 66 : 116, 20).build());
        if (writable) {
            editor = new DeskPageEditor(font, leftPos + 32, topPos + 30, 122, 119);
            editor.setValue(pages.get(page));
            editor.setValueListener(text -> {
                if (!text.equals(pages.get(page))) {
                    pages.set(page, text);
                    dirty = true;
                }
            });
            addRenderableWidget(editor);
            sign = addRenderableWidget(Button.builder(Component.translatable("book.signButton"), button -> setSigning(true))
                    .bounds(leftPos + 30, footer, 66, 20).build());
            titleBox = new EditBox(font, leftPos + 34, topPos + 67, 124, 20, Component.translatable("book.editTitle"));
            titleBox.setMaxLength(DeskBookMenu.MAX_TITLE_LENGTH);
            titleBox.setValue(draftTitle);
            titleBox.setResponder(text -> {
                draftTitle = text;
                if (finalizeBook != null) {
                    finalizeBook.active = !text.isBlank();
                }
            });
            addRenderableWidget(titleBox);
            finalizeBook = addRenderableWidget(Button.builder(Component.translatable("book.finalizeButton"), button -> save(true))
                    .bounds(leftPos + 22, footer, 82, 20).build());
            cancel = addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> setSigning(false))
                    .bounds(leftPos + 106, footer, 66, 20).build());
        }
        setSigning(signing);
    }

    private int pageCount() {
        return Math.max(1, writable ? pages.size() : reading.getPageCount());
    }

    private void turn(int delta) {
        int target = page + delta;
        if (writable && target == pages.size() && pages.size() < DeskBookMenu.MAX_PAGES) {
            pages.add("");
        }
        page = Mth.clamp(target, 0, pageCount() - 1);
        if (writable) {
            editor.setValue(pages.get(page));
        } else {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.McbPagePacket(menu.containerId, page));
        }
        updateArrows();
    }

    private void setSigning(boolean value) {
        signing = value;
        done.visible = !value;
        if (writable) {
            editor.visible = !value;
            titleBox.setVisible(value);
            sign.visible = !value;
            finalizeBook.visible = value;
            finalizeBook.active = !draftTitle.isBlank();
            cancel.visible = value;
            setInitialFocus(value ? titleBox : editor);
        }
        updateArrows();
    }

    private void updateArrows() {
        back.visible = !signing && page > 0;
        next.visible = !signing && (page + 1 < pageCount() || writable && pages.size() < DeskBookMenu.MAX_PAGES);
    }

    private void save(boolean signed) {
        if (writable && (signed || dirty)) {
            while (pages.size() > 1 && pages.get(pages.size() - 1).isEmpty()) {
                pages.remove(pages.size() - 1);
            }
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.McbEditPacket(menu.containerId, page, List.copyOf(pages),
                    signed ? Optional.of(draftTitle) : Optional.empty()));
        } else if (writable) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.McbPagePacket(menu.containerId, page));
        }
        onClose();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            return super.keyPressed(key, scanCode, modifiers);
        }
        // A desk book has no inventory slots, so printable keys belong to the editor.
        var focused = getFocused();
        return focused != null && focused.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(BOOK, leftPos, topPos, 0, 0, 192, 192);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (signing) {
            graphics.drawCenteredString(font, Component.translatable("book.editTitle"), 96, 42, 0x3b2c20);
            graphics.drawWordWrap(font, Component.translatable("book.finalizeWarning"), 36, 102, 120, 0x3b2c20);
            return;
        }
        Component indicator = Component.translatable("book.pageIndicator", page + 1, pageCount());
        graphics.drawString(font, indicator, 152 - font.width(indicator), 18, 0x24201b, false);
        if (!writable) {
            var lines = font.split(reading.getPage(page), 114);
            for (int row = 0; row < Math.min(lines.size(), 14); row++) {
                graphics.drawString(font, lines.get(row), 36, 34 + row * 9, 0x24201b, false);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
