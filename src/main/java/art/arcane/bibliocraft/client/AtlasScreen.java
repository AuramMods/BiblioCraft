package art.arcane.bibliocraft.client;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.menu.AtlasMenu;
import art.arcane.bibliocraft.network.ModNetwork;
import art.arcane.bibliocraft.registry.ModItems;
import com.mojang.math.Axis;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

final class AtlasScreen extends AbstractContainerScreen<AtlasMenu> {
    private static final ResourceLocation INVENTORY = texture("atlasgui");
    private static final ResourceLocation BUTTONS = texture("atlasguibuttons");
    private static final ResourceLocation TRANSFER = texture("atlastransfergui");
    private static final int MAP_SIZE = 172;
    private static final int MAP_X = 42;
    private static final int MAP_Y = 28;
    private final Button[] arrows = new Button[4];
    private final Button[] compasses = new Button[AtlasContents.SUPPLIES];
    private AtlasMenu.Mode displayed;
    private Tool tool = Tool.SELECT;
    private int targeting = -1;
    private Button autoCenter;
    private Button autoCreate;
    private EditBox name;
    private Button color;
    private Button save;
    private Button cancel;
    private Button remove;
    private Button needle;
    private int colorIndex;
    private @Nullable MapPin.Point editingPoint;
    private @Nullable MapPin editingPin;
    private @Nullable Integer editingMapId;
    private int editingSlot;

    AtlasScreen(AtlasMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 256;
        imageHeight = 241;
    }

    @Override
    protected void init() {
        super.init();
        clearWidgets();
        displayed = menu.mode();
        switch (displayed) {
            case INVENTORY -> initInventory();
            case MAP -> initMap();
            case TRANSFER -> initTransfer();
        }
        updateButtons();
    }

    private void initInventory() {
        button("map", 217, 160, 34, () -> click(AtlasMenu.SHOW_MAP));
        button("minus", 65, 23, 17, () -> click(AtlasMenu.ZOOM_OUT));
        button("plus", 100, 23, 17, () -> click(AtlasMenu.ZOOM_IN));
        autoCenter = button("off", 85, 45, 32, () -> click(AtlasMenu.AUTO_CENTER));
        autoCreate = button("off", 85, 70, 32, () -> click(AtlasMenu.AUTO_CREATE));
    }

    private void initMap() {
        String previousName = name == null ? "" : name.getValue();
        button("inventory", 190, 4, 64, () -> click(AtlasMenu.SHOW_INVENTORY));
        button("select", 2, 53, 37, () -> choose(Tool.SELECT));
        button("add", 2, 77, 37, () -> choose(Tool.ADD));
        button("edit", 2, 101, 37, () -> choose(Tool.EDIT));
        arrows[0] = button("north", 120, 8, 16, () -> click(AtlasMenu.NEIGHBOR));
        arrows[1] = button("east", 239, 28, 16, () -> click(AtlasMenu.NEIGHBOR + 1));
        arrows[2] = button("south", 120, 215, 16, () -> click(AtlasMenu.NEIGHBOR + 2));
        arrows[3] = button("west", 21, 28, 16, () -> click(AtlasMenu.NEIGHBOR + 3));
        for (int i = 0; i < compasses.length; i++) {
            int slot = i;
            compasses[i] = addRenderableWidget(Button.builder(Component.literal(Integer.toString(i + 1)),
                    widget -> target(slot)).bounds(leftPos + 220, topPos + 57 + i * 19, 34, 18).build());
        }
        needle = button("needle", 216, 174, 40, () -> click(AtlasMenu.COMPASS_SLOT + targeting));
        name = addRenderableWidget(new EditBox(font, leftPos + 4, topPos + 215, 110, 19, text("name")));
        name.setMaxLength(MapPin.NAME_LENGTH);
        color = addRenderableWidget(Button.builder(Component.empty(), button -> {
            colorIndex = (colorIndex + 1) % MapPin.WOOL_COLORS.size();
            updateColor();
        }).bounds(leftPos + 117, topPos + 215, 20, 19).build());
        save = button("save", 139, 215, 35, () -> savePin(editingPin == null ? AtlasMenu.Edit.ADD : AtlasMenu.Edit.CHANGE));
        remove = button("remove", 176, 215, 39, () -> savePin(AtlasMenu.Edit.REMOVE));
        cancel = button("cancel", 217, 215, 37, this::clearEditor);
        if (editingPoint != null) {
            name.setValue(previousName);
            updateColor();
            setInitialFocus(name);
        }
    }

