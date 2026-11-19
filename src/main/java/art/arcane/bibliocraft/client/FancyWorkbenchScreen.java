package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.FancyWorkbenchBlockEntity;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.menu.FancyWorkbenchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class FancyWorkbenchScreen extends AbstractContainerScreen<FancyWorkbenchMenu> {
    private static final ResourceLocation WORKBENCH =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/fancyworkbenchgui.png");
    private static final ResourceLocation SIDE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/fancyworkbenchbookcasegui.png");
    private static final ItemStack BOOK_ICON = new ItemStack(Items.BOOK);
    private ImageButton save;
    private ImageButton load;
    private ItemStack shownBook = ItemStack.EMPTY;
    private RecordedRecipe saved = RecordedRecipe.read(ItemStack.EMPTY);

    FancyWorkbenchScreen(FancyWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 192;
        inventoryLabelY = 99;
    }

    @Override
    protected void init() {
        super.init();
        save = arrow(53, 17, FancyWorkbenchMenu.SAVE_RECIPE, "save");
        load = arrow(22, 0, FancyWorkbenchMenu.LOAD_RECIPE, "load");
    }

    private ImageButton arrow(int y, int u, int action, String label) {
        Component text = Component.translatable("gui.bibliocraft.fancy_workbench." + label);
        ImageButton button = new ImageButton(leftPos + 10, topPos + y, 15, 11, u, 200, 11, WORKBENCH,
                256, 256, clicked -> minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action), text);
        button.setTooltip(Tooltip.create(text));
        return addRenderableWidget(button);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float tick, int mouseX, int mouseY) {
        graphics.blit(WORKBENCH, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.hasLeftBookcase()) {
            graphics.blit(SIDE, leftPos - 57, topPos, 0, 0, 55, 180);
        }
        if (menu.hasRightBookcase()) {
            graphics.blit(SIDE, leftPos + 178, topPos, 0, 0, 55, 180);
        }
        if (menu.getSlot(0).getItem().isEmpty()) {
            ghost(graphics, BOOK_ICON, 8, 35);
        }
        for (int index = 0; index < RecordedRecipe.GRID_SIZE; index++) {
            var slot = menu.getSlot(FancyWorkbenchMenu.GRID_START + index);
            ItemStack ingredient = saved.grid().get(index);
            if (!slot.hasItem() && !ingredient.isEmpty()) {
                ghost(graphics, ingredient, slot.x, slot.y);
            }
        }
    }

    private void ghost(GuiGraphics graphics, ItemStack stack, int x, int y) {
        int px = leftPos + x;
        int py = topPos + y;
        graphics.renderFakeItem(stack, px, py);
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 180);
        graphics.fill(px, py, px + 16, py + 16, 0x70645f5b);
        graphics.pose().popPose();
    }

    @Override
    protected boolean hasClickedOutside(double x, double y, int left, int top, int button) {
        if (y >= top && y < top + 180 && ((menu.hasLeftBookcase() && x >= left - 57 && x < left)
                || (menu.hasRightBookcase() && x >= left + imageWidth && x < left + 233))) {
            return false;
        }
        return super.hasClickedOutside(x, y, left, top, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (menu.hasLeftBookcase()) {
            sideLabel(graphics, -30, "left");
        }
        if (menu.hasRightBookcase()) {
            sideLabel(graphics, 205, "right");
        }
    }

    private void sideLabel(GuiGraphics graphics, int x, String side) {
        Component title = Component.translatable("gui.bibliocraft.fancy_workbench." + side);
        graphics.drawString(font, title, x - font.width(title) / 2, 6, 0x404040, false);
        Component bookcase = Component.translatable("block.bibliocraft.bookcase");
        graphics.drawString(font, bookcase, x - font.width(bookcase) / 2, 15, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ItemStack book = menu.getSlot(0).getItem();
        if (!ItemStack.matches(shownBook, book)) {
            shownBook = book.copy();
            saved = RecordedRecipe.read(book);
        }
        save.visible = FancyWorkbenchBlockEntity.acceptsBook(book) && !RecordedRecipe.isSigned(book)
                && menu.getSlot(FancyWorkbenchMenu.RESULT).hasItem();
        save.active = save.visible;
        load.visible = saved.isRecorded();
        load.active = load.visible;
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
