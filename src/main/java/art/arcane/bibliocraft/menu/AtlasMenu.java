package art.arcane.bibliocraft.menu;

import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.AtlasItem;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModMenus;
import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class AtlasMenu extends HeldItemMenu {
    public static final int SHOW_MAP = 0;
    public static final int SHOW_INVENTORY = 1;
    public static final int AUTO_CENTER = 2;
    public static final int AUTO_CREATE = 3;
    public static final int ZOOM_OUT = 4;
    public static final int ZOOM_IN = 5;
    public static final int SEND_PINS = 6;
    public static final int RECEIVE_PINS = 7;
    public static final int NEIGHBOR = 10;
    public static final int MAP_SLOT = 100;
    public static final int COMPASS_SLOT = 200;
    public static final List<Direction> DIRECTIONS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);

    private static final int MODE = 0;
    private static final int SELECTED = 1;
    private static final int COMPASS = 2;
    private static final int SETTINGS = 3;
    private static final int X_LOW = 4;
    private static final int X_HIGH = 5;
    private static final int Z_LOW = 6;
    private static final int Z_HIGH = 7;
    private static final int REGION = 8;
    private static final int FRAME_PINS = 9;
    private static final int NEIGHBORS = 10;
    private final SimpleContainerData data = new SimpleContainerData(11);
    private final AtlasContents contents;
    private final @Nullable MapFrameBlockEntity frame;
    private final ItemStack transferMap;
    private final int lockedHotbar;
    private final InteractionHand hand;

    public AtlasMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, buffer.readEnum(InteractionHand.class), null);
    }

    public AtlasMenu(int id, Inventory inventory, InteractionHand hand, @Nullable MapFrameBlockEntity frame) {
        super(ModMenus.ATLAS.get(), id, inventory.player, hand);
        this.hand = hand;
        this.frame = frame;
        transferMap = frame == null ? ItemStack.EMPTY : frame.getItem(MapFrameBlockEntity.MAP);
        lockedHotbar = hand == InteractionHand.MAIN_HAND ? inventory.selected : 40;
        contents = new AtlasContents(heldStack);
        data.set(MODE, (frame != null ? Mode.TRANSFER : contents.opensMap() ? Mode.MAP : Mode.INVENTORY).ordinal());
        addDataSlots(data);
        for (int slot = 0; slot < AtlasContents.SIZE; slot++) {
            int x = slot < AtlasContents.SUPPLIES ? 11 + slot * 18 : 138 + (slot - AtlasContents.SUPPLIES) % 6 * 18;
            int y = slot < AtlasContents.SUPPLIES ? 123 : 15 + (slot - AtlasContents.SUPPLIES) / 6 * 18;
            addSlot(new Slot(contents, slot, x, y) {
                @Override
                public boolean isActive() {
                    return mode() == Mode.INVENTORY;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isActive() && AtlasContents.accepts(getContainerSlot(), stack);
                }

                @Override
                public boolean mayPickup(Player player) {
                    return isActive();
                }

                @Override
                public int getMaxStackSize() {
                    return getContainerSlot() >= AtlasContents.SUPPLIES ? 1 : super.getMaxStackSize();
                }
            });
        }
        for (int i = 0; i < 27; i++) {
            addPlayerSlot(inventory, 9 + i, 48 + i % 9 * 18, 159 + i / 9 * 18);
        }
        for (int i = 0; i < 9; i++) {
            addPlayerSlot(inventory, i, 48 + i * 18, 217);
        }
        if (!owner.level().isClientSide) {
            contents.addListener(container -> inventoryChanged());
            refresh();
        }
    }

    public static void open(ServerPlayer player, InteractionHand hand, @Nullable MapFrameBlockEntity frame) {
        ItemStack stack = player.getItemInHand(hand);
        NetworkHooks.openScreen(player, new SimpleMenuProvider(
                (id, inventory, owner) -> new AtlasMenu(id, inventory, hand, frame), stack.getHoverName()),
                buffer -> buffer.writeEnum(hand));
        AtlasItem.sendMap(player, AtlasContents.displayedMap(stack), false);
    }

    private void addPlayerSlot(Inventory inventory, int slot, int x, int y) {
        addSlot(new Slot(inventory, slot, x, y) {
            @Override
            public boolean isActive() {
                return mode() == Mode.INVENTORY;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return isActive() && slot != lockedHotbar;
            }

            @Override
            public boolean mayPickup(Player player) {
                return isActive() && slot != lockedHotbar;
            }
        });
    }

    public AtlasContents contents() {
        return contents;
    }

    public Mode mode() {
        return Mode.values()[data.get(MODE)];
    }

    public int selected() {
        return data.get(SELECTED);
    }

    public int compass() {
        return data.get(COMPASS);
    }

    public boolean autoCenter() {
        return (data.get(SETTINGS) & 1) != 0;
    }

    public boolean autoCreate() {
        return (data.get(SETTINGS) & 2) != 0;
    }

    public int zoom() {
        return data.get(SETTINGS) >> 2;
    }

    public int framePins() {
        return data.get(FRAME_PINS);
    }

    public boolean hasNeighbor(int index) {
        return (data.get(NEIGHBORS) & (1 << index)) != 0;
    }

    public ItemStack selectedMap() {
        return contents.getItem(selected());
    }

    public @Nullable Region region() {
        int region = data.get(REGION);
        return region == 0 ? null : new Region(coordinate(X_LOW, X_HIGH), coordinate(Z_LOW, Z_HIGH),
                (region & 7) - 1, (region & 8) != 0);
    }

    private int coordinate(int low, int high) {
        return data.get(high) << 16 | data.get(low) & 0xffff;
    }

    public void refresh() {
        int selected = contents.selected();
        data.set(SELECTED, selected);
        data.set(COMPASS, contents.selectedCompass());
        data.set(SETTINGS, (contents.zoom() << 2) | (contents.autoCenter() ? 1 : 0) | (contents.autoCreate() ? 2 : 0));
        data.set(FRAME_PINS, frame == null ? 0 : frame.pins().size());
        var map = contents.mapData(owner.level(), selected);
        if (map == null) {
            data.set(REGION, 0);
            data.set(NEIGHBORS, 0);
            return;
        }
        data.set(X_LOW, (short) map.centerX);
        data.set(X_HIGH, map.centerX >> 16);
        data.set(Z_LOW, (short) map.centerZ);
        data.set(Z_HIGH, map.centerZ >> 16);
        data.set(REGION, (map.scale + 1) | (map.dimension.equals(owner.level().dimension()) ? 8 : 0));
        int neighbors = 0;
        for (int index = 0; index < DIRECTIONS.size(); index++) {
            if (contents.neighbor(owner.level(), map, DIRECTIONS.get(index)) >= 0) {
                neighbors |= 1 << index;
            }
        }
        data.set(NEIGHBORS, neighbors);
    }

    private void inventoryChanged() {
        contents.saveInventory();
        if (contents.selectedCompass() < 0) {
            contents.selectCompass(-1);
        }
        if (!contents.autoCenter() && contents.selected() < 0) {
            contents.select(-1);
            for (int slot = AtlasContents.SUPPLIES; slot < AtlasContents.SIZE; slot++) {
                if (contents.isFilled(slot)) {
                    contents.select(slot);
                    break;
                }
            }
        }
        owner.getInventory().setChanged();
        refresh();
    }

    public boolean owns(ItemStack stack, Player player) {
        return heldStack == stack && stillValid(player);
    }

    @Override
    public boolean stillValid(Player player) {
        if (player.level().isClientSide) {
            return owner == player && player.getItemInHand(hand).is(ModItems.ATLAS_BOOK.get());
        }
        return super.stillValid(player) && heldStack.is(ModItems.ATLAS_BOOK.get()) && BiblioConfig.COMMON.enableAtlas.get()
                && (frame == null || BiblioConfig.COMMON.enableMapFrame.get() && frame.stillValid(player)
                && frame.getItem(MapFrameBlockEntity.MAP) == transferMap
                && AtlasContents.sameMap(transferMap, contents.selectedMap()));
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (mode() == Mode.INVENTORY && stillValid(player) && (type != ClickType.SWAP || button != lockedHotbar)) {
            super.clicked(slot, button, type, player);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (mode() != Mode.INVENTORY || !stillValid(player) || index < 0 || index >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = getSlot(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        boolean moved = index < AtlasContents.SIZE ? moveItemStackTo(current, AtlasContents.SIZE, slots.size(), true)
                : AtlasContents.accepts(0, current) ? moveItemStackTo(current, 0, AtlasContents.SUPPLIES, false)
                : moveItemStackTo(current, AtlasContents.SUPPLIES, AtlasContents.SIZE, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (current.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, current);
        return original;
    }

    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (!(player instanceof ServerPlayer serverPlayer) || player.containerMenu != this || !stillValid(player)) {
            return false;
        }
        if (frame != null) {
            if (button == SEND_PINS) {
                frame.replacePins(MapPin.read(contents.selectedMap()));
            } else if (button == RECEIVE_PINS) {
                MapPin.write(contents.selectedMap(), frame.pins());
                inventoryChanged();
            } else {
                return false;
            }
            serverPlayer.closeContainer();
            return true;
        }
        if (!getCarried().isEmpty()) {
            return false;
        }
        if (button == SHOW_MAP || button == SHOW_INVENTORY) {
            boolean showMap = button == SHOW_MAP;
            contents.opensMap(showMap);
            data.set(MODE, (showMap ? Mode.MAP : Mode.INVENTORY).ordinal());
        } else if (button >= MAP_SLOT && button < MAP_SLOT + AtlasContents.SIZE && contents.isFilled(button - MAP_SLOT)) {
            contents.select(button - MAP_SLOT);
            contents.setOptions(false, false, contents.zoom());
        } else if (button >= COMPASS_SLOT && button < COMPASS_SLOT + AtlasContents.SUPPLIES && contents.isCompass(button - COMPASS_SLOT)) {
            int slot = button - COMPASS_SLOT;
            contents.selectCompass(contents.selectedCompass() == slot ? -1 : slot);
        } else if (button >= NEIGHBOR && button < NEIGHBOR + DIRECTIONS.size()) {
            int next = contents.neighbor(player.level(), contents.mapData(player.level(), contents.selected()),
                    DIRECTIONS.get(button - NEIGHBOR));
            if (next < 0) {
                return false;
            }
            contents.select(next);
            contents.setOptions(false, false, contents.zoom());
        } else if (button == AUTO_CENTER) {
            contents.setOptions(!contents.autoCenter(), false, contents.zoom());
        } else if (button == AUTO_CREATE) {
            contents.setOptions(contents.autoCenter() || !contents.autoCreate(), !contents.autoCreate(), contents.zoom());
        } else if (button == ZOOM_IN || button == ZOOM_OUT) {
            contents.setOptions(contents.autoCenter(), contents.autoCreate(), contents.zoom() + (button == ZOOM_IN ? -1 : 1));
        } else {
            return false;
        }
        owner.getInventory().setChanged();
        refresh();
        AtlasItem.sendMap(serverPlayer, contents.selectedMap(), false);
        return true;
    }

    public boolean edit(Player player, Edit action, int slot, int mapId, @Nullable UUID pinId, MapPin.Point position,
                        String name, DyeColor color, int compassSlot) {
        if (mode() != Mode.MAP || player.containerMenu != this || !stillValid(player) || slot != contents.selected()
                || !MapPin.Point.valid(position.u(), position.v())) {
            return false;
        }
        ItemStack map = contents.selectedMap();
        var saved = contents.mapData(player.level(), slot);
        if (saved == null || !Objects.equals(MapItem.getMapId(map), mapId)) {
            return false;
        }
        var pins = new ArrayList<>(MapPin.read(map));
        MapPin pin = pins.stream().filter(entry -> entry.id().equals(pinId)).findFirst().orElse(null);
        String label = SharedConstants.filterText(name).strip();
        switch (action) {
            case ADD -> {
                if (pins.size() >= MapPin.LIMIT || MapPin.near(pins, position) != null) {
                    return false;
                }
                pins.add(new MapPin(UUID.randomUUID(), position, label, color));
            }
            case CHANGE, REMOVE -> {
                if (pin == null) {
                    return false;
                }
                if (action == Edit.REMOVE) {
                    pins.remove(pin);
                } else {
                    pins.set(pins.indexOf(pin), new MapPin(pin.id(), pin.point(), label, color));
                }
            }
            case TARGET -> {
                if (!BiblioConfig.COMMON.enableWaypointCompass.get() || !contents.isCompass(compassSlot)
                        || pinId != null && pin == null) {
                    return false;
                }
                MapPin target = pin == null ? new MapPin(UUID.randomUUID(), position, label, color) : pin;
                WaypointCompassItem.setTarget(contents.getItem(compassSlot), target.target(saved));
                contents.selectCompass(compassSlot);
                contents.setChanged();
                return true;
            }
        }
        MapPin.write(map, pins);
        contents.setChanged();
        return true;
    }

    public enum Mode { INVENTORY, MAP, TRANSFER }
    public enum Edit { ADD, CHANGE, REMOVE, TARGET }
    public record Region(int x, int z, int scale, boolean currentDimension) {
        public int width() {
            return 128 << scale;
        }
    }
}
