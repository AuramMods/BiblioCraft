package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.DiscRackBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.DiscRackMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public final class DiscRackBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    public static final EnumProperty<DiscRackMount> MOUNT = EnumProperty.create("mount", DiscRackMount.class);
    public static final double FIRST_SLOT_CENTER = 0.918;
    public static final double SLOT_SPACING = 0.1045;
    private static final Map<Direction, VoxelShape> WALL = horizontalShapes(Shapes.box(0, 0, 0.25, 0.35, 1, 0.75));
    private static final Map<Direction, VoxelShape> WALL_HORIZONTAL =
            horizontalShapes(Shapes.box(0, 0.25, 0, 0.35, 0.75, 1));
    private static final Map<Direction, VoxelShape> CEILING = horizontalShapes(Shapes.box(0, 0.65, 0.25, 1, 1, 0.75));

    public DiscRackBlock(Properties properties, VoxelShape floorShape) {
        super(properties, floorShape, "disc_rack");
        registerDefaultState(defaultBlockState().setValue(MOUNT, DiscRackMount.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(MOUNT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction side = context.getClickedFace();
        Player player = context.getPlayer();
        DiscRackMount mount;
        if (player == null) {
            mount = side == Direction.UP ? DiscRackMount.FLOOR
                    : side == Direction.DOWN ? DiscRackMount.CEILING : DiscRackMount.WALL;
        } else {
            float pitch = player.getXRot();
            mount = pitch < -30 ? DiscRackMount.CEILING : pitch < 30 ? DiscRackMount.WALL : DiscRackMount.FLOOR;
        }
        Direction facing = context.getHorizontalDirection().getOpposite();
        if (mount.isWall()) {
            Direction normal = side.getAxis().isHorizontal() ? side : facing;
            facing = normal.getCounterClockWise();
        }
        return defaultBlockState().setValue(MOUNT, mount).setValue(FACING, facing);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (state.getValue(MOUNT).isWall()) {
            Direction opening = state.getValue(MOUNT).opening(state.getValue(FACING));
            return state.setValue(FACING, mirror.mirror(opening).getCounterClockWise());
        }
        return super.mirror(state, mirror);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(MOUNT)) {
            case FLOOR -> super.getShape(state, level, pos, context);
            case WALL -> WALL.get(facing);
            case WALL_HORIZONTAL -> WALL_HORIZONTAL.get(facing);
            case CEILING -> CEILING.get(facing);
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState adjustWithDrill(BlockState state, boolean sneaking) {
        if (sneaking) {
            return DrillAdjustable.super.adjustWithDrill(state, true);
        }
        return switch (state.getValue(MOUNT)) {
            case WALL -> state.setValue(MOUNT, DiscRackMount.WALL_HORIZONTAL);
            case WALL_HORIZONTAL -> state.setValue(MOUNT, DiscRackMount.WALL);
            default -> state;
        };
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableDiscRack.get()
                || !(level.getBlockEntity(pos) instanceof DiscRackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int slot = slotAt(state, pos, hit.getLocation());
            var held = player.getMainHandItem();
            Direction opening = state.getValue(MOUNT).opening(state.getValue(FACING));
            boolean changed = DiscRackBlockEntity.isDisc(held) && insertStack(rack, slot, held);
            if (held.isEmpty() && player.isShiftKeyDown() && !changed) {
                changed = ejectStack(level, pos, opening, rack, slot);
            }
            if (!changed) {
                player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new DiscRackMenu(id, inventory, rack),
                        getName()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static int slotAt(BlockState state, BlockPos pos, Vec3 target) {
        float inverseYaw = (state.getValue(FACING).toYRot() - 180) * Mth.DEG_TO_RAD;
        Vec3 local = target.subtract(Vec3.atCenterOf(pos)).yRot(inverseYaw).add(0.5, 0.5, 0.5);
        double x = state.getValue(MOUNT).slotAxis(local);
        return Mth.clamp((int) Math.round((FIRST_SLOT_CENTER - x) / SLOT_SPACING), 0, DiscRackBlockEntity.SLOT_COUNT - 1);
    }
}
