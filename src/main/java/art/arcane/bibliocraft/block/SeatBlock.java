package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.SeatBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.entity.SeatEntity;
import art.arcane.bibliocraft.item.DrillSelection;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class SeatBlock extends InventoryFurnitureBlock {
    public static final BooleanProperty OCCUPIED = BlockStateProperties.OCCUPIED;
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST);
    private static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST, BlockStateProperties.WEST);
    private static final VoxelShape SEAT = Shapes.box(0.15, 0.54, 0.15, 0.85, 0.751, 0.85);
    private static final VoxelShape CARPET = Shapes.box(0, 0, 0, 1, 0.063, 1);
    private static final List<VoxelShape> EXTENSIONS = List.of(
            Shapes.box(0.15, 0.54, 0, 0.85, 0.751, 0.231), Shapes.box(0, 0.54, 0.15, 0.231, 0.751, 0.85),
            Shapes.box(0.15, 0.54, 0.769, 0.85, 0.751, 1), Shapes.box(0.769, 0.54, 0.15, 1, 0.751, 0.85));
    private static final List<VoxelShape> LEGS = List.of(
            Shapes.box(0.596, 0, 0.157, 0.843, 0.585, 0.418), Shapes.box(0.157, 0, 0.157, 0.418, 0.585, 0.404),
            Shapes.box(0.157, 0, 0.582, 0.404, 0.585, 0.843), Shapes.box(0.582, 0, 0.596, 0.843, 0.585, 0.843));
    private static final List<VoxelShape> BRACES = List.of(
            Shapes.box(0.300, 0.197, 0.244, 0.695, 0.257, 0.304), Shapes.box(0.244, 0.197, 0.305, 0.304, 0.257, 0.700),
            Shapes.box(0.305, 0.197, 0.696, 0.700, 0.257, 0.756), Shapes.box(0.696, 0.197, 0.300, 0.756, 0.257, 0.695));
    private static final List<Integer> BACK_PARTS = List.of(0, bit(19) | bit(20), bit(21) | bit(22),
            bit(21) | bit(22) | bit(23), bit(24) | bit(25), bit(21) | bit(22) | bit(26));
    private static final List<Map<Direction, VoxelShape>> SHAPES = java.util.stream.IntStream.range(0, 16)
            .mapToObj(SeatBlock::shapeForConnections).toList();

    public SeatBlock(Properties properties) {
        super(properties, SEAT, "seat");
        registerDefaultState(defaultBlockState().setValue(OCCUPIED, false).setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.WEST, false).setValue(BlockStateProperties.SOUTH, false).setValue(BlockStateProperties.EAST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(OCCUPIED, BlockStateProperties.NORTH, BlockStateProperties.WEST, BlockStateProperties.SOUTH, BlockStateProperties.EAST);
    }

    public static Direction front(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite().getClockWise());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return mapConnections(state, super.rotate(state, rotation), rotation::rotate);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction newFront = mirror.mirror(front(state));
        return mapConnections(state, state.setValue(FACING, newFront.getClockWise()), mirror::mirror);
    }

    private static BlockState mapConnections(BlockState from, BlockState to, UnaryOperator<Direction> transform) {
        for (Direction side : SIDES) {
            to = to.setValue(CONNECTIONS.get(transform.apply(side)), from.getValue(CONNECTIONS.get(side)));
        }
        return to;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        BooleanProperty connection = CONNECTIONS.get(side);
        if (connection != null && state.getValue(connection)
                && (!neighbor.is(this) || !neighbor.getValue(CONNECTIONS.get(side.getOpposite())))) {
            return state.setValue(connection, false);
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES.get(localConnections(state)).get(state.getValue(FACING));
        return level.getBlockEntity(pos) instanceof SeatBlockEntity seat
                && SeatBlockEntity.carpetColor(seat.getItem(SeatBlockEntity.CARPET)) != null
                ? Shapes.or(shape, CARPET) : shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static int localConnections(BlockState state) {
        Rotation rotation = switch (state.getValue(FACING)) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
        int mask = 0;
        for (int side = 0; side < SIDES.size(); side++) {
            if (state.getValue(CONNECTIONS.get(rotation.rotate(SIDES.get(side))))) {
                mask |= bit(side);
            }
        }
        return mask;
    }

    private static int bit(int index) {
        return 1 << index;
    }

    private static boolean hasLeg(int connections, int corner) {
        int northSouth = bit(0) | bit(2);
        int westEast = bit(1) | bit(3);
        if ((connections & northSouth) == northSouth || (connections & westEast) == westEast) {
            return false;
        }
        int cornerSides = bit(corner) | bit((corner + 3) & 3);
        // A corner bench keeps three legs. A single extension drops both legs along its shared edge.
        return Integer.bitCount(connections) == 2 ? connections != cornerSides : (connections & cornerSides) == 0;
    }

    private static Map<Direction, VoxelShape> shapeForConnections(int connections) {
        VoxelShape shape = SEAT;
        for (int side = 0; side < SIDES.size(); side++) {
            if ((connections & bit(side)) != 0) {
                shape = Shapes.or(shape, EXTENSIONS.get(side));
            }
            if (hasLeg(connections, side)) {
                shape = Shapes.or(shape, LEGS.get(side));
                if (hasLeg(connections, (side + 1) & 3)) {
                    shape = Shapes.or(shape, BRACES.get(side));
                }
            }
        }
        return horizontalShapes(shape);
    }

    public static int visibleParts(BlockState state, int back, boolean carpet) {
        int connections = localConnections(state);
        // Material indexes follow seat.mtl. Extensions run north, west, south, east in the OBJ.
        int visible = bit(0) | bit(1) | BACK_PARTS.get(back);
        for (int side = 0; side < SIDES.size(); side++) {
            if ((connections & bit(side)) != 0) {
                visible |= bit(10 + side) | bit(14 + side);
            }
            if (hasLeg(connections, side)) {
                visible |= bit(2 + side);
                if (hasLeg(connections, (side + 1) & 3)) {
                    visible |= bit(6 + side);
                }
            }
        }
        if (back == 1 || back == 4) {
            if ((connections & bit(0)) != 0) {
                visible |= bit(back == 1 ? 27 : 29);
            }
            if ((connections & bit(2)) != 0) {
                visible |= bit(back == 1 ? 28 : 30);
            }
        }
        return carpet ? visible | bit(18) : visible;
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (!BiblioConfig.COMMON.enableSeat.get() || hand != InteractionHand.MAIN_HAND
                || !(level.getBlockEntity(pos) instanceof SeatBlockEntity seat)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            var held = player.getMainHandItem();
            int slot = SeatBlockEntity.attachmentSlot(held, hit.getDirection());
            if (slot >= 0 && seat.getItem(slot).isEmpty()) {
                seat.setItem(slot, held.split(1));
            } else if (slot != SeatBlockEntity.CARPET) {
                if (player.isShiftKeyDown()) {
                    SeatEntity mount = SeatEntity.find(server, pos);
                    if (mount != null && mount.getFirstPassenger() instanceof PathfinderMob mob) {
                        seat.delayMobScan(server);
                        mob.stopRiding();
                    }
                } else {
                    SeatEntity.sit(server, pos, player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult adjustWithDrill(UseOnContext context, boolean sneaking) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        var face = context.getClickedFace();
        if (!(level.getBlockEntity(pos) instanceof SeatBlockEntity seat)) {
            return InteractionResult.PASS;
        }
        if (sneaking) {
            if (!level.isClientSide) {
                if (face == Direction.UP) {
                    connect(context, seat);
                } else {
                    BlockState state = seat.getBlockState();
                    level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        int slot = seat.removableAttachment(face);
        if (slot < 0) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ejectStack(level, Vec3.atBottomCenterOf(pos).add(0, 1, 0), seat, slot);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void connect(UseOnContext context, SeatBlockEntity seat) {
        BlockPos previous = DrillSelection.select(context, "SeatSelection", "message.bibliocraft.seat.selected");
        if (previous == null) {
            return;
        }
        var player = context.getPlayer();
        var level = context.getLevel();
        var pos = context.getClickedPos();
        Direction direction = SIDES.stream().filter(side -> pos.relative(side).equals(previous)).findFirst().orElse(null);
        if (direction == null || !direction.getAxis().isHorizontal()
                || !(level.getBlockEntity(previous) instanceof SeatBlockEntity neighbor) || !neighbor.canAccess(player)) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.seat.invalid"), true);
            return;
        }
        var side = CONNECTIONS.get(direction);
        var opposite = CONNECTIONS.get(direction.getOpposite());
        BlockState a = seat.getBlockState();
        BlockState b = neighbor.getBlockState();
        boolean connect = !(a.getValue(side) && b.getValue(opposite));
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pos, a.setValue(side, connect), flags);
        level.setBlock(previous, b.setValue(opposite, connect), flags);
        DrillSelection.reportJoin(player, connect);
    }

    public static void setOccupied(ServerLevel level, BlockPos pos, boolean occupied) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SeatBlock && state.getValue(OCCUPIED) != occupied) {
            level.setBlock(pos, state.setValue(OCCUPIED, occupied), Block.UPDATE_CLIENTS);
            if (BiblioConfig.COMMON.chairRedstone.get()) {
                updatePowerNeighbors(level, pos, state, null);
            }
        }
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return BiblioConfig.COMMON.enableSeat.get() && BiblioConfig.COMMON.chairRedstone.get();
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return isSignalSource(state) && state.getValue(OCCUPIED) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (!replacement.is(this) && level instanceof ServerLevel server) {
            SeatEntity mount = SeatEntity.find(server, pos);
            if (mount != null) {
                mount.discard();
            }
            if (state.getValue(OCCUPIED) && isSignalSource(state)) {
                updatePowerNeighbors(level, pos, state, null);
            }
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.SEAT.get()) {
            return null;
        }
        return (world, pos, current, entity) -> ((SeatBlockEntity) entity).tick(server);
    }
}
