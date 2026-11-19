package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MapFrameBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int MAP = 0;
    private int rotation;
    private List<MapPin> pins = List.of();

    public MapFrameBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAP_FRAME.get(), pos, state, 1);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == MAP && stack.is(Items.FILLED_MAP);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return 1;
    }

    public int rotation() {
        return rotation;
    }

    public void rotateMap() {
        rotation = (rotation + 1) & 3;
        setChanged();
    }

    public List<MapPin> pins() {
        return getItem(MAP).isEmpty() ? List.of() : pins;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        pins = MapPin.read(stack);
        super.setItem(slot, stack);
    }

    public @Nullable MapPin pinAt(MapPin.Point point) {
        return MapPin.near(pins(), point);
    }

    public boolean addPin(MapPin pin) {
        var pins = new ArrayList<>(pins());
        if (pins.size() >= MapPin.LIMIT || pinAt(pin.point()) != null || !getItem(MAP).is(Items.FILLED_MAP)) {
            return false;
        }
        pins.add(pin);
        savePins(pins);
        return true;
    }

    public boolean changePin(MapPin original, @Nullable MapPin replacement) {
        List<MapPin> pins = new ArrayList<>(pins());
        int index = pins.indexOf(original);
        if (index == -1) {
            return false;
        }
        if (replacement == null) {
            pins.remove(index);
        } else {
            pins.set(index, replacement);
        }
        savePins(pins);
        return true;
    }

    private void savePins(List<MapPin> pins) {
        MapPin.write(getItem(MAP), pins);
        this.pins = List.copyOf(pins);
        setChanged();
    }

    public void replacePins(List<MapPin> pins) {
        if (getItem(MAP).is(Items.FILLED_MAP)) {
            savePins(pins);
        }
    }

    public void tickMap(ServerLevel server) {
        if (BiblioConfig.COMMON.enableMapFrame.get()) {
            DisplayedMap.tick(server, worldPosition, getItem(MAP));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        rotation = tag.getInt("mapRotation") & 3;
        pins = MapPin.read(getItem(MAP));
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("mapRotation", rotation);
    }
}
