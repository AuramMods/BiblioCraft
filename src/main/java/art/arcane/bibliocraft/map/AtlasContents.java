package art.arcane.bibliocraft.map;

import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class AtlasContents extends SimpleContainer {
    public static final int SUPPLIES = 6;
    public static final int SIZE = 48;
    private static final String INVENTORY = "Inventory";
    private final ItemStack atlas;

    public AtlasContents(ItemStack atlas) {
        super(SIZE);
        this.atlas = atlas;
        for (Tag entry : items(atlas)) {
            CompoundTag saved = (CompoundTag) entry;
            int slot = saved.getByte("Slot");
            if (slot >= 0 && slot < SIZE) {
                super.setItem(slot, ItemStack.of(saved));
            }
        }
    }

    private static ListTag items(ItemStack atlas) {
        var tag = atlas.getTag();
        return tag == null ? new ListTag() : tag.getList(INVENTORY, Tag.TAG_COMPOUND);
    }

    private static ItemStack itemAt(ItemStack atlas, int slot) {
        if (slot >= 0 && slot < SIZE) {
            for (Tag entry : items(atlas)) {
                CompoundTag saved = (CompoundTag) entry;
                if (saved.getByte("Slot") == slot) {
                    return ItemStack.of(saved);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public static ItemStack displayedMap(ItemStack atlas) {
        return itemAt(atlas, savedSlot(atlas, "mapSlot"));
    }

    public static @Nullable WaypointCompassItem.Target displayedCompass(ItemStack atlas) {
        int slot = savedSlot(atlas, "savedCompass");
        var compass = slot < SUPPLIES ? itemAt(atlas, slot) : ItemStack.EMPTY;
        return compass.is(ModItems.WAYPOINT_COMPASS.get()) ? WaypointCompassItem.getTarget(compass) : null;
    }

    private static int savedSlot(ItemStack atlas, String key) {
        var tag = atlas.getTag();
        return tag != null && tag.contains(key, Tag.TAG_ANY_NUMERIC) ? tag.getInt(key) : -1;
    }

    public static boolean accepts(int slot, ItemStack stack) {
        if (slot >= SUPPLIES) {
            return stack.is(Items.FILLED_MAP) || stack.is(Items.MAP);
        }
        return stack.is(Items.MAP) || stack.is(Items.WRITABLE_BOOK) || stack.is(Items.WRITTEN_BOOK)
                || stack.is(ModItems.WAYPOINT_COMPASS.get()) || stack.is(ModItems.BIG_BOOK.get())
                || stack.is(ModItems.BIBLIO_CLIPBOARD.get()) || stack.is(ModItems.MAP_TOOL.get())
                || stack.is(ModItems.TAPE_MEASURE.get()) || stack.is(ModItems.HAND_DRILL.get())
                || stack.is(ModItems.BIBLIO_DRILL.get());
    }

    public void saveInventory() {
        ListTag saved = new ListTag();
        for (int slot = 0; slot < SIZE; slot++) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                CompoundTag item = stack.save(new CompoundTag());
                item.putByte("Slot", (byte) slot);
                saved.add(item);
            }
        }
        atlas.getOrCreateTag().put(INVENTORY, saved);
    }

    public boolean autoCenter() {
        return atlas.getTag() != null && atlas.getTag().getBoolean("autoCenter");
    }

    public boolean autoCreate() {
        return atlas.getTag() != null && atlas.getTag().getBoolean("autoCreate");
    }

    public int zoom() {
        return atlas.getTag() == null ? 0 : Mth.clamp(atlas.getTag().getInt("zoomLevel"), 0, 4);
    }

    public boolean opensMap() {
        return atlas.getTag() != null && atlas.getTag().getInt("lastGUImode") == 1;
    }

    public void opensMap(boolean map) {
        atlas.getOrCreateTag().putInt("lastGUImode", map ? 1 : 0);
    }

    public void setOptions(boolean autoCenter, boolean autoCreate, int zoom) {
        var tag = atlas.getOrCreateTag();
        tag.putBoolean("autoCenter", autoCenter);
        tag.putBoolean("autoCreate", autoCenter && autoCreate);
        tag.putInt("zoomLevel", Mth.clamp(zoom, 0, 4));
    }

    public int selected() {
        int slot = savedSlot(atlas, "mapSlot");
        return isFilled(slot) ? slot : -1;
    }

    public void select(int slot) {
        atlas.getOrCreateTag().putInt("mapSlot", isFilled(slot) ? slot : -1);
    }

    public boolean isFilled(int slot) {
        return slot >= SUPPLIES && slot < SIZE && getItem(slot).is(Items.FILLED_MAP);
    }

    public int filledMapCount() {
        int count = 0;
        for (int slot = SUPPLIES; slot < SIZE; slot++) {
            if (isFilled(slot)) {
                count++;
            }
        }
        return count;
    }

    public ItemStack selectedMap() {
        return getItem(selected());
    }

    public int selectedCompass() {
        int slot = savedSlot(atlas, "savedCompass");
        return isCompass(slot) ? slot : -1;
    }

    public boolean isCompass(int slot) {
        return slot >= 0 && slot < SUPPLIES && getItem(slot).is(ModItems.WAYPOINT_COMPASS.get());
    }

    public void selectCompass(int slot) {
        atlas.getOrCreateTag().putInt("savedCompass", isCompass(slot) ? slot : -1);
    }

    public int matchingMap(ItemStack map) {
        Integer id = map.is(Items.FILLED_MAP) ? MapItem.getMapId(map) : null;
        if (id != null) {
            for (int slot = SUPPLIES; slot < SIZE; slot++) {
                if (isFilled(slot) && id.equals(MapItem.getMapId(getItem(slot)))) {
                    return slot;
                }
            }
        }
        return -1;
    }

    public int neighbor(Level level, @Nullable MapItemSavedData selected, Direction direction) {
        if (selected == null) {
            return -1;
        }
        int width = 128 << selected.scale;
        int x = selected.centerX + width * direction.getStepX();
        int z = selected.centerZ + width * direction.getStepZ();
        for (int slot = SUPPLIES; slot < SIZE; slot++) {
            var candidate = mapData(level, slot);
            if (candidate != null && candidate.scale == selected.scale && candidate.dimension.equals(selected.dimension)
                    && candidate.centerX == x && candidate.centerZ == z) {
                return slot;
            }
        }
        return -1;
    }

    public @Nullable MapItemSavedData mapData(Level level, int slot) {
        return isFilled(slot) ? MapItem.getSavedData(getItem(slot), level) : null;
    }

    public boolean updateSelection(Player player, boolean mayCreate) {
        if (!autoCenter()) {
            return false;
        }
        Level level = player.level();
        int previous = selected();
        int scale = zoom();
        if (contains(mapData(level, previous), player, scale)) {
            return false;
        }
        for (int slot = SUPPLIES; slot < SIZE; slot++) {
            if (contains(mapData(level, slot), player, scale)) {
                select(slot);
                return true;
            }
        }
        int destination = mayCreate && autoCreate() ? writingSlot() : -1;
        if (destination >= 0) {
            var map = MapItem.create(level, player.getBlockX(), player.getBlockZ(), (byte) scale, true, false);
            putNewMap(destination, map);
            return true;
        }
        if (previous != -1) {
            select(-1);
            return true;
        }
        return false;
    }

    private static boolean contains(@Nullable MapItemSavedData map, Player player, int scale) {
        if (map == null || map.scale != scale || !map.dimension.equals(player.level().dimension())) {
            return false;
        }
        int radius = 64 << scale;
        double x = player.getX() - map.centerX;
        double z = player.getZ() - map.centerZ;
        return x >= -radius && x < radius && z >= -radius && z < radius;
    }

    public boolean copyIn(ItemStack map) {
        int destination = writingSlot();
        if (destination < 0 || !map.is(Items.FILLED_MAP) || MapItem.getMapId(map) == null || matchingMap(map) >= 0) {
            return false;
        }
        putNewMap(destination, map.copyWithCount(1));
        return true;
    }

    private void putNewMap(int slot, ItemStack map) {
        if (getItem(slot).isEmpty()) {
            removeItem(reserveBlank(), 1);
        }
        setItem(slot, map);
        select(slot);
        saveInventory();
    }

    private int writingSlot() {
        int empty = -1;
        for (int slot = SUPPLIES; slot < SIZE; slot++) {
            if (getItem(slot).is(Items.MAP)) {
                return slot;
            }
            if (empty < 0 && getItem(slot).isEmpty()) {
                empty = slot;
            }
        }
        return empty >= 0 && reserveBlank() >= 0 ? empty : -1;
    }

    public ItemStack copyOut() {
        ItemStack map = selectedMap();
        int blank = reserveBlank();
        if (blank < 0 || map.isEmpty() || MapItem.getMapId(map) == null) {
            return ItemStack.EMPTY;
        }
        removeItem(blank, 1);
        saveInventory();
        return map.copyWithCount(1);
    }

    private int reserveBlank() {
        for (int slot = 0; slot < SUPPLIES; slot++) {
            if (getItem(slot).is(Items.MAP)) {
                return slot;
            }
        }
        return -1;
    }

    public static boolean sameMap(ItemStack first, ItemStack second) {
        return first.is(Items.FILLED_MAP) && second.is(Items.FILLED_MAP) && MapItem.getMapId(first) != null
                && Objects.equals(MapItem.getMapId(first), MapItem.getMapId(second));
    }
}
