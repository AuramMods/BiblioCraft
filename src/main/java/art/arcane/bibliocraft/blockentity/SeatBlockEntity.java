package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.FurnitureMaterial;
import art.arcane.bibliocraft.block.SeatBlock;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.entity.SeatEntity;
import art.arcane.bibliocraft.item.SeatBackItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SeatBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int CUSHION = 0;
    public static final int BACK = 1;
    public static final int CARPET = 2;
    public static final ModelProperty<Appearance> APPEARANCE = new ModelProperty<>();
    private static final int MOB_SCAN_INTERVAL = 100;
    private static final int[] NO_AUTOMATION = {};
    private long nextMobScan;
    private boolean reconcileOccupancy = true;

    public SeatBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SEAT.get(), pos, state, 3);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return NO_AUTOMATION;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? LazyOptional.empty() : super.getCapability(capability, side);
    }

    public static int attachmentSlot(ItemStack stack, Direction face) {
        if (stack.getItem() instanceof SeatBackItem) {
            return BACK;
        }
        if (carpetColor(stack) != null) {
            return face.getAxis().isVertical() ? CUSHION : CARPET;
        }
        return -1;
    }

    public int removableAttachment(Direction face) {
        if (face == Direction.UP && !getItem(CUSHION).isEmpty()) {
            return CUSHION;
        }
        if (!getItem(BACK).isEmpty()) {
            return BACK;
        }
        return face != Direction.UP && !getItem(CARPET).isEmpty() ? CARPET : -1;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            reconcileOccupancy = true;
            nextMobScan = server.getGameTime() + 1 + Math.floorMod(worldPosition.asLong(), MOB_SCAN_INTERVAL);
        }
    }

    public void delayMobScan(ServerLevel level) {
        nextMobScan = level.getGameTime() + MOB_SCAN_INTERVAL;
    }

    public void tick(ServerLevel level) {
        if (reconcileOccupancy) {
            reconcileOccupancy = false;
            SeatEntity mount = SeatEntity.find(level, worldPosition);
            SeatBlock.setOccupied(level, worldPosition, mount != null && mount.isVehicle());
        }
        if (!BiblioConfig.COMMON.enableSeat.get() || isLocked() || getBlockState().getValue(SeatBlock.OCCUPIED)
                || level.getGameTime() < nextMobScan) {
            return;
        }
        delayMobScan(level);
        for (PathfinderMob mob : level.getEntitiesOfClass(PathfinderMob.class, new AABB(worldPosition).inflate(2, 1, 2),
                candidate -> candidate.isAlive() && candidate.hasCustomName() && !candidate.isPassenger())) {
            if (SeatEntity.sit(level, worldPosition, mob)) {
                return;
            }
        }
    }

    public Appearance appearance() {
        ItemStack back = getItem(BACK);
        int style = back.getItem() instanceof SeatBackItem item ? item.style() : 0;
        FurnitureMaterial material = style == 0 ? getMaterial() : FurnitureMaterial.fromItem(back);
        if (style != 0 && getMaterial().wood() != FurnitureMaterial.Wood.FRAMED && material.panel() != null) {
            material = new FurnitureMaterial(material.wood(), null);
        }
        DyeColor cushion = carpetColor(getItem(CUSHION));
        return new Appearance(style, material, cushion == null ? DyeColor.WHITE : cushion, carpetColor(getItem(CARPET)));
    }

    @Override
    public ModelData getModelData() {
        return super.getModelData().derive().with(APPEARANCE, appearance()).build();
    }

    @Override
    public void load(CompoundTag tag) {
        Appearance previous = appearance();
        super.load(tag);
        if (!appearance().equals(previous)) {
            refreshModel();
        }
    }

    public record Appearance(int back, FurnitureMaterial backMaterial, DyeColor cushion, @Nullable DyeColor carpet) {}
}
