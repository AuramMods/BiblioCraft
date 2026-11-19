package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.ClockBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.DrillSelection;
import art.arcane.bibliocraft.menu.ClockMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class ClockBlock extends HorizontalFacingEntityBlock {
    public static final EnumProperty<ClockPart> PART = EnumProperty.create("part", ClockPart.class);
    public static final IntegerProperty SHIFT = IntegerProperty.create("shift", 0, 2);
    private static final double[] DEPTHS = {0, 0.35, 0.675};
    private static final int PAIR_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final List<Map<Direction, VoxelShape>> SHAPES = Arrays.stream(DEPTHS)
            .mapToObj(depth -> horizontalShapes(Shapes.box(depth, 0, 0.2549, depth + 0.3372, 1, 0.7451))).toList();

    public ClockBlock(Properties properties) {
        super(properties, Shapes.empty(), "clock");
        registerDefaultState(defaultBlockState().setValue(PART, ClockPart.WALL).setValue(SHIFT, 0));
    }

    public static double depth(BlockState state) {
        return DEPTHS[state.getValue(SHIFT)];
    }

    public static @Nullable BlockPos partner(BlockGetter level, BlockPos pos, BlockState state) {
        ClockPart part = state.getValue(PART);
        Direction side = part.partnerSide();
        if (side == null) {
            return null;
        }
        BlockPos otherPos = pos.relative(side);
        BlockState other = level.getBlockState(otherPos);
        ClockPart expected = part == ClockPart.LOWER ? ClockPart.UPPER : ClockPart.LOWER;
        return other.is(state.getBlock()) && other.getValue(PART) == expected && aligns(state, other) ? otherPos : null;
    }

    public static @Nullable ClockBlockEntity mainClock(BlockGetter level, BlockPos clicked) {
        if (!(level.getBlockEntity(clicked) instanceof ClockBlockEntity clock)) {
            return null;
        }
        BlockState state = clock.getBlockState();
        if (state.getValue(PART) != ClockPart.LOWER) {
            return clock;
        }
        BlockPos other = partner(level, clicked, state);
        return other != null && level.getBlockEntity(other) instanceof ClockBlockEntity top ? top : null;
    }

    private static boolean aligns(BlockState first, BlockState second) {
        return first.getValue(FACING) == second.getValue(FACING) && first.getValue(SHIFT).equals(second.getValue(SHIFT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART, SHIFT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite().getCounterClockWise());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction front = state.getValue(FACING).getClockWise();
        return state.setValue(FACING, mirror.mirror(front).getCounterClockWise());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(SHIFT)).get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableClock.get()
                || !(level.getBlockEntity(pos) instanceof ClockBlockEntity clicked)) {
            return InteractionResult.PASS;
        }
        ClockBlockEntity clock = mainClock(level, pos);
        if (!clicked.canAccess(player) || (clock != null && !clock.canAccess(player))) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        if (clock != null && player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, viewer) -> new ClockMenu(id, clicked, clock), getName()),
                    clock.getSettings()::write);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult adjustWithDrill(UseOnContext context, boolean joining) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (joining) {
            joinWithDrill(context, state);
            return InteractionResult.CONSUME;
        }
        BlockPos other = partner(level, pos, state);
        if (other != null && level.getBlockEntity(other) instanceof ClockBlockEntity clock && !clock.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        BlockState shifted = state.cycle(SHIFT);
        level.setBlock(pos, shifted, PAIR_UPDATE);
        if (other != null) {
            level.setBlock(other, level.getBlockState(other).setValue(SHIFT, shifted.getValue(SHIFT)), PAIR_UPDATE);
        }
        return InteractionResult.CONSUME;
    }

    private void joinWithDrill(UseOnContext context, BlockState state) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockPos selected = DrillSelection.select(context, "ClockSelection", "message.bibliocraft.clock.selected");
        if (selected == null) {
            return;
        }
        if ((!pos.above().equals(selected) && !pos.below().equals(selected))
                || !(level.getBlockEntity(selected) instanceof ClockBlockEntity other)
                || !other.canAccess(player) || !aligns(state, other.getBlockState())) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.clock.invalid"), true);
            return;
        }
        BlockState otherState = other.getBlockState();
        boolean joining = state.getValue(PART) == ClockPart.WALL && otherState.getValue(PART) == ClockPart.WALL;
        if (!joining && !selected.equals(partner(level, pos, state))) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.clock.invalid"), true);
            return;
        }
        BlockPos lower = selected.getY() < pos.getY() ? selected : pos;
        BlockPos upper = lower.above();
        BlockState lowerState = level.getBlockState(lower);
        BlockState upperState = level.getBlockState(upper);
        if (joining && level.getBlockEntity(lower) instanceof ClockBlockEntity lowerClock) {
            lowerClock.stopOwnPulse();
        }
        level.setBlock(lower, lowerState.setValue(PART, joining ? ClockPart.LOWER : ClockPart.WALL), PAIR_UPDATE);
        level.setBlock(upper, upperState.setValue(PART, joining ? ClockPart.UPPER : ClockPart.WALL), PAIR_UPDATE);
        updatePowerNeighbors(level, lower, lowerState, null);
        updatePowerNeighbors(level, upper, upperState, null);
    }

    public static void updateClockPower(Level level, BlockPos pos, BlockState state) {
        updatePowerNeighbors(level, pos, state, null);
        BlockPos other = partner(level, pos, state);
        if (other != null) {
            updatePowerNeighbors(level, other, level.getBlockState(other), null);
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        ClockBlockEntity clock = mainClock(level, pos);
        return clock != null && clock.isPowered() ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return getSignal(state, level, pos, direction);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative() && level.getBlockEntity(pos) instanceof ClockBlockEntity clock && clock.isLocked()) {
            return 0;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        return level.getBlockEntity(pos) instanceof ClockBlockEntity clock ? clock.asItem(stack) : stack;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !replacement.is(this)) {
            BlockPos other = partner(level, pos, state);
            if (other != null) {
                BlockState otherState = level.getBlockState(other).setValue(PART, ClockPart.WALL);
                level.setBlock(other, otherState, PAIR_UPDATE);
                updatePowerNeighbors(level, other, otherState, null);
            }
            updatePowerNeighbors(level, pos, state, null);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.CLOCK.get()
                ? (world, pos, current, entity) -> ((ClockBlockEntity) entity).tick() : null;
    }
}
