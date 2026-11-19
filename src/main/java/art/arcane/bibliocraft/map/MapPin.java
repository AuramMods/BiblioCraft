package art.arcane.bibliocraft.map;

import art.arcane.bibliocraft.catalog.CatalogContents;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

public record MapPin(UUID id, Point point, String name, DyeColor color) {
    public static final int LIMIT = 32;
    public static final int NAME_LENGTH = 42;
    public static final List<DyeColor> WOOL_COLORS = List.of(DyeColor.BLACK, DyeColor.RED, DyeColor.GREEN, DyeColor.LIME,
            DyeColor.BROWN, DyeColor.BLUE, DyeColor.CYAN, DyeColor.LIGHT_BLUE, DyeColor.PURPLE, DyeColor.MAGENTA,
            DyeColor.PINK, DyeColor.YELLOW, DyeColor.ORANGE, DyeColor.GRAY, DyeColor.LIGHT_GRAY, DyeColor.WHITE);
    private static final double CLICK_RADIUS = 0.026;
    private static final String TAG = "BiblioMapPins";

    public MapPin {
        name = CatalogContents.bounded(name, NAME_LENGTH);
    }

    public static List<MapPin> read(ItemStack map) {
        CompoundTag tag = map.getTag();
        if (tag == null) {
            return List.of();
        }
        ListTag pins = tag.getList(TAG, Tag.TAG_COMPOUND);
        List<MapPin> result = new ArrayList<>(Math.min(LIMIT, pins.size()));
        for (int i = 0; i < pins.size() && result.size() < LIMIT; i++) {
            CompoundTag pin = pins.getCompound(i);
            double u = pin.getDouble("U");
            double v = pin.getDouble("V");
            if (pin.hasUUID("Id") && pin.contains("U", Tag.TAG_ANY_NUMERIC) && pin.contains("V", Tag.TAG_ANY_NUMERIC)
                    && Point.valid(u, v)) {
                UUID id = pin.getUUID("Id");
                if (result.stream().noneMatch(existing -> existing.id().equals(id))) {
                    result.add(new MapPin(id, new Point(u, v), pin.getString("Name"), DyeColor.byId(pin.getInt("Color"))));
                }
            }
        }
        return List.copyOf(result);
    }

    public static void write(ItemStack map, List<MapPin> pins) {
        if (pins.isEmpty()) {
            map.removeTagKey(TAG);
            return;
        }
        var items = new ListTag();
        for (MapPin pin : pins.stream().limit(LIMIT).toList()) {
            var tag = new CompoundTag();
            tag.putUUID("Id", pin.id());
            tag.putDouble("U", pin.point().u());
            tag.putDouble("V", pin.point().v());
            tag.putString("Name", pin.name());
            tag.putInt("Color", pin.color().getId());
            items.add(tag);
        }
        map.getOrCreateTag().put(TAG, items);
    }

    public static @Nullable MapPin near(List<MapPin> pins, Point point) {
        MapPin nearest = null;
        double distance = CLICK_RADIUS * CLICK_RADIUS;
        for (MapPin pin : pins) {
            double candidate = point.distanceSquared(pin.point());
            if (candidate < distance) {
                distance = candidate;
                nearest = pin;
            }
        }
        return nearest;
    }

    public WaypointCompassItem.Target target(MapItemSavedData map) {
        int width = 128 << map.scale;
        return new WaypointCompassItem.Target(Mth.floor(map.centerX + (point.u() - 0.5) * width),
                Mth.floor(map.centerZ + (point.v() - 0.5) * width), name, map.dimension.location());
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(id);
        buffer.writeDouble(point.u()).writeDouble(point.v());
        buffer.writeUtf(name, NAME_LENGTH).writeEnum(color);
    }

    public static MapPin read(FriendlyByteBuf buffer) {
        UUID id = buffer.readUUID();
        Point point = new Point(buffer.readDouble(), buffer.readDouble());
        return new MapPin(id, point, buffer.readUtf(NAME_LENGTH), buffer.readEnum(DyeColor.class));
    }

    public record Point(double u, double v) {
        public Point rotate(int turns) {
            return switch (turns & 3) {
                case 1 -> new Point(1 - v, u);
                case 2 -> new Point(1 - u, 1 - v);
                case 3 -> new Point(v, 1 - u);
                default -> this;
            };
        }

        public static boolean valid(double u, double v) {
            return u >= 0 && u <= 1 && v >= 0 && v <= 1;
        }

        public double distanceSquared(Point other) {
            double du = u - other.u;
            double dv = v - other.v;
            return du * du + dv * dv;
        }
    }
}
