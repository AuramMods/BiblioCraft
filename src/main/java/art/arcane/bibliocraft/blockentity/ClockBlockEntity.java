package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.ClockBlock;
import art.arcane.bibliocraft.block.ClockPart;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class ClockBlockEntity extends FurnitureBlockEntity {
    public static final int PULSE_TICKS = 40;
    private ClockSettings settings = ClockSettings.DEFAULT;
    private long lastChimePeriod = Long.MIN_VALUE;
    private long lastPulsePeriod = Long.MIN_VALUE;
    private long lastTickSoundTime = Long.MIN_VALUE;
    private long pulseUntil;
    private boolean powered;

    public ClockBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CLOCK.get(), pos, state);
    }

    public ClockSettings getSettings() {
        return settings;
    }

    public void setSettings(ClockSettings settings) {
        if (!this.settings.equals(settings)) {
            this.settings = settings;
            syncState();
        }
    }

    private void syncState() {
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public boolean isPowered() {
        return powered;
    }

    public void wind() {
        play(ModSounds.WIND.get(), 0.6F);
    }

    public ItemStack asItem(ItemStack stack) {
        if (!settings.equals(ClockSettings.DEFAULT)) {
            stack.getOrCreateTagElement("BlockEntityTag").put(ClockSettings.TAG, settings.save());
        }
        return getMaterial().applyTo(stack);
    }

    public void stopOwnPulse() {
        if (powered || pulseUntil != 0) {
            powered = false;
            pulseUntil = 0;
            setChanged();
        }
    }

    public void tick() {
        BlockState state = getBlockState();
        ClockPart part = state.getValue(ClockBlock.PART);
        if (part != ClockPart.WALL && ClockBlock.partner(level, worldPosition, state) == null) {
            state = state.setValue(ClockBlock.PART, ClockPart.WALL);
            level.setBlock(worldPosition, state, Block.UPDATE_ALL);
        } else if (part == ClockPart.LOWER) {
            stopOwnPulse();
            return;
        }
        boolean enabled = BiblioConfig.COMMON.enableClock.get();
        long time = level.getDayTime();
        long period = Math.floorDiv(time, ClockSettings.SLOT_TICKS);
        int slot = ClockSettings.slot(time);
        if (enabled) {
            tickSounds(time, period, slot);
        }
        boolean scheduled = enabled && settings.redstoneEnabled() && ClockSettings.includes(settings.redstone(), slot);
        if (scheduled && settings.pulse()) {
            if (lastPulsePeriod != period) {
                lastPulsePeriod = period;
                pulseUntil = level.getGameTime() + PULSE_TICKS;
                setChanged();
            }
        } else if (pulseUntil != 0) {
            pulseUntil = 0;
            setChanged();
        }
        boolean output = scheduled && (!settings.pulse() || level.getGameTime() < pulseUntil);
        if (powered != output) {
            powered = output;
            syncState();
            ClockBlock.updateClockPower(level, worldPosition, state);
        }
    }

    private void tickSounds(long time, long period, int slot) {
        if (settings.ticking() && time != lastTickSoundTime) {
            switch (Math.floorMod(time, 40)) {
                case 17 -> play(ModSounds.TICK.get(), 0.4F);
                case 37 -> play(ModSounds.TOCK.get(), 0.4F);
                default -> { }
            }
            lastTickSoundTime = time;
        }
        if (settings.chimeEnabled() && period != lastChimePeriod && ClockSettings.includes(settings.chimes(), slot)) {
            lastChimePeriod = period;
            setChanged();
            play(ModSounds.WOUNDCHIME.get(), 0.4F);
        }
    }

    private void play(SoundEvent sound, float volume) {
        if (level != null && !level.isClientSide) {
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, volume, 1);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        BlockPos floor = getBlockState().getValue(ClockBlock.PART) == ClockPart.UPPER ? worldPosition.below() : worldPosition;
        return new AABB(floor, worldPosition.offset(1, 1, 1)).inflate(0.02);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        settings = ClockSettings.load(tag.getCompound(ClockSettings.TAG));
        lastChimePeriod = tag.contains("LastChimePeriod") ? tag.getLong("LastChimePeriod") : Long.MIN_VALUE;
        lastPulsePeriod = tag.contains("LastPulsePeriod") ? tag.getLong("LastPulsePeriod") : Long.MIN_VALUE;
        pulseUntil = tag.getLong("PulseUntil");
        powered = tag.getBoolean("Powered");
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (!settings.equals(ClockSettings.DEFAULT)) {
            tag.put(ClockSettings.TAG, settings.save());
        }
        if (lastChimePeriod != Long.MIN_VALUE) {
            tag.putLong("LastChimePeriod", lastChimePeriod);
        }
        if (lastPulsePeriod != Long.MIN_VALUE) {
            tag.putLong("LastPulsePeriod", lastPulsePeriod);
        }
        if (pulseUntil != 0) {
            tag.putLong("PulseUntil", pulseUntil);
        }
        if (powered) {
            tag.putBoolean("Powered", true);
        }
    }
}
