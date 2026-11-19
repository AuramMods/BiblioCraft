package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import java.util.List;

public final class PaintingPressBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int CANVAS = 0;
    public static final int TRANSFER_TICK = 11;
    public static final int CYCLE_TICKS = 22;
    public static final ModelProperty<Integer> VISIBLE_PARTS = new ModelProperty<>();
    private PaintingArt selection = PaintingArt.BLANK;
    private PaintingArt transfer = PaintingArt.BLANK;
    private int progress;
    private long animationStart = -1;

    public PaintingPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PAINTING_PRESS.get(), pos, state, 1);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == CANVAS && stack.is(ModItems.PAINTING_CANVAS.get());
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (!getItem(slot).isEmpty() && count > 0) {
            progress = 0;
        }
        return super.removeItem(slot, count);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        progress = 0;
        super.setItem(slot, stack);
    }

    public PaintingArt selection() {
        return selection;
    }

    public void select(PaintingArt art) {
        if (!art.equals(selection)) {
            selection = art;
            setChanged();
        }
    }

    public boolean startTransfer() {
        if (progress != 0 || getItem(CANVAS).isEmpty() || !BiblioConfig.COMMON.enablePainting.get()) {
            return false;
        }
        transfer = selection;
        progress = 1;
        setChanged();
        return true;
    }

    public void tick(ServerLevel server) {
        if (progress == 0 || !BiblioConfig.COMMON.enablePainting.get()) {
            return;
        }
        if (getItem(CANVAS).isEmpty() || ++progress > CYCLE_TICKS) {
            progress = 0;
            setChanged();
        } else if (progress == TRANSFER_TICK) {
            ItemStack painted = getItem(CANVAS).copy();
            if (transfer.isBlank()) {
                for (String key : List.of("paintingType", "paintingTitle", "ArtworkWidth", "ArtworkHeight", "blank")) {
                    painted.removeTagKey(key);
                }
            } else {
                transfer.save(painted.getOrCreateTag());
            }
            super.setItem(CANVAS, painted);
        } else {
            server.blockEntityChanged(worldPosition);
        }
    }

    public int progress() {
        return progress;
    }

    public float lidAngle(float partialTick) {
        if (level == null || animationStart < 0) {
            return 0;
        }
        float elapsed = Mth.clamp(level.getGameTime() - animationStart + partialTick, 0, CYCLE_TICKS);
        return Mth.clamp(TRANSFER_TICK - Math.abs(TRANSFER_TICK - elapsed), 0, 10) * -2.5f;
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(VISIBLE_PARTS, getItem(CANVAS).isEmpty() ? 0 : 1).build();
    }

    @Override
    public void load(CompoundTag tag) {
        boolean hadCanvas = !getItem(CANVAS).isEmpty();
        super.load(tag);
        selection = PaintingArt.from(tag);
        transfer = PaintingArt.from(tag.getCompound("Transfer"));
        progress = Mth.clamp(tag.getInt("CycleProgress"), 0, CYCLE_TICKS);
        animationStart = tag.contains("AnimationStart") ? tag.getLong("AnimationStart") : -1;
        if (hadCanvas != !getItem(CANVAS).isEmpty()) {
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        selection.save(tag);
        if (progress > 0) {
            var artwork = new CompoundTag();
            transfer.save(artwork);
            tag.put("Transfer", artwork);
            tag.putInt("CycleProgress", progress);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        if (level != null && progress > 0) {
            tag.putLong("AnimationStart", level.getGameTime() - (progress - 1));
        }
        return tag;
    }
}
