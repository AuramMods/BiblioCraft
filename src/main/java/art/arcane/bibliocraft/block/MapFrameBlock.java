package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.MapFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.WaypointCompassItem;
import art.arcane.bibliocraft.map.MapPin;
import art.arcane.bibliocraft.menu.MapFrameMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class MapFrameBlock extends InventoryFurnitureBlock {
    public static final EnumProperty<AttachFace> FACE = MountedFacingEntityBlock.FACE;
    private static final VoxelShape WALL = Shapes.box(0, 0, 0, .045, 1, 1);
    private static final VoxelShape FLOOR = Shapes.box(0, 0, 0, 1, .045, 1);
    private static final VoxelShape CEILING = Shapes.box(0, .955, 0, 1, 1, 1);

    public MapFrameBlock(Properties properties) {
        super(properties, WALL, "map_frame");
        registerDefaultState(defaultBlockState().setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        AttachFace mount = player == null
                ? side == Direction.UP ? AttachFace.FLOOR : side == Direction.DOWN ? AttachFace.CEILING : AttachFace.WALL
                : player.getXRot() < -30 ? AttachFace.CEILING : player.getXRot() < 30 ? AttachFace.WALL : AttachFace.FLOOR;
        Direction facing = mount == AttachFace.WALL
                ? (side.getAxis().isHorizontal() ? side : context.getHorizontalDirection().getOpposite()).getCounterClockWise()
                : Direction.NORTH;
        return defaultBlockState().setValue(FACE, mount).setValue(FACING, facing);
    }

    public static Direction opening(BlockState state) {
        return switch (state.getValue(FACE)) {
            case FLOOR -> Direction.UP;
            case CEILING -> Direction.DOWN;
            case WALL -> state.getValue(FACING).getClockWise();
        };
    }

    public static @Nullable MapPin.Point pointAt(BlockState state, BlockHitResult hit, int rotation) {
        if (hit.getDirection() != opening(state)) {
            return null;
        }
        Vec3 p = northHit(state, hit);
        MapPin.Point point = switch (state.getValue(FACE)) {
            case WALL -> new MapPin.Point(1 - p.z, 1 - p.y);
            case FLOOR -> new MapPin.Point(p.x, p.z);
            case CEILING -> new MapPin.Point(p.x, 1 - p.z);
        };
        return MapPin.Point.valid(point.u(), point.v()) ? point.rotate(-rotation) : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case WALL -> super.getShape(state, world, pos, context);
            case FLOOR -> FLOOR;
            case CEILING -> CEILING;
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return getShape(state, world, pos, context);
    }

    public static int visibleParts(BlockGetter world, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        var face = state.getValue(FACE);
        Direction top = switch (face) {
            case WALL -> Direction.UP;
            case FLOOR -> facing;
            case CEILING -> facing.getOpposite();
        };
        Direction right = face == AttachFace.WALL ? facing : facing.getClockWise();
        boolean t = connected(world, pos.relative(top), state);
        boolean b = connected(world, pos.relative(top.getOpposite()), state);
        boolean r = connected(world, pos.relative(right), state);
        boolean l = connected(world, pos.relative(right.getOpposite()), state);
        return 1 | (!t ? 2 : 0) | (!r ? 4 : 0) | (!b ? 8 : 0) | (!l ? 16 : 0)
                | (!t || !l ? 32 : 0) | (!t || !r ? 64 : 0)
                | (!b || !r ? 128 : 0) | (!b || !l ? 256 : 0);
    }

    private static boolean connected(BlockGetter world, BlockPos pos, BlockState state) {
        BlockState other = world.getBlockState(pos);
        return other.is(state.getBlock()) && state.getValue(FACE) == other.getValue(FACE)
                && (state.getValue(FACE) != AttachFace.WALL || state.getValue(FACING) == other.getValue(FACING));
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableMapFrame.get()
                || !(level.getBlockEntity(pos) instanceof MapFrameBlockEntity frame)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        ItemStack map = frame.getItem(MapFrameBlockEntity.MAP);
        boolean drilling = (held.is(ModItems.HAND_DRILL.get()) && BiblioConfig.COMMON.enableHandDrill.get())
                || (held.is(ModItems.BIBLIO_DRILL.get()) && BiblioConfig.COMMON.enableDrill.get());
        MapPin.Point point = pointAt(state, hit, frame.rotation());
        MapPin pin = point == null ? null : frame.pinAt(point);
        if (player.isShiftKeyDown() && !map.isEmpty()) {
            if (!level.isClientSide) {
                frame.rotateMap();
            }
        } else if (held.is(ModItems.MAP_TOOL.get()) && point != null && !map.isEmpty()) {
            if (player instanceof ServerPlayer serverPlayer) {
                var data = MapItem.getSavedData(map, level);
                if (data != null) {
                    MapFrameMenu.open(serverPlayer, frame, hand, point, data);
                }
            }
        } else if (map.isEmpty() && frame.canPlaceItem(MapFrameBlockEntity.MAP, held)) {
            if (!level.isClientSide) {
                frame.setItem(MapFrameBlockEntity.MAP, held.split(1));
            }
        } else if (drilling && !map.isEmpty()) {
            if (!level.isClientSide && ejectStack(level, pos, opening(state), frame, MapFrameBlockEntity.MAP)) {
                level.playSound(null, pos, held.is(ModItems.HAND_DRILL.get()) ? ModSounds.DRILL.get() : ModSounds.SCREW.get(),
                        SoundSource.BLOCKS, 0.8f, 1);
            }
        } else if (pin != null) {
            if (!level.isClientSide) {
                var data = MapItem.getSavedData(map, level);
                if (data != null) {
                    var target = pin.target(data);
                    if (held.is(ModItems.WAYPOINT_COMPASS.get()) && BiblioConfig.COMMON.enableWaypointCompass.get()) {
                        WaypointCompassItem.setTarget(held, target);
                        player.getInventory().setChanged();
                    }
                    player.displayClientMessage(target.description(), false);
                }
            }
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.MAP_FRAME.get()) {
            return null;
        }
        return (world, pos, current, entity) -> ((MapFrameBlockEntity) entity).tickMap(server);
    }
}
