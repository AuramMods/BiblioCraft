package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.MarkerPoleBlock;
import art.arcane.bibliocraft.item.TapeMeasureItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MarkerPoleBlockEntity extends BlockEntity {
    private static final int CHECK_INTERVAL = 20;
    private UUID owner;
    private UUID measurement;

    public MarkerPoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MARKER_POLE.get(), pos, state);
    }

    public void bind(ServerLevel level, UUID player, UUID id) {
        owner = player;
        measurement = id;
        var active = ActiveMarkers.get(level);
        active.ids.add(id);
        active.setDirty();
        setChanged();
    }

    public void unregister(ServerLevel level) {
        if (measurement != null) {
            ActiveMarkers.get(level).remove(measurement);
        }
    }

    public static void remove(ServerLevel level, BlockPos pos, UUID id) {
        ActiveMarkers.get(level).remove(id);
        if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof MarkerPoleBlockEntity pole
                && pole.getBlockState().getValue(MarkerPoleBlock.TEMPORARY) && id.equals(pole.measurement)) {
            level.removeBlock(pos, false);
        }
    }

    public void checkMeasurement(ServerLevel level) {
        var player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (measurement == null || !ActiveMarkers.get(level).ids.contains(measurement)
                || (player != null && !inInventory(player.getInventory(), measurement))) {
            level.removeBlock(worldPosition, false);
        } else {
            scheduleCheck(level);
        }
    }

    private static boolean inInventory(Inventory inventory, UUID id) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (TapeMeasureItem.hasMarker(inventory.getItem(slot), id)) {
                return true;
            }
        }
        return TapeMeasureItem.hasMarker(inventory.player.containerMenu.getCarried(), id);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && getBlockState().getValue(MarkerPoleBlock.TEMPORARY)) {
            scheduleCheck(server);
        }
    }

    private void scheduleCheck(ServerLevel level) {
        level.scheduleTick(worldPosition, getBlockState().getBlock(), CHECK_INTERVAL);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        measurement = tag.hasUUID("Measurement") ? tag.getUUID("Measurement") : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (owner != null && measurement != null) {
            tag.putUUID("Owner", owner);
            tag.putUUID("Measurement", measurement);
        }
    }

    // The saved IDs let a completed measurement remove its marker when an unloaded origin next ticks.
    private static final class ActiveMarkers extends SavedData {
        private final Set<UUID> ids = new HashSet<>();

        private static ActiveMarkers get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(ActiveMarkers::load, ActiveMarkers::new, "bibliocraft_markers");
        }

        private void remove(UUID id) {
            if (ids.remove(id)) {
                setDirty();
            }
        }

        private static ActiveMarkers load(CompoundTag tag) {
            var active = new ActiveMarkers();
            for (Tag id : tag.getList("Markers", Tag.TAG_INT_ARRAY)) {
                if (id instanceof net.minecraft.nbt.IntArrayTag array && array.size() == 4) {
                    active.ids.add(NbtUtils.loadUUID(id));
                }
            }
            return active;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            var markers = new ListTag();
            for (UUID id : ids) {
                markers.add(NbtUtils.createUUID(id));
            }
            tag.put("Markers", markers);
            return tag;
        }
    }
}
