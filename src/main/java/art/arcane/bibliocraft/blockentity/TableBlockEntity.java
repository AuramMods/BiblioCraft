package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class TableBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int DISPLAY = 0;
    public static final int CLOTH = 1;
    public static final int CARPET = 2;
    public static final ModelProperty<DyeColor> CLOTH_COLOR = new ModelProperty<>();
    public static final ModelProperty<DyeColor> CARPET_COLOR = new ModelProperty<>();
    private int xAngle;
    private int yAngle;

    public TableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TABLE.get(), pos, state, 3);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == DISPLAY || carpetColor(stack) != null;
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == DISPLAY ? super.getSlotLimit(slot) : 1;
    }

    public void rotateDisplay(Direction face) {
        if (face.getAxis() == Direction.Axis.Z) {
            xAngle = (xAngle + 1) & 3;
        } else {
            yAngle = (yAngle + 1) & 3;
        }
        setChanged();
    }

    public int xAngle() {
        return xAngle;
    }

    public int yAngle() {
        return yAngle;
    }

    public void tickMap(ServerLevel level) {
        DisplayedMap.tick(level, worldPosition, getItem(DISPLAY));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server) {
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).expandTowards(0, 1, 0);
    }

    @Override
    public void load(CompoundTag tag) {
        DyeColor previousCloth = carpetColor(getItem(CLOTH));
        DyeColor previousCarpet = carpetColor(getItem(CARPET));
        super.load(tag);
        xAngle = tag.getInt("slotxangle") & 3;
        yAngle = tag.getInt("slotyangle") & 3;
        if (previousCloth != carpetColor(getItem(CLOTH)) || previousCarpet != carpetColor(getItem(CARPET))) {
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("slotxangle", xAngle);
        tag.putInt("slotyangle", yAngle);
    }

    @Override
    public ModelData getModelData() {
        var data = super.getModelData().derive();
        DyeColor cloth = carpetColor(getItem(CLOTH));
        DyeColor carpet = carpetColor(getItem(CARPET));
        if (cloth != null) {
            data.with(CLOTH_COLOR, cloth);
        }
        if (carpet != null) {
            data.with(CARPET_COLOR, carpet);
        }
        return data.build();
    }
}
