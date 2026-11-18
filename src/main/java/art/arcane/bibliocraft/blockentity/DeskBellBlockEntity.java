package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.DeskBellBlock;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class DeskBellBlockEntity extends FurnitureBlockEntity {
    private int sampleTicks;
    private int lastPower;

    public DeskBellBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BELL.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos) {
        if (++sampleTicks < 3) {
            return;
        }
        sampleTicks = 0;
        int power = level.getBestNeighborSignal(pos);
        if (lastPower != power) {
            if (power > lastPower && BiblioConfig.COMMON.enableDeskBell.get()) {
                DeskBellBlock.ring(level, pos);
            }
            lastPower = power;
            setChanged();
        }
    }

    @Override
    public boolean toggleLock(Player player) {
        if (!super.toggleLock(player)) {
            return false;
        }
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
        return true;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        lastPower = Mth.clamp(tag.getInt("LastPower"), 0, 15);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("LastPower", lastPower);
    }
}
