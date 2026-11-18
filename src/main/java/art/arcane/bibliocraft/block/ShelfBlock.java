package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.ShelfBlockEntity;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.menu.ShelfMenu;
import art.arcane.bibliocraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ShelfBlock extends HorizontalFacingEntityBlock {
    public static final BooleanProperty TOP = BooleanProperty.create("top");

    public ShelfBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "shelf");
        registerDefaultState(defaultBlockState().setValue(TOP, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TOP);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // The OBJ opens east in the base north state. Keep the existing saved-state rotations.
        Direction facing = context.getHorizontalDirection().getClockWise();
        boolean top = !context.getLevel().getBlockState(context.getClickedPos().above()).isAir();
        return defaultBlockState().setValue(FACING, facing).setValue(TOP, top);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return direction == Direction.UP
                ? state.setValue(TOP, !neighbor.isAir())
                : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioFeatureToggles.isBlockEnabled(ModBlocks.SHELF.getId())
                || !(level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            Direction opening = state.getValue(FACING).getClockWise();
            if (!player.isShiftKeyDown() && hit.getDirection() == opening) {
                int slot = slotAt(pos, hit.getLocation(), opening);
                if (transferStack(level, pos, opening, shelf, slot, player)) {
                    return InteractionResult.CONSUME;
                }
            }
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, viewer) -> new ShelfMenu(id, inventory, shelf), getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int slotAt(BlockPos pos, Vec3 hit, Direction opening) {
        Direction right = opening.getCounterClockWise();
        double rightOffset = (hit.x - pos.getX() - 0.5) * right.getStepX()
                + (hit.z - pos.getZ() - 0.5) * right.getStepZ();
        int row = hit.y - pos.getY() >= 0.5 ? 0 : 2;
        return row + (rightOffset >= 0 ? 1 : 0);
    }

    private static boolean transferStack(Level level, BlockPos pos, Direction opening,
                                         ShelfBlockEntity shelf, int slot, Player player) {
        ItemStack stored = shelf.getItem(slot);
        ItemStack held = player.getMainHandItem();
        if (!held.isEmpty() && (stored.isEmpty() || ItemStack.isSameItemSameTags(stored, held))) {
            int limit = Math.min(held.getMaxStackSize(), shelf.getMaxStackSize());
            int amount = Math.min(held.getCount(), limit - stored.getCount());
            if (amount > 0) {
                shelf.setItem(slot, held.copyWithCount(stored.getCount() + amount));
                held.shrink(amount);
                return true;
            }
        }
        if (stored.isEmpty()) {
            return false;
        }

        Vec3 drop = Vec3.atCenterOf(pos).relative(opening, 0.25);
        ItemEntity entity = new ItemEntity(level, drop.x, drop.y, drop.z, stored.copy());
        entity.setDefaultPickUpDelay();
        if (level.addFreshEntity(entity)) {
            shelf.setItem(slot, ItemStack.EMPTY);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
            return shelf.getMaterial().applyTo(stack);
        }
        return stack;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof ShelfBlockEntity shelf) {
            Containers.dropContents(level, pos, shelf);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
