package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.book.SlottedBookContents;
import art.arcane.bibliocraft.book.SlottedBookContents.Label;
import art.arcane.bibliocraft.menu.SlottedBookMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;

final class SlottedBookScreen extends AbstractContainerScreen<SlottedBookMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            BiblioCraft.MODID, "textures/gui/slottedbookgui.png");
    private EditBox[] fields;

    SlottedBookScreen(SlottedBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 174;
        imageHeight = 215;
    }

    @Override
    protected void init() {
        Label draft = fields == null ? menu.label() : readFields();
        super.init();
        fields = new EditBox[1 + SlottedBookContents.LINES];
        fields[0] = field(15, draft.title(), text("title"));
        fields[0].setHint(text("title"));
        for (int index = 0; index < SlottedBookContents.LINES; index++) {
            fields[index + 1] = field(54 + 10 * index, draft.lines().get(index), text("line", index + 1));
        }
        fields[1].setHint(text("description"));
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(leftPos + 30, topPos + 104, 42, 20).build());
        addRenderableWidget(Button.builder(text("save"), button -> {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.SlottedBookEdit(menu.containerId, readFields()));
            onClose();
        }).bounds(leftPos + 100, topPos + 104, 42, 20).build());
    }

    private EditBox field(int y, String value, Component title) {
        var box = new EditBox(font, leftPos + 31, topPos + y, 110, 10, title);
        box.setBordered(false);
        box.setTextColor(0x343029);
        box.setMaxLength(SlottedBookContents.TEXT_LENGTH);
        box.setValue(value);
        return addRenderableWidget(box);
    }

    private Label readFields() {
        return new Label(fields[0].getValue(), Arrays.stream(fields).skip(1).map(EditBox::getValue).toList());
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
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
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.bibliocraft.utility_book." + key, args);
    }
}
