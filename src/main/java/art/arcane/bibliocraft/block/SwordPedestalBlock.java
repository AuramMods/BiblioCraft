package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.SwordPedestalBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public final class SwordPedestalBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    private static final Map<Direction, VoxelShape> OCCUPIED_OUTLINES =
            horizontalShapes(Block.box(4.4128, 0, 1.1392, 11.5872, 16, 14.8608));

    public SwordPedestalBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "sword_pedestal");
        registerDefaultState(defaultBlockState().setValue(ColorFacingEntityBlock.COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ColorFacingEntityBlock.COLOR);
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableSwordPedestal.get()
                || !(level.getBlockEntity(pos) instanceof SwordPedestalBlockEntity pedestal)) {
            return InteractionResult.PASS;
        }
        InteractionResult dye = tryApplyDye(state, level, pos, player, hand);
        if (dye.consumesAction()) {
            return dye;
        }
        ItemStack held = player.getItemInHand(hand);
        if (pedestal.isEmpty() && !pedestal.canPlaceItem(0, held)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            if (pedestal.isEmpty()) {
                insertStack(pedestal, 0, held);
            } else {
                Direction opening = Direction.getNearest(player.getX() - pos.getX() - 0.5, 0,
                        player.getZ() - pos.getZ() - 0.5);
                ejectStack(level, pos, opening, pedestal, 0);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return displaysSword(level, pos) ? OCCUPIED_OUTLINES.get(state.getValue(FACING))
                : super.getShape(state, level, pos, context);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ColoredBlockItem.applyColor(super.getCloneItemStack(level, pos, state),
                state.getValue(ColorFacingEntityBlock.COLOR));
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return displaysSword(level, pos) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    public static void updateSignalNeighbors(Level level, BlockPos pos, BlockState state) {
        updatePowerNeighbors(level, pos, state, null);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        boolean powered = !level.isClientSide && !state.is(replacement.getBlock()) && displaysSword(level, pos);
        super.onRemove(state, level, pos, replacement, moving);
        if (powered) {
            updateSignalNeighbors(level, pos, state);
        }
    }

    private static boolean displaysSword(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SwordPedestalBlockEntity pedestal && !pedestal.isEmpty();
    }
}
