package art.arcane.bibliocraft.entity;

import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlocks;
import art.arcane.bibliocraft.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.DismountHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public final class SeatEntity extends Entity {
    private static final String TAG_SEAT_POS = "SeatPos";
    private static final EntityDataAccessor<Optional<BlockPos>> SEAT_POS =
            SynchedEntityData.defineId(SeatEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final int MAX_EMPTY_TICKS = 20;
    private int emptyTicks;

    public SeatEntity(EntityType<? extends SeatEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setSilent(true);
    }

    public static @Nullable SeatEntity find(ServerLevel level, BlockPos pos) {
        for (SeatEntity seat : level.getEntitiesOfClass(SeatEntity.class, new AABB(pos).inflate(0.25))) {
            if (!seat.isRemoved() && pos.equals(seat.seatPos())) {
                return seat;
            }
        }
        return null;
    }

    public static boolean sit(ServerLevel level, BlockPos pos, LivingEntity passenger) {
        if (passenger.isPassenger() || !passenger.isAlive() || passenger.isSpectator()) {
            return false;
        }
        SeatEntity seat = find(level, pos);
        if (seat != null) {
            return !seat.isVehicle() && passenger.startRiding(seat, true);
        }
        seat = ModEntities.SEAT_ENTITY.get().create(level);
        if (seat == null) {
            return false;
        }
        seat.setSeatPos(pos);
        if (!level.addFreshEntity(seat)) {
            return false;
        }
        if (!passenger.startRiding(seat, true)) {
            seat.discard();
            return false;
        }
        return true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(SEAT_POS, Optional.empty());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains(TAG_SEAT_POS)) {
            setSeatPos(BlockPos.of(tag.getLong(TAG_SEAT_POS)));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        var pos = seatPos();
        if (pos != null) {
            tag.putLong(TAG_SEAT_POS, pos.asLong());
        }
    }

    @Override
    public void tick() {
        super.tick();
        BlockPos pos = seatPos();
        if (level() instanceof ServerLevel server) {
            if (pos == null || !server.getBlockState(pos).is(ModBlocks.SEAT.get()) || !BiblioConfig.COMMON.enableSeat.get()) {
                if (pos != null) {
                    SeatBlock.setOccupied(server, pos, false);
                }
                discard();
                return;
            }
            SeatBlock.setOccupied(server, pos, isVehicle());
            if (isVehicle()) {
                emptyTicks = 0;
            } else if (++emptyTicks > MAX_EMPTY_TICKS) {
                discard();
                return;
            }
        }
        if (pos != null) {
            setPos(pos.getX() + 0.5, pos.getY() + 0.52, pos.getZ() + 0.5);
        }
    }

    @Override
    protected void addPassenger(Entity passenger) {
        super.addPassenger(passenger);
        syncOccupancy();
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        syncOccupancy();
    }

    private void syncOccupancy() {
        BlockPos pos = seatPos();
        if (level() instanceof ServerLevel server && pos != null) {
            SeatBlock.setOccupied(server, pos, isVehicle());
        }
    }

    @Override
    public double getPassengersRidingOffset() {
        return getFirstPassenger() instanceof Player ? -0.10 : 0.0;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        BlockPos pos = seatPos();
        if (pos != null) {
            var state = level().getBlockState(pos);
            Direction front = state.is(ModBlocks.SEAT.get()) ? SeatBlock.front(state) : Direction.NORTH;
            for (Direction side : new Direction[]{front, front.getClockWise(), front.getCounterClockWise(), front.getOpposite()}) {
                Vec3 dismount = DismountHelper.findSafeDismountLocation(passenger.getType(), level(), pos.relative(side), true);
                if (dismount != null) {
                    return dismount;
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
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

    public void setSeatPos(BlockPos pos) {
        entityData.set(SEAT_POS, Optional.of(pos.immutable()));
        setPos(pos.getX() + 0.5, pos.getY() + 0.52, pos.getZ() + 0.5);
    }

    public @Nullable BlockPos seatPos() {
        return entityData.get(SEAT_POS).orElse(null);
    }
}
