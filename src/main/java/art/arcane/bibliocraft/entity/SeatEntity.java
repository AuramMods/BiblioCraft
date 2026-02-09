package art.arcane.bibliocraft.entity;

import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SeatEntity extends Entity {
    private static final String TAG_SEAT_POS = "SeatPos";
    private static final int MAX_EMPTY_TICKS = 20;

    private BlockPos seatPos = BlockPos.ZERO;
    private int emptyTicks = 0;

    public SeatEntity(EntityType<? extends SeatEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        // Placeholder seat entity has no synced data yet.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_SEAT_POS)) {
            this.seatPos = BlockPos.of(tag.getLong(TAG_SEAT_POS));
            refreshSeatPosition();
        }
        this.emptyTicks = 0;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (!seatPos.equals(BlockPos.ZERO)) {
            tag.putLong(TAG_SEAT_POS, seatPos.asLong());
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        refreshSeatPosition();

        if (level().isClientSide()) {
            return;
        }

        if (!isSeatStillValid()) {
            discard();
            return;
        }

        if (getPassengers().isEmpty()) {
            emptyTicks++;
            if (emptyTicks > MAX_EMPTY_TICKS) {
                discard();
            }
            return;
        }

        emptyTicks = 0;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.10D;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    public void setSeatPos(BlockPos seatPos) {
        this.seatPos = seatPos.immutable();
        this.emptyTicks = 0;
        refreshSeatPosition();
    }

    public boolean isForSeat(BlockPos seatPos) {
        return this.seatPos.equals(seatPos);
    }

    private boolean isSeatStillValid() {
        return !seatPos.equals(BlockPos.ZERO) && level().getBlockState(seatPos).is(ModBlocks.SEAT.get());
    }

    private void refreshSeatPosition() {
        if (!seatPos.equals(BlockPos.ZERO)) {
            setPos(seatPos.getX() + 0.5D, seatPos.getY() + 0.35D, seatPos.getZ() + 0.5D);
        }
    }
}
