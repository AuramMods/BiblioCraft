package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class CookieJarBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SLOT_COUNT = 8;
    public static final ModelProperty<Integer> VISIBLE_COOKIES = new ModelProperty<>();
    private long pulseEnd;

    public CookieJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOKIE_JAR.get(), pos, state, SLOT_COUNT);
    }

    public long getPulseEnd() {
        return pulseEnd;
    }

    public void setPulseEnd(long tick) {
        pulseEnd = tick;
        setChanged();
    }

    private int occupiedSlots() {
        int count = 0;
        for (int slot = 0; slot < getContainerSize(); slot++) {
            if (!getItem(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void load(CompoundTag tag) {
        int previous = occupiedSlots();
        super.load(tag);
        pulseEnd = tag.getLong("PulseEnd");
        if (occupiedSlots() != previous) {
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (pulseEnd > 0) {
            tag.putLong("PulseEnd", pulseEnd);
        }
    }

    @Override
    public ModelData getModelData() {
        // The legacy jar fills bottom-first, regardless of which inventory slots are in use.
        return super.getModelData().derive().with(VISIBLE_COOKIES, (1 << occupiedSlots()) - 1).build();
    }
}
