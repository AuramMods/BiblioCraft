package art.arcane.bibliocraft.blockentity;

import art.arcane.bibliocraft.block.PaintingFrameBlock;
import art.arcane.bibliocraft.painting.PaintingArt;
import art.arcane.bibliocraft.painting.PaintingSettings;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class PaintingFrameBlockEntity extends FurnitureInventoryBlockEntity {
    public static final int CANVAS = 0;
    private int links;
    private PaintingSettings settings = PaintingSettings.DEFAULT;

    public PaintingFrameBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    public static boolean accepts(ItemStack stack) {
        return stack.is(ModItems.PAINTING_CANVAS.get());
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot == CANVAS && accepts(stack);
    }

    @Override
    protected int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!stack.isEmpty() && !ItemStack.isSameItemSameTags(getItem(slot), stack)) {
            PaintingArt art = PaintingArt.from(stack);
            settings = new PaintingSettings(0, 1, 0, 0, 0, art.width(), art.height(), settings.hidden());
        }
        super.setItem(slot, stack);
    }

    public PaintingArt art() {
        return PaintingArt.from(getItem(CANVAS));
    }

    public boolean borderless() {
        return getBlockState().getBlock() instanceof PaintingFrameBlock frame
                && frame.style() == PaintingFrameBlock.Style.BORDERLESS;
    }

    public PaintingSettings settings() {
        return settings;
    }

    public void edit(PaintingSettings next) {
        PaintingSettings validated = next.forArt(art(), borderless());
        if (!settings.equals(validated)) {
            settings = validated;
            setChanged();
        }
    }

    public int links() {
        return links;
    }

    private void setLink(int side, boolean connected) {
        int bit = 1 << side;
        int next = connected ? links | bit : links & ~bit;
        if (next != links) {
            links = next;
            setChanged();
        }
    }

    public boolean toggleLink(int side, Player player) {
        if (level == null || !canAccess(player)) {
            return false;
        }
        var other = neighbor(level, worldPosition.relative(PaintingFrameBlock.side(getBlockState(), side)));
        if (other == null || !other.canAccess(player)) {
            return false;
        }
        int opposite = (side + 2) & 3;
        boolean connect = (links & 1 << side) == 0 || (other.links & 1 << opposite) == 0;
        setLink(side, connect);
        other.setLink(opposite, connect);
        return true;
    }

    public void connectBorderless(@Nullable Player player) {
        if (level == null || !borderless()) {
            return;
        }
        for (int side = 0; side < 4; side++) {
            var other = neighbor(level, worldPosition.relative(PaintingFrameBlock.side(getBlockState(), side)));
            if (other != null && (player == null ? !other.isLocked() : other.canAccess(player))) {
                setLink(side, true);
                other.setLink((side + 2) & 3, true);
            }
        }
    }

    public void detach() {
        if (level == null) {
            return;
        }
        for (int side = 0; side < 4; side++) {
            var other = neighbor(level, worldPosition.relative(PaintingFrameBlock.side(getBlockState(), side)));
            if (other != null) {
                other.setLink((side + 2) & 3, false);
            }
        }
    }

    public int activeLinks(BlockGetter level) {
        int result = 0;
        for (int side = 0; side < 4; side++) {
            if (linkedFrame(level, side) != null) {
                result |= 1 << side;
            }
        }
        return result;
    }

    public @Nullable PaintingFrameBlockEntity linkedFrame(BlockGetter level, int side) {
        if ((links & 1 << side) == 0) {
            return null;
        }
        var other = neighbor(level, worldPosition.relative(PaintingFrameBlock.side(getBlockState(), side)));
        return other != null && (other.links & 1 << ((side + 2) & 3)) != 0 ? other : null;
    }

    private @Nullable PaintingFrameBlockEntity neighbor(BlockGetter world, BlockPos pos) {
        if (world instanceof net.minecraft.world.level.Level live && !live.hasChunkAt(pos)) {
            return null;
        }
        if (world.getBlockEntity(pos) instanceof PaintingFrameBlockEntity other
                && other.getBlockState().getValue(PaintingFrameBlock.FACING)
                == getBlockState().getValue(PaintingFrameBlock.FACING)) {
            return other;
        }
        return null;
    }

    public boolean insetArtwork(BlockGetter view) {
        return !borderless() && settings.scale() == 1 && settings.aspect() == 0 && activeLinks(view) == 0
                && art().source() != PaintingArt.Source.VANILLA
                && settings.customWidth() == 1 && settings.customHeight() == 1;
    }

    @Override
    public AABB getRenderBoundingBox() {
        PaintingArt art = art();
        if (art.isBlank()) {
            return new AABB(worldPosition);
        }
        var bounds = settings.footprint(art, 0);
        Direction front = getBlockState().getValue(PaintingFrameBlock.FACING).getClockWise();
        Direction left = front.getClockWise();
        Vec3 anchor = Vec3.atBottomCenterOf(worldPosition).relative(front, -0.46).relative(left, 0.5);
        return new AABB(anchor.relative(left, -bounds.left()).add(0, bounds.bottom(), 0),
                anchor.relative(left, -bounds.right()).add(0, bounds.top(), 0)).inflate(0.05);
    }

    @Override
    public void load(CompoundTag tag) {
        int oldLinks = links;
        boolean hidden = settings.hidden();
        super.load(tag);
        settings = PaintingSettings.read(tag).forArt(art(), borderless());
        links = tag.getInt("FrameLinks") & 15;
        if (links != oldLinks || settings.hidden() != hidden) {
            refreshModel();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        settings.save(tag);
        tag.putInt("FrameLinks", links);
    }
}
