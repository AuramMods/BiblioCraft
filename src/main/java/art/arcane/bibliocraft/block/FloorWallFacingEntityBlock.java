package art.arcane.bibliocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class FloorWallFacingEntityBlock extends PlaceholderEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    private final Map<Direction, VoxelShape> floorShapesByFacing;
    private final Map<Direction, VoxelShape> wallShapesByFacing;
    private final Map<Direction, VoxelShape> floorCollisionByFacing;
    private final Map<Direction, VoxelShape> wallCollisionByFacing;

    public FloorWallFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            String blockEntityPath
    ) {
        this(properties, floorShape, wallShape, floorShape, wallShape, blockEntityPath);
    }

    public FloorWallFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            VoxelShape floorCollisionShape,
            VoxelShape wallCollisionShape,
            String blockEntityPath
    ) {
        super(properties, floorShape, floorCollisionShape, blockEntityPath);
        this.floorShapesByFacing = horizontalShapes(floorShape);
        this.wallShapesByFacing = horizontalShapes(wallShape);
        this.floorCollisionByFacing = horizontalShapes(floorCollisionShape);
        this.wallCollisionByFacing = horizontalShapes(wallCollisionShape);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FACE, getPlacementFace(context));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case WALL -> wallShapesByFacing.getOrDefault(facing, wallShapesByFacing.get(Direction.NORTH));
            default -> floorShapesByFacing.getOrDefault(facing, floorShapesByFacing.get(Direction.NORTH));
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case WALL -> wallCollisionByFacing.getOrDefault(facing, wallCollisionByFacing.get(Direction.NORTH));
            default -> floorCollisionByFacing.getOrDefault(facing, floorCollisionByFacing.get(Direction.NORTH));
        };
    }

    private static AttachFace getPlacementFace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            int pitch = Mth.floor(player.getXRot() * 3.0F / 180.0F + 0.5D) & 3;
            pitch++;
            pitch %= 2;
            return pitch == 1 ? AttachFace.WALL : AttachFace.FLOOR;
        }

        return context.getClickedFace() == Direction.UP ? AttachFace.FLOOR : AttachFace.WALL;
    }
}
