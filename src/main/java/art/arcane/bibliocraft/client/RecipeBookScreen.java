package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.crafting.RecordedRecipe;
import art.arcane.bibliocraft.menu.RecipeBookMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RecipeBookScreen extends AbstractContainerScreen<RecipeBookMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/recipebookgui.png");
    private static final int INK = 0x402c1e;
    private final EditBox[] lines = new EditBox[RecordedRecipe.TEXT_LINES];
    private RecordedRecipe recipe = RecordedRecipe.read(ItemStack.EMPTY);
    private Map<Component, Integer> ingredients = Map.of();
    private Button save;
    private Button sign;
    private Button craft;
    private Button confirm;
    private Button cancel;
    private boolean loaded;
    private boolean signed;
    private boolean signing;

    RecipeBookScreen(RecipeBookMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 158;
    }

    public static void receive(ModNetwork.RecipeTextPacket packet) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof RecipeBookMenu menu
                && menu.containerId == packet.menuId()) {
            menu.receiveFeedback(packet.message());
        }
    }

    @Override
    protected void init() {
        super.init();
        for (int index = 0; index < lines.length; index++) {
            EditBox previous = lines[index];
            EditBox line = new EditBox(font, leftPos + 12, topPos + 22 + index * 10, 104, 10,
                    text("note", index + 1));
            line.setMaxLength(RecordedRecipe.LINE_LENGTH);
            line.setBordered(false);
            line.setTextColor(INK);
            line.setTextColorUneditable(INK);
            if (previous != null) {
                line.setValue(previous.getValue());
            }
            lines[index] = addRenderableWidget(line);
        }
        if (!signed) {
            lines[0].setHint(text("description"));
        }
        save = button("save", 66, 130, 47, pressed -> annotate(false));
        sign = button("sign", 13, 130, 47, pressed -> signing = true);
        confirm = button("confirm", 13, 130, 47, pressed -> annotate(true));
        cancel = button("cancel", 66, 130, 47, pressed -> signing = false);
        craft = button("craft", 210, 59, 38, pressed -> minecraft.gameMode.handleInventoryButtonClick(
                menu.containerId, RecipeBookMenu.CRAFT));
        updateButtons();
    }

    private Button button(String key, int x, int y, int width, Button.OnPress press) {
        return addRenderableWidget(Button.builder(text(key), press)
                .bounds(leftPos + x, topPos + y, width, 18).build());
    }

    private void readBook() {
        ItemStack book = menu.book();
        if (loaded || book.isEmpty()) {
            return;
        }
        loaded = true;
        recipe = RecordedRecipe.read(book);
        signed = RecordedRecipe.isSigned(book);
        if (signed) {
            lines[0].setHint(null);
        }
        List<String> notes = RecordedRecipe.description(book);
        for (int index = 0; index < lines.length; index++) {
            lines[index].setValue(notes.get(index));
            lines[index].setEditable(!signed);
        }
        var counts = new LinkedHashMap<Component, Integer>();
        for (ItemStack stack : recipe.grid()) {
            if (!stack.isEmpty()) {
                counts.merge(stack.getHoverName(), 1, Integer::sum);
            }
        }
        ingredients = counts;
    }

    private void updateButtons() {
        save.visible = sign.visible = loaded && !signed && !signing;
        confirm.visible = cancel.visible = loaded && !signed && signing;
        craft.visible = loaded && !signing && menu.portableCrafting() && recipe.isRecorded();
        for (EditBox line : lines) {
            line.setVisible(loaded && !signing);
        }
    }

    private void annotate(boolean sign) {
        ModNetwork.CHANNEL.sendToServer(new ModNetwork.RecipeBookEditPacket(menu.containerId,
                Arrays.stream(lines).map(EditBox::getValue).toList(), sign));
        onClose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (int slot = 0; slot < recipe.grid().size(); slot++) {
            ItemStack ingredient = recipe.grid().get(slot);
            if (!ingredient.isEmpty()) {
                graphics.renderItem(ingredient, leftPos + 142 + slot % 3 * 18, topPos + 20 + slot / 3 * 18);
            }
        }
        if (!recipe.output().isEmpty()) {
            int x = leftPos + 224;
            int y = topPos + 37;
            graphics.renderItem(recipe.output(), x, y);
            graphics.renderItemDecorations(font, recipe.output(), x, y);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        Component name = recipe.output().isEmpty() ? title : recipe.output().getHoverName();
        line(graphics, name, 12, 10, 105);
        line(graphics, name, 142, 10, 105);
        int y = 80;
        for (var ingredient : ingredients.entrySet()) {
            line(graphics, text("ingredient", ingredient.getValue(), ingredient.getKey()), 142, y, 106);
            y += 7;
        }
        if (signing) {
            graphics.drawWordWrap(font, text("sign_warning"), 16, 39, 95, INK);
        } else if (!menu.feedback().getString().isEmpty()) {
            line(graphics, menu.feedback(), 142, 140, 105);
        }
    }

    private void line(GuiGraphics graphics, Component text, int x, int y, int width) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(0.8F, 0.8F, 1);
        graphics.drawString(font, font.plainSubstrByWidth(text.getString(), (int) (width / 0.8F)), 0, 0, INK, false);
        graphics.pose().popPose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        readBook();
        updateButtons();
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        ItemStack hovered = ItemStack.EMPTY;
        if (isHovering(224, 37, 16, 16, mouseX, mouseY)) {
            hovered = recipe.output();
        } else {
            for (int slot = 0; slot < recipe.grid().size(); slot++) {
                if (isHovering(142 + slot % 3 * 18, 20 + slot / 3 * 18, 16, 16, mouseX, mouseY)) {
                    hovered = recipe.grid().get(slot);
                    break;
                }
            }
        }
        if (!hovered.isEmpty()) {
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (!signed && !signing && getFocused() instanceof EditBox line
                && minecraft.options.keyInventory.matches(key, scanCode)) {
            line.keyPressed(key, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void containerTick() {
        for (EditBox line : lines) {
            line.tick();
        }
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.bibliocraft.recipe_book." + key, args);
    }
}
