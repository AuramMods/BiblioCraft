package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.book.ClipboardContent;
import art.arcane.bibliocraft.book.ClipboardContent.Page;
import art.arcane.bibliocraft.book.ClipboardContent.Task;
import art.arcane.bibliocraft.book.ClipboardContent.View;
import art.arcane.bibliocraft.menu.ClipboardMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;

public final class ClipboardScreen extends AbstractContainerScreen<ClipboardMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "textures/gui/clipboardgui.png");
    private static final int INK = 0x404040;
    private final int[] marks = new int[ClipboardContent.TASKS];
    private final TaskBox[] checks = new TaskBox[ClipboardContent.TASKS];
    private EditBox[] fields;
    private PageButton previous;
    private PageButton next;
    private View page;
    private boolean waiting;

    ClipboardScreen(ClipboardMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        page = menu.initial();
        imageWidth = imageHeight = 192;
    }

    @Override
    protected void init() {
        Page draft = fields == null ? page.page() : readFields();
        super.init();
        fields = new EditBox[ClipboardContent.TASKS + 1];
        fields[0] = field(34, 12, 125, ClipboardContent.TITLE_LENGTH, text("title"));
        for (int index = 0; index < ClipboardContent.TASKS; index++) {
            checks[index] = addRenderableWidget(new TaskBox(index));
            fields[index + 1] = field(44, 27 + index * 15, 115, ClipboardContent.TASK_LENGTH, text("task", index + 1));
        }
        previous = addRenderableWidget(new PageButton(leftPos + 38, topPos + 157, false, button -> turn(-1), true));
        next = addRenderableWidget(new PageButton(leftPos + 120, topPos + 157, true, button -> turn(1), true));
        loadFields(draft);
        setInitialFocus(fields[0]);
        updateButtons();
    }

    private EditBox field(int x, int y, int width, int limit, Component title) {
        var box = new EditBox(font, leftPos + x, topPos + y, width, 10, title);
        box.setBordered(false);
        box.setTextColor(INK);
        box.setMaxLength(limit);
        return addRenderableWidget(box);
    }

    private Page readFields() {
        var tasks = new ArrayList<Task>(ClipboardContent.TASKS);
        for (int index = 0; index < ClipboardContent.TASKS; index++) {
            tasks.add(new Task(fields[index + 1].getValue(), marks[index]));
        }
        return new Page(fields[0].getValue(), tasks);
    }

    private void loadFields(Page current) {
        fields[0].setValue(current.title());
        for (int index = 0; index < ClipboardContent.TASKS; index++) {
            Task task = current.tasks().get(index);
            marks[index] = task.state();
            fields[index + 1].setValue(task.text());
        }
    }

    private void turn(int step) {
        int nextPage = page.current() + step;
        if (waiting || nextPage < 0 || nextPage > page.count() || nextPage >= ClipboardContent.MAX_PAGES) {
            return;
        }
        savePage();
        waiting = true;
        updateButtons();
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.ClipboardVisit(menu.containerId, nextPage));
    }

    private void savePage() {
        Page edited = readFields();
        if (!edited.equals(page.page())) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.ClipboardEdit(menu.containerId, page.current(), edited));
        }
    }

    private void updateButtons() {
        previous.visible = page.current() > 0;
        next.visible = page.current() < ClipboardContent.MAX_PAGES - 1;
        previous.active = next.active = !waiting;
        for (var box : checks) {
            box.active = !waiting;
        }
        for (var field : fields) {
            field.setEditable(!waiting);
        }
    }

    public static void accept(ModNetwork.ClipboardPage reply) {
        if (Minecraft.getInstance().screen instanceof ClipboardScreen screen
                && screen.menu.containerId == reply.menuId() && screen.waiting) {
            screen.page = reply.view();
            screen.waiting = false;
            screen.loadFields(reply.view().page());
            screen.updateButtons();
        }
    }

    @Override
    public void onClose() {
        if (!waiting && fields != null && minecraft.player.containerMenu == menu) {
            savePage();
        }
        super.onClose();
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == GLFW.GLFW_KEY_PAGE_UP || key == GLFW.GLFW_KEY_PAGE_DOWN) {
            turn(key == GLFW.GLFW_KEY_PAGE_UP ? -1 : 1);
            return true;
        }
        if (getFocused() instanceof EditBox field) {
            if (minecraft.options.keyInventory.matches(key, scanCode)) {
                return true;
            }
            int direction = switch (key) {
                case GLFW.GLFW_KEY_UP -> -1;
                case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> 1;
                default -> 0;
            };
            if (direction != 0) {
                int index = Arrays.asList(fields).indexOf(field);
                setFocused(fields[Mth.clamp(index + direction, 0, fields.length - 1)]);
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        for (EditBox field : fields) {
            field.tick();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        String number = Integer.toString(page.current() + 1);
        graphics.drawString(font, number, leftPos + 93 - font.width(number) / 2, topPos + 160, INK, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.bibliocraft.clipboard." + key, args);
    }

    private final class TaskBox extends AbstractButton {
        private final int task;

        private TaskBox(int task) {
            super(leftPos + 32, topPos + 25 + 15 * task, 10, 10, text("task", task + 1));
            this.task = task;
        }

        @Override
        public void onPress() {
            marks[task] = (marks[task] + 1) % 3;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            if (marks[task] == 1) {
                graphics.blit(TEXTURE, getX() - 2, getY() - 1, 51, 195, 16, 12);
            } else if (marks[task] == 2) {
                graphics.blit(TEXTURE, getX() - 3, getY() - 2, 72, 194, 16, 15);
            }
            if (active && isHoveredOrFocused()) {
                graphics.renderOutline(getX(), getY(), width, height, 0x805B432C);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            defaultButtonNarrationText(output);
        }
    }
}
