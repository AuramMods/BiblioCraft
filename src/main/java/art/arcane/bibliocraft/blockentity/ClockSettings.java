package art.arcane.bibliocraft.blockentity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public record ClockSettings(long chimes, long redstone, boolean ticking, boolean chimeEnabled,
                            boolean redstoneEnabled, boolean pulse) {
    public static final String TAG = "ClockSettings";
    public static final int SLOTS = 48;
    public static final int SLOT_TICKS = 500;
    private static final long SLOT_MASK = (1L << SLOTS) - 1;
    public static final ClockSettings DEFAULT = new ClockSettings(0, 0, true, false, false, true);

    public ClockSettings {
        chimes &= SLOT_MASK;
        redstone &= SLOT_MASK;
    }

    public static int slot(long dayTime) {
        // World tick zero is sunrise at 06:00. The dial starts at midnight.
        return (Math.floorMod(Math.floorDiv(dayTime, SLOT_TICKS), SLOTS) + 12) % SLOTS;
    }

    public static boolean includes(long schedule, int slot) {
        return (schedule & (1L << slot)) != 0;
    }

    public ClockSettings mergeEdits(ClockSettings opened, ClockSettings edited) {
        return new ClockSettings(mergeSchedule(chimes, opened.chimes, edited.chimes),
                mergeSchedule(redstone, opened.redstone, edited.redstone),
                opened.ticking == edited.ticking ? ticking : edited.ticking,
                opened.chimeEnabled == edited.chimeEnabled ? chimeEnabled : edited.chimeEnabled,
                opened.redstoneEnabled == edited.redstoneEnabled ? redstoneEnabled : edited.redstoneEnabled,
                opened.pulse == edited.pulse ? pulse : edited.pulse);
    }

    private static long mergeSchedule(long current, long opened, long edited) {
        long changed = opened ^ edited;
        return (current & ~changed) | (edited & changed);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Chimes", chimes);
        tag.putLong("Redstone", redstone);
        tag.putBoolean("Ticking", ticking);
        tag.putBoolean("ChimeEnabled", chimeEnabled);
        tag.putBoolean("RedstoneEnabled", redstoneEnabled);
        tag.putBoolean("Pulse", pulse);
        return tag;
    }

    public static ClockSettings load(CompoundTag tag) {
        return tag.isEmpty() ? DEFAULT : new ClockSettings(tag.getLong("Chimes"), tag.getLong("Redstone"),
                tag.getBoolean("Ticking"), tag.getBoolean("ChimeEnabled"), tag.getBoolean("RedstoneEnabled"),
                tag.getBoolean("Pulse"));
    }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeLong(chimes).writeLong(redstone).writeBoolean(ticking).writeBoolean(chimeEnabled)
                .writeBoolean(redstoneEnabled).writeBoolean(pulse);
    }

    public static ClockSettings read(FriendlyByteBuf buffer) {
        return new ClockSettings(buffer.readLong(), buffer.readLong(), buffer.readBoolean(), buffer.readBoolean(),
                buffer.readBoolean(), buffer.readBoolean());
    }
}
