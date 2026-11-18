package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.WoolCarpetBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

public final class DeskBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int OPEN_BOOK = 0;
    public static final int LEFT_BOOKS = 1;
    public static final int RIGHT_BOOKS = 5;
    public static final int BOOKS_PER_STACK = 4;
    public static final int CARPET = 9;
    public static final ModelProperty<Integer> VISIBLE_PARTS = new ModelProperty<>();
    public static final ModelProperty<DyeColor> CARPET_COLOR = new ModelProperty<>();
    private static final int OPEN_BOOK_MESH = 8;
    private static final int CARPET_MESH = 9;
    private static final int[] STORAGE_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int[] TOP_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8};
    private int page;

    public DeskBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESK.get(), pos, state, 10);
    }

    public static boolean acceptsBook(ItemStack stack) {
        return stack.is(Items.FILLED_MAP) || BookcaseBlockEntity.acceptsBook(stack);
    }

    public static boolean isCarpet(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof WoolCarpetBlock;
    }

    public int firstEmptyInStack(int start) {
        for (int slot = start; slot < start + BOOKS_PER_STACK; slot++) {
            if (getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public int topOfStack(int start) {
        for (int slot = start + BOOKS_PER_STACK - 1; slot >= start; slot--) {
            if (!getItem(slot).isEmpty()) {
                return slot;
            }
        }
        return -1;
    }

    public boolean receiveWrittenBook(ItemStack book) {
        if (isLocked() || !BiblioConfig.COMMON.enableWritingdesk.get() || !book.is(Items.WRITTEN_BOOK)) {
            return false;
        }
        int slot = firstEmptyInStack(LEFT_BOOKS);
        if (slot < 0) {
            slot = firstEmptyInStack(RIGHT_BOOKS);
        }
        if (slot < 0) {
            return false;
        }
        setItem(slot, book.copy());
        return true;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == CARPET ? isCarpet(stack) : acceptsBook(stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? STORAGE_SLOTS : TOP_SLOTS;
    }

    @Override
    protected int getSlotLimit(int slot) {
        return slot == CARPET ? 1 : super.getSlotLimit(slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot == OPEN_BOOK && getItem(slot) != stack) {
            page = 0;
        }
        super.setItem(slot, stack);
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
        setChanged();
    }

    public void tickMaps(ServerLevel level) {
        ItemStack map = getItem(OPEN_BOOK);
        if (!map.is(Items.FILLED_MAP) || level.getGameTime() % BiblioConfig.COMMON.mapUpdateRate.get() != 0) {
            return;
        }
        var data = MapItem.getSavedData(map, level);
        Integer id = MapItem.getMapId(map);
        if (data == null || id == null) {
            return;
        }
        for (var player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(worldPosition), false)) {
            data.getHoldingPlayer(player);
            if (!data.locked) {
                ((MapItem) Items.FILLED_MAP).update(level, player, data);
            }
            var packet = data.getUpdatePacket(id, player);
            if (packet != null) {
                player.connection.send(packet);
            }
        }
    }

    public int visibleParts() {
        ItemStack book = getItem(OPEN_BOOK);
        int mask = !book.isEmpty() && !book.is(Items.FILLED_MAP) ? 1 << OPEN_BOOK_MESH : 0;
        if (!getItem(CARPET).isEmpty()) {
            mask |= 1 << CARPET_MESH;
        }
        int left = 0;
        int right = 0;
        for (int index = 0; index < BOOKS_PER_STACK; index++) {
            if (!getItem(LEFT_BOOKS + index).isEmpty()) {
                mask |= 1 << left++;
            }
            if (!getItem(RIGHT_BOOKS + index).isEmpty()) {
                mask |= 1 << (BOOKS_PER_STACK + right++);
            }
        }
        return mask;
    }

    private DyeColor carpetColor() {
        return getItem(CARPET).getItem() instanceof BlockItem item && item.getBlock() instanceof WoolCarpetBlock carpet
                ? carpet.getColor() : DyeColor.WHITE;
    }

    @Override
    public void load(CompoundTag tag) {
        int before = visibleParts();
        DyeColor previousColor = carpetColor();
        super.load(tag);
        page = Math.max(0, tag.getInt("Page"));
        if (before != visibleParts() || previousColor != carpetColor()) {
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("Page", page);
    }

    @Override
    public ModelData getModelData() {
        return super.getModelData().derive().with(VISIBLE_PARTS, visibleParts()).with(CARPET_COLOR, carpetColor()).build();
    }
}
