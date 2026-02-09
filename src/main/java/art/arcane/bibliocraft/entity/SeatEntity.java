package art.arcane.bibliocraft.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public class SeatEntity extends Entity {
    public SeatEntity(EntityType<? extends SeatEntity> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        // Placeholder seat entity has no synced data yet.
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // Placeholder seat entity has no persisted fields yet.
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        // Placeholder seat entity has no persisted fields yet.
    }
}
