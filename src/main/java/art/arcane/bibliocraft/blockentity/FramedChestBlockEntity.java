package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.FramedChestBlock;
import art.arcane.bibliocraft.block.FramedChestPart;
import art.arcane.bibliocraft.menu.FramedChestMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChestLidController;
import net.minecraft.world.level.block.entity.ContainerOpenersCounter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class FramedChestBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int SLOTS = 27;
    public static final int OPEN_EVENT = 1;
    private final ChestLidController lid = new ChestLidController();
    private ItemStack label = ItemStack.EMPTY;
    private final ContainerOpenersCounter openers = new ContainerOpenersCounter() {
        @Override
        protected void onOpen(Level level, BlockPos pos, BlockState state) {
            playChestSound(level, pos, state, SoundEvents.CHEST_OPEN);
        }

        @Override
        protected void onClose(Level level, BlockPos pos, BlockState state) {
            playChestSound(level, pos, state, SoundEvents.CHEST_CLOSE);
        }

        @Override
        protected void openerCountChanged(Level level, BlockPos pos, BlockState state, int previous, int current) {
            if ((previous == 0) != (current == 0)) {
                level.blockEvent(pos, state.getBlock(), OPEN_EVENT, current);
            }
        }

        @Override
        protected boolean isOwnContainer(Player player) {
            return player.containerMenu instanceof FramedChestMenu menu
                    && menu.containsChest(FramedChestBlockEntity.this) && menu.stillValid(player);
        }
    };

    public FramedChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FRAMED_CHEST.get(), pos, state, SLOTS);
    }

    public ItemStack getLabel() {
        return label;
    }

    public int getOpenerCount() {
        return openers.getOpenerCount();
    }

    @Override
    public void startOpen(Player player) {
        if (!isRemoved() && level != null && !level.isClientSide && !player.isSpectator()) {
            openers.incrementOpeners(player, level, worldPosition, getBlockState());
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!isRemoved() && level != null && !level.isClientSide && !player.isSpectator() && getOpenerCount() > 0) {
            openers.decrementOpeners(player, level, worldPosition, getBlockState());
        }
    }

    public void recheckOpeners() {
        if (!isRemoved() && level != null) {
            openers.recheckOpeners(level, worldPosition, getBlockState());
        }
    }

    public void tickLid() {
        lid.tickLid();
    }

    public float openness(float partialTick) {
        if (level != null && getBlockState().getValue(FramedChestBlock.PART) == FramedChestPart.RIGHT) {
            FramedChestBlockEntity other = FramedChestBlock.partner(level, worldPosition, getBlockState());
            if (other != null) {
                return other.lid.getOpenness(partialTick);
            }
        }
        return lid.getOpenness(partialTick);
    }

    @Override
    public boolean triggerEvent(int event, int value) {
        if (event == OPEN_EVENT) {
            lid.shouldBeOpen(value > 0);
            return true;
        }
        return super.triggerEvent(event, value);
    }

    private static void playChestSound(Level level, BlockPos pos, BlockState state, SoundEvent sound) {
        FramedChestBlockEntity other = FramedChestBlock.partner(level, pos, state);
        if (other != null && state.getValue(FramedChestBlock.PART) == FramedChestPart.RIGHT) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        if (other != null) {
            center = center.add(Vec3.atCenterOf(other.getBlockPos())).scale(0.5);
        }
        level.playSound(null, center.x, center.y, center.z, sound, SoundSource.BLOCKS,
                0.5F, 0.9F + 0.1F * level.random.nextFloat());
    }

    @Override
    public void setChanged() {
        label = findLabel();
        super.setChanged();
    }

    private ItemStack findLabel() {
        List<ItemStack> totals = new ArrayList<>();
        for (int slot = 0; slot < getContainerSize(); slot++) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack match = ItemStack.EMPTY;
            for (ItemStack group : totals) {
                if (ItemStack.isSameItemSameTags(group, stack)) {
                    match = group;
                    break;
                }
            }
            if (match.isEmpty()) {
                totals.add(stack.copy());
            } else {
                match.grow(stack.getCount());
            }
        }
        ItemStack largest = ItemStack.EMPTY;
        for (ItemStack group : totals) {
            if (group.getCount() > largest.getCount()) {
                largest = group;
            }
        }
        return largest.copyWithCount(1);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveFurnitureData(tag);
        tag.put("DisplayItem", label.save(new CompoundTag()));
        tag.putBoolean("Open", getOpenerCount() > 0);
        return tag;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        label = tag.contains("DisplayItem") ? ItemStack.of(tag.getCompound("DisplayItem")) : findLabel();
        lid.shouldBeOpen(tag.getBoolean("Open"));
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(0.09).expandTowards(0, 0.55, 0);
    }
}
