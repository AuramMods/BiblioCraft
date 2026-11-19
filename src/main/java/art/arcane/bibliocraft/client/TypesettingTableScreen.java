package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.menu.TypesettingTableMenu;
import art.arcane.bibliocraft.menu.TypesettingTableMenu.Action;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class TypesettingTableScreen extends AbstractContainerScreen<TypesettingTableMenu> {
    private static final ResourceLocation LEFT = texture("typesettinggui_l");
    private static final ResourceLocation RIGHT = texture("typesettinggui_r");
    private final Button[] select = new Button[TypesettingTableMenu.PAGE_SIZE];
    private final Button[] visibility = new Button[TypesettingTableMenu.PAGE_SIZE];
    private final Button[] delete = new Button[TypesettingTableMenu.PAGE_SIZE];
    private Button previous;
    private Button next;
    private boolean showDelete;

    TypesettingTableScreen(TypesettingTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 320;
        imageHeight = 240;
    }

    @Override
    protected void init() {
        super.init();
        for (int row = 0; row < select.length; row++) {
            int index = row;
            int y = 22 + row * 24;
            select[row] = button("select", 6, y, 44, () -> sendRow(Action.SELECT, index));
            visibility[row] = button("private", 210, y, 52, () -> sendRow(Action.SHARE, index));
            delete[row] = button("delete", 264, y, 48, () -> sendRow(Action.DELETE, index));
        }
        previous = button("previous", 7, 214, 48, () -> send(Action.PREVIOUS, null));
        next = button("next", 162, 214, 48, () -> send(Action.NEXT, null));
        button("discard", 213, 214, 51, () -> showDelete = !showDelete);
        button("done", 267, 214, 45, this::onClose);
    }

    private Button button(String label, int x, int y, int width, Runnable action) {
        return addRenderableWidget(Button.builder(text(label), button -> action.run())
                .bounds(leftPos + x, topPos + y, width, 20).build());
    }

    private void sendRow(Action action, int index) {
        var rows = menu.page().rows();
        if (index < rows.size()) {
            send(action, rows.get(index).id());
        }
    }

    private void send(Action action, @Nullable UUID id) {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.TypesettingActionPacket(menu.containerId, action, id));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        var page = menu.page();
        for (int i = 0; i < select.length; i++) {
            boolean present = i < page.rows().size();
            select[i].visible = present;
            visibility[i].visible = present;
            delete[i].visible = present && showDelete && page.rows().get(i).canManage();
            if (present) {
                var row = page.rows().get(i);
                select[i].setTooltip(Tooltip.create(Component.literal(row.title() + " · " + row.author())));
                visibility[i].setMessage(text(row.shared() ? "public" : "private"));
                visibility[i].active = row.canManage();
            }
        }
        previous.active = page.index() > 0;
        next.active = page.index() + 1 < page.total();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var page = menu.page();
        graphics.drawString(font, title, (imageWidth - font.width(title)) / 2, 8, 0x332A22, false);
        for (int i = 0; i < page.rows().size(); i++) {
            var row = page.rows().get(i);
            int y = 23 + i * 24;
            if (row.id().equals(page.selected())) {
                graphics.fill(52, y - 1, 208, y + 19, 0x38A4783B);
            }
            graphics.drawString(font, font.plainSubstrByWidth(row.title(), 150), 55, y, 0x332A22, false);
            graphics.drawString(font, font.plainSubstrByWidth(row.author(), 150), 55, y + 10, 0x5B4C3D, false);
        }
        if (page.rows().isEmpty()) {
            graphics.drawString(font, text("empty"), 13, 28, 0x332A22, false);
        }
        Component pages = Component.translatable("gui.bibliocraft.typesetting.page", page.index() + 1, page.total());
        graphics.drawString(font, pages, 108 - font.width(pages) / 2, 220, 0x332A22, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int panelHeight = imageHeight - 8;
        graphics.blit(LEFT, leftPos, topPos, 0, 0, 246, panelHeight, 256, 256);
        graphics.blit(RIGHT, leftPos + 246, topPos, 182, 0, 74, panelHeight, 256, 256);
        graphics.blit(LEFT, leftPos, topPos + panelHeight, 0, 248, 246, 8, 256, 256);
        graphics.blit(RIGHT, leftPos + 246, topPos + panelHeight, 182, 248, 74, 8, 256, 256);
    }

    private static Component text(String key) {
        return Component.translatable("gui.bibliocraft.typesetting." + key);
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/" + name + ".png");
    }

    public static void receive(ModNetwork.TypesettingPagePacket message) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof TypesettingTableMenu menu
                && menu.containerId == message.menuId()) {
            menu.receive(message.page());
        }
    }
}
