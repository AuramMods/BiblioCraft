package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.book.PrintingLibrary;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.PrintingPlateItem;
import art.arcane.bibliocraft.map.AtlasContents;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PrintingPressBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int INK = 0;
    public static final int PLATE = 1;
    public static final int BLANK = 2;
    public static final int RESULT = 3;
    public static final int PRINT_TICKS = 200;
    public static final ModelProperty<Integer> VISIBLE_PARTS = new ModelProperty<>();
    public static final ModelProperty<Integer> INK_LEVEL = new ModelProperty<>();
    private static final int[] INSERT_SLOTS = {INK, PLATE, BLANK};
    private static final int[] EXTRACT_SLOTS = {RESULT};
    private LazyOptional<IItemHandler> intake = sidedHandler(Direction.UP);
    private LazyOptional<IItemHandler> outlet = sidedHandler(Direction.DOWN);
    private ItemStack jobPlate = ItemStack.EMPTY;
    private ItemStack jobAtlas = ItemStack.EMPTY;
    private int ticksLeft;
    private int visibleParts;
    private int inkLevel;
    private long animationStart = -1;

    public PrintingPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PRINTING_PRESS.get(), pos, state, 4);
    }

    public static boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case INK -> stack.is(Tags.Items.DYES_BLACK) || stack.is(Items.INK_SAC);
            case PLATE -> stack.is(ModItems.PRINT_PLATE.get()) || stack.is(ModItems.ENCHANTED_PLATE.get())
                    || BiblioConfig.COMMON.enableAtlas.get() && stack.is(ModItems.ATLAS_PLATE.get());
            case BLANK -> stack.is(Items.BOOK) || BiblioConfig.COMMON.enableAtlas.get()
                    && stack.is(ModItems.ATLAS_BOOK.get()) && new AtlasContents(stack).isEmpty();
            default -> false;
        };
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return accepts(slot, stack);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == INK || slot == BLANK ? 64 : 1;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? EXTRACT_SLOTS : INSERT_SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return side != Direction.DOWN && super.canPlaceItemThroughFace(slot, stack, side);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return side == Direction.DOWN && slot == RESULT && super.canTakeItemThroughFace(slot, stack, side);
    }

    private LazyOptional<IItemHandler> sidedHandler(Direction side) {
        return LazyOptional.of(() -> new SidedInvWrapper(this, side) {
            @Override
            public int getSlotLimit(int slot) {
                int realSlot = SidedInvWrapper.getSlot(PrintingPressBlockEntity.this, slot, side);
                return realSlot < 0 ? 0 : PrintingPressBlockEntity.this.getSlotLimit(realSlot);
            }

            @Override
            public void setStackInSlot(int slot, @NotNull ItemStack stack) {
                int realSlot = SidedInvWrapper.getSlot(PrintingPressBlockEntity.this, slot, side);
                if (realSlot >= 0 && !isLocked() && (stack.isEmpty() || side != Direction.DOWN
                        && canPlaceItem(realSlot, stack))) {
                    super.setStackInSlot(slot, stack);
                }
            }
        });
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && !isRemoved() && side != null) {
            return (side == Direction.DOWN ? outlet : intake).cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        intake.invalidate();
        outlet.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        intake = sidedHandler(Direction.UP);
        outlet = sidedHandler(Direction.DOWN);
    }

    public void tick(ServerLevel server) {
        if (!BiblioConfig.COMMON.enablePrintpressTypeMachine.get()) {
            return;
        }
        ItemStack plate = getItem(PLATE);
        ItemStack blank = getItem(BLANK);
        boolean atlasJob = plate.is(ModItems.ATLAS_PLATE.get());
        if (ticksLeft == 0) {
            if (hasInputs(atlasJob, blank) && accepts(INK, getItem(INK)) && !printableBook(server, plate).isEmpty()) {
                jobPlate = plate.copy();
                jobAtlas = atlasJob ? blank.copyWithCount(1) : ItemStack.EMPTY;
                ticksLeft = PRINT_TICKS;
                removeItem(INK, 1);
            }
            return;
        }
        if (!hasInputs(atlasJob, blank) || !ItemStack.isSameItemSameTags(plate, jobPlate)
                || atlasJob && !ItemStack.isSameItemSameTags(blank, jobAtlas)) {
            resetJob();
            return;
        }
        if (--ticksLeft != 0) {
            // Persist the press position without sending a block-entity packet every tick.
            server.blockEntityChanged(worldPosition);
            return;
        }
        ItemStack printed = printableBook(server, plate);
        if (!printed.isEmpty()) {
            removeItem(BLANK, 1);
            if (atlasJob) {
                setItem(PLATE, ItemStack.EMPTY);
            } else if (plate.is(ModItems.ENCHANTED_PLATE.get())) {
                if (PrintingPlateItem.remainingUses(plate) <= 1) {
                    setItem(PLATE, ItemStack.EMPTY);
                } else {
                    var used = plate.copy();
                    var tag = used.getOrCreateTag();
                    tag.putInt("Uses", tag.getInt("Uses") + 1);
                    setItem(PLATE, used);
                }
            }
            setItem(RESULT, printed.copyWithCount(1));
        }
        resetJob();
    }

    private boolean hasInputs(boolean atlasJob, ItemStack blank) {
        return getItem(RESULT).isEmpty() && (atlasJob ? blank.is(ModItems.ATLAS_BOOK.get()) && accepts(BLANK, blank)
                : blank.is(Items.BOOK));
    }

    private void resetJob() {
        ticksLeft = 0;
        jobPlate = ItemStack.EMPTY;
        jobAtlas = ItemStack.EMPTY;
        setChanged();
    }

    private static ItemStack printableBook(ServerLevel server, ItemStack plate) {
        var id = PrintingPlateItem.bookId(plate);
        if (id != null) {
            var entry = PrintingLibrary.get(server).find(id);
            return entry == null ? ItemStack.EMPTY : entry.book();
        }
        if (plate.is(ModItems.ATLAS_PLATE.get())) {
            return PrintingPlateItem.atlasCopy(plate);
        }
        return PrintingPlateItem.enchantedCopy(plate);
    }

    public int ticksLeft() {
        return ticksLeft;
    }

    public float animationTime(float partialTick) {
        return level == null || animationStart < 0 ? 0
                : Math.max(0, level.getGameTime() - animationStart + partialTick);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.builder().with(VISIBLE_PARTS, visibleParts).with(INK_LEVEL, inkLevel).build();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        jobPlate = ItemStack.of(tag.getCompound("JobPlate"));
        jobAtlas = ItemStack.of(tag.getCompound("JobAtlas"));
        ticksLeft = jobPlate.isEmpty() ? 0 : Mth.clamp(tag.getInt("TicksLeft"), 0, PRINT_TICKS);
        animationStart = tag.contains("AnimationStart") ? tag.getLong("AnimationStart") : -1;
        int parts = tag.getInt("VisibleParts");
        int ink = Mth.clamp(tag.getInt("InkLevel"), 0, 8);
        if (parts != visibleParts || ink != inkLevel) {
            visibleParts = parts;
            inkLevel = ink;
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (ticksLeft > 0) {
            tag.putInt("TicksLeft", ticksLeft);
            tag.put("JobPlate", jobPlate.save(new CompoundTag()));
            if (!jobAtlas.isEmpty()) {
                tag.put("JobAtlas", jobAtlas.save(new CompoundTag()));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveFurnitureData(tag);
        int mask = getItem(PLATE).isEmpty() ? 0 : 1;
        int bookLayers = (getItem(BLANK).getCount() + 15) / 16;
        mask |= ((1 << bookLayers) - 1) << 1;
        ItemStack finished = getItem(RESULT);
        if (!finished.isEmpty()) {
            mask |= 1 << (finished.is(Items.ENCHANTED_BOOK) ? 6 : 5);
        }
        tag.putInt("VisibleParts", mask);
        int count = getItem(INK).getCount();
        tag.putInt("InkLevel", count == 0 ? 0 : Math.min(8, 1 + count / 8));
        if (ticksLeft > 0 && level != null) {
            tag.putLong("AnimationStart", level.getGameTime() - (PRINT_TICKS - ticksLeft));
        }
        return tag;
    }
}