    private void initTransfer() {
        button("export", 6, 106, 110, () -> click(AtlasMenu.SEND_PINS));
        button("import", 6, 138, 110, () -> click(AtlasMenu.RECEIVE_PINS));
        button("cancel", 203, 80, 47, this::onClose);
    }

    private Button button(String key, int x, int y, int width, Runnable action) {
        return addRenderableWidget(Button.builder(text(key), pressed -> action.run())
                .bounds(leftPos + x, topPos + y, width, 19).build());
    }

    private void click(int action) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, action);
    }

    @Override
    protected void containerTick() {
        if (displayed != menu.mode()) {
            tool = Tool.SELECT;
            editingPoint = null;
            init();
        }
        if (displayed == AtlasMenu.Mode.MAP && editingPoint != null
                && (editingSlot != menu.selected() || !Objects.equals(editingMapId, MapItem.getMapId(menu.selectedMap())))) {
            clearEditor();
        }
        updateButtons();
        if (displayed == AtlasMenu.Mode.MAP && editingPoint != null) {
            name.tick();
        }
    }

    private void updateButtons() {
        if (displayed == AtlasMenu.Mode.INVENTORY) {
            autoCenter.setMessage(text(menu.autoCenter() ? "on" : "off"));
            autoCreate.setMessage(text(menu.autoCreate() ? "on" : "off"));
        } else if (displayed == AtlasMenu.Mode.MAP) {
            for (int i = 0; i < arrows.length; i++) {
                arrows[i].visible = menu.hasNeighbor(i) && (i != 2 || editingPoint == null);
            }
            for (int i = 0; i < compasses.length; i++) {
                ItemStack stack = menu.contents().getItem(i);
                compasses[i].visible = stack.is(ModItems.WAYPOINT_COMPASS.get());
                if (compasses[i].visible) {
                    var target = WaypointCompassItem.getTarget(stack);
                    compasses[i].setMessage(Component.literal((menu.compass() == i ? "• " : "") + (i + 1)));
                    compasses[i].setTooltip(Tooltip.create(target.description()));
                }
            }
            needle.visible = targeting >= 0 && menu.contents().getItem(targeting).is(ModItems.WAYPOINT_COMPASS.get());
            name.visible = color.visible = save.visible = cancel.visible = editingPoint != null;
            remove.visible = editingPoint != null && editingPin != null;
        }
    }

    private void choose(Tool tool) {
        this.tool = tool;
        targeting = -1;
        clearEditor();
    }

    private void target(int slot) {
        choose(Tool.TARGET);
        targeting = slot;
    }

    private void editPin(MapPin.Point clicked, @Nullable MapPin pin) {
        editingPoint = pin == null ? clicked : pin.point();
        editingPin = pin;
        editingSlot = menu.selected();
        editingMapId = MapItem.getMapId(menu.selectedMap());
        name.setValue(pin == null ? text("number", MapPin.read(menu.selectedMap()).size() + 1).getString() : pin.name());
        colorIndex = MapPin.WOOL_COLORS.indexOf(pin == null ? DyeColor.RED : pin.color());
        updateColor();
        updateButtons();
        setInitialFocus(name);
        name.setHighlightPos(0);
    }

    private void updateColor() {
        DyeColor dye = MapPin.WOOL_COLORS.get(colorIndex);
        color.setTooltip(Tooltip.create(Component.translatable("color.minecraft." + dye.getName())));
    }

    private void clearEditor() {
        editingPoint = null;
        editingPin = null;
        if (getFocused() == name) {
            setFocused(null);
        }
        updateButtons();
    }

    private void savePin(AtlasMenu.Edit action) {
        if (editingPoint != null && editingMapId != null) {
            ModNetwork.CHANNEL.sendToServer(new ModNetwork.AtlasPinPacket(menu.containerId, action, editingSlot, editingMapId,
                    editingPin == null ? null : editingPin.id(), editingPoint, name.getValue(), MapPin.WOOL_COLORS.get(colorIndex), -1));
            clearEditor();
        }
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        if (displayed == AtlasMenu.Mode.INVENTORY && (button == 2 || hasControlDown())) {
            for (int slot = AtlasContents.SUPPLIES; slot < AtlasContents.SIZE; slot++) {
                var current = menu.getSlot(slot);
                if (current.hasItem() && isHovering(current.x, current.y, 16, 16, x, y)) {
                    click(AtlasMenu.MAP_SLOT + slot);
                    return true;
                }
            }
        }
        if (displayed == AtlasMenu.Mode.MAP && button == 0 && editingPoint == null && menu.region() != null) {
            var point = mapPoint(x, y);
            if (point != null) {
                var pins = MapPin.read(menu.selectedMap());
                var pin = MapPin.near(pins, point);
                if (tool == Tool.ADD && pin == null || tool == Tool.EDIT && pin != null) {
                    editPin(point, pin);
                } else if (tool == Tool.TARGET && targeting >= 0) {
                    Integer id = MapItem.getMapId(menu.selectedMap());
                    if (id != null) {
                        ModNetwork.CHANNEL.sendToServer(new ModNetwork.AtlasPinPacket(menu.containerId, AtlasMenu.Edit.TARGET,
                                menu.selected(), id, pin == null ? null : pin.id(), point, text("target").getString(),
                                DyeColor.RED, targeting));
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(x, y, button);
    }

    private @Nullable MapPin.Point mapPoint(double x, double y) {
        double u = (x - leftPos - MAP_X) / MAP_SIZE;
        double v = (y - topPos - MAP_Y) / MAP_SIZE;
        return MapPin.Point.valid(u, v) ? new MapPin.Point(u, v) : null;
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (editingPoint != null && getFocused() == name && minecraft.options.keyInventory.matches(key, scanCode)) {
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        switch (displayed) {
            case INVENTORY -> {
                graphics.blit(INVENTORY, leftPos, topPos, 0, 0, imageWidth, imageHeight);
                int slot = menu.selected();
                if (slot >= AtlasContents.SUPPLIES) {
                    var selected = menu.getSlot(slot);
                    graphics.blit(BUTTONS, leftPos + selected.x - 1, topPos + selected.y - 1, 0, 0, 18, 18);
                }
            }
            case MAP -> drawMap(graphics);
            case TRANSFER -> graphics.blit(TRANSFER, leftPos, topPos + 75, 256, 96, 0, 0, 128, 48, 128, 128);
        }
    }

    private void drawMap(GuiGraphics graphics) {
        int x = leftPos + MAP_X;
        int y = topPos + MAP_Y;
        graphics.fill(x - 3, y - 3, x + MAP_SIZE + 3, y + MAP_SIZE + 3, 0xff294b63);
        graphics.fill(x, y, x + MAP_SIZE, y + MAP_SIZE, 0xffd1b78b);
        var region = menu.region();
        if (region == null || minecraft.level == null) {
            graphics.drawCenteredString(font, text("no_map"), x + MAP_SIZE / 2, y + MAP_SIZE / 2, 0xffffff);
            return;
        }
        ItemStack stack = menu.selectedMap();
        var pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 2);
        pose.scale(MAP_SIZE / 128f, MAP_SIZE / 128f, 1);
        DisplayedMapRenderer.renderPixels(pose, graphics.bufferSource(), stack, minecraft.level, true, LightTexture.FULL_BRIGHT);
        graphics.flush();
        pose.popPose();
        if (region.currentDimension() && minecraft.player != null) {
            var player = minecraft.player;
            double u = (player.getX() - region.x()) / region.width() + .5;
            double v = (player.getZ() - region.z()) / region.width() + .5;
            if (MapPin.Point.valid(u, v)) {
                pose.pushPose();
                pose.translate(x + u * MAP_SIZE, y + v * MAP_SIZE, 3);
                pose.mulPose(Axis.ZP.rotationDegrees(player.getYRot() + 180));
                graphics.fill(-1, -4, 2, 4, 0xffeeeeee);
                graphics.fill(-2, 2, 3, 4, 0xff333333);
                pose.popPose();
            }
        }
        for (MapPin pin : MapPin.read(stack)) {
            point(graphics, x, y, pin.point(), 0xff000000 | pin.color().getTextColor());
        }
        if (targeting >= 0 && menu.contents().getItem(targeting).is(ModItems.WAYPOINT_COMPASS.get())) {
            var target = WaypointCompassItem.getTarget(menu.contents().getItem(targeting));
            if (region.currentDimension() && (target.dimension() == null || target.dimension().equals(minecraft.level.dimension().location()))) {
                var location = new MapPin.Point((target.x() - region.x()) / (double) region.width() + .5,
                        (target.z() - region.z()) / (double) region.width() + .5);
                if (MapPin.Point.valid(location.u(), location.v())) {
                    point(graphics, x, y, location, 0xffffffff);
                }
            }
        }
    }

    private void point(GuiGraphics graphics, int x, int y, MapPin.Point point, int color) {
        int px = x + (int) (point.u() * (MAP_SIZE - 1));
        int py = y + (int) (point.v() * (MAP_SIZE - 1));
        graphics.fill(px - 3, py - 3, px + 4, py + 4, 4, 0xff292929);
        graphics.fill(px - 2, py - 2, px + 3, py + 3, 5, color);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        var region = menu.region();
        switch (displayed) {
            case INVENTORY -> {
                graphics.drawString(font, text("title"), 37, 12, 0x403522, false);
                graphics.drawString(font, text("zoom"), 12, 28, 0x403522, false);
                graphics.drawString(font, Integer.toString(menu.zoom() + 1), 86, 28, 0x264d7c, false);
                graphics.drawString(font, text("center"), 12, 52, 0x403522, false);
                graphics.drawString(font, text("create"), 12, 77, 0x403522, false);
                if (region != null) {
                    graphics.drawString(font, text("x", region.x()), 12, 94, 0x403522, false);
                    graphics.drawString(font, text("z", region.z()), 12, 103, 0x403522, false);
                    graphics.drawString(font, text("scale", region.scale() + 1), 12, 112, 0x403522, false);
                }
            }
            case MAP -> {
                graphics.drawString(font, title, 3, 10, 0xffffff);
                if (editingPoint == null) {
                    graphics.drawCenteredString(font, text("tool." + tool.name().toLowerCase(java.util.Locale.ROOT)),
                            128, 204, 0xffffff);
                }
            }
            case TRANSFER -> {
                graphics.drawCenteredString(font, text("transfer"), 128, 86, 0xffffff);
                graphics.drawString(font, text("pins_atlas", MapPin.read(menu.selectedMap()).size()), 121, 111, 0x403522, false);
                graphics.drawString(font, text("pins_frame", menu.framePins()), 121, 144, 0x403522, false);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (displayed == AtlasMenu.Mode.MAP) {
            if (editingPoint != null) {
                graphics.fill(color.getX() + 3, color.getY() + 3, color.getX() + 17, color.getY() + 16,
                        0xff000000 | MapPin.WOOL_COLORS.get(colorIndex).getTextColor());
            } else {
                var point = mapPoint(mouseX, mouseY);
                var pin = point == null ? null : MapPin.near(MapPin.read(menu.selectedMap()), point);
                if (pin != null) {
                    graphics.renderTooltip(font, Component.literal(pin.name()), mouseX, mouseY);
                }
            }
        } else if (displayed == AtlasMenu.Mode.INVENTORY) {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "textures/gui/" + name + ".png");
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.bibliocraft.atlas." + key, args);
    }

    private enum Tool { SELECT, ADD, EDIT, TARGET }
}
