package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.BookcaseMenu;
import art.arcane.bibliocraft.storygen.StoryBook;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class BookcaseBlock extends ShiftableFurnitureBlock {
    private final boolean stockedOnPlacement;

    public BookcaseBlock(Properties properties, VoxelShape shape, boolean stockedOnPlacement) {
        super(properties, shape, "bookcase", 0.25);
        this.stockedOnPlacement = stockedOnPlacement;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The base OBJ opens east. Preserve the orientation of saved block states.
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(opening(state)).getCounterClockWise());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        var blockEntityTag = BlockItem.getBlockEntityData(stack);
        if (!level.isClientSide && stockedOnPlacement
                && (blockEntityTag == null || !blockEntityTag.contains("Items"))
                && level.getBlockEntity(pos) instanceof BookcaseBlockEntity bookcase) {
            for (int slot = 0; slot < BookcaseBlockEntity.SLOT_COUNT; slot++) {
                if (level.random.nextInt(10) < 8) {
                    bookcase.setItem(slot, StoryBook.create(level.random));
                }
            }
        }
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableBookcase.get()
                || !(level.getBlockEntity(pos) instanceof BookcaseBlockEntity bookcase)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BlockItem item && item.getBlock() instanceof BookcaseBlock) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Direction opening = opening(state);
            int slot = slotAt(pos, hit.getLocation(), opening);
            if (player.isShiftKeyDown()) {
                ejectShiftedStack(state, level, pos, opening, bookcase, slot);
            } else if (bookcase.getItem(slot).isEmpty() && bookcase.canPlaceItem(slot, held)) {
                bookcase.setItem(slot, held.split(Math.min(held.getMaxStackSize(), bookcase.getMaxStackSize())));
            } else {
                player.openMenu(new SimpleMenuProvider(
                        (id, inventory, viewer) -> new BookcaseMenu(id, inventory, bookcase), getName()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    static Direction opening(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    static int slotAt(BlockPos pos, Vec3 hit, Direction opening) {
        Direction right = opening.getCounterClockWise();
        double offset = (hit.x - pos.getX() - 0.5) * right.getStepX()
                + (hit.z - pos.getZ() - 0.5) * right.getStepZ();
        int columns = BookcaseBlockEntity.COLUMNS;
        int column = Mth.clamp(Mth.floor((offset + 0.5) * columns), 0, columns - 1);
        return (hit.y - pos.getY() >= 0.5 ? 0 : columns) + column;
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return BiblioConfig.COMMON.enableBookcase.get() && level.getBlockEntity(pos) instanceof BookcaseBlockEntity bookcase
                ? Integer.bitCount(bookcase.getOccupiedSlots()) / 8.0F : 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // The query direction points from the receiving block toward this block.
        return direction != opening(state).getOpposite()
                && level.getBlockEntity(pos) instanceof BookcaseBlockEntity bookcase
                ? bookcase.getRedstoneSignal() : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    public static void updateSignalNeighbors(Level level, BlockPos pos, BlockState state) {
        updatePowerNeighbors(level, pos, state, opening(state));
    }

    @Override
    public void afterDrill(Level level, BlockPos pos, BlockState previous, BlockState adjusted) {
        if (opening(previous) != opening(adjusted)
                && level.getBlockEntity(pos) instanceof BookcaseBlockEntity bookcase && bookcase.getRedstoneSignal() > 0) {
            updatePowerNeighbors(level, pos, adjusted, null);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean powered = !level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof BookcaseBlockEntity bookcase && bookcase.getRedstoneSignal() > 0;
        super.onRemove(state, level, pos, replacement, moving);
        if (powered) {
            updateSignalNeighbors(level, pos, state);
        }
    }
}
