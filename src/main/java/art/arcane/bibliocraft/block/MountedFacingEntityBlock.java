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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class MountedFacingEntityBlock extends PlaceholderEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;

    private final Map<Direction, VoxelShape> floorShapesByFacing;
    private final Map<Direction, VoxelShape> ceilingShapesByFacing;
    private final Map<Direction, VoxelShape> floorCollisionByFacing;
    private final Map<Direction, VoxelShape> ceilingCollisionByFacing;
    private final Map<Direction, VoxelShape> wallShapesByFacing;
    private final Map<Direction, VoxelShape> wallCollisionByFacing;

    public MountedFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            VoxelShape ceilingShape,
            String blockEntityPath
    ) {
        this(properties, floorShape, wallShape, ceilingShape, floorShape, wallShape, ceilingShape, blockEntityPath);
    }

    public MountedFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            VoxelShape ceilingShape,
            VoxelShape floorCollisionShape,
            VoxelShape wallCollisionShape,
            VoxelShape ceilingCollisionShape,
            String blockEntityPath
    ) {
        super(properties, floorShape, floorCollisionShape, blockEntityPath);
        this.floorShapesByFacing = createRotations(floorShape);
        this.ceilingShapesByFacing = createRotations(ceilingShape);
        this.floorCollisionByFacing = createRotations(floorCollisionShape);
        this.ceilingCollisionByFacing = createRotations(ceilingCollisionShape);
        this.wallShapesByFacing = createRotations(wallShape);
        this.wallCollisionByFacing = createRotations(wallCollisionShape);
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
            case FLOOR -> floorShapesByFacing.getOrDefault(facing, floorShapesByFacing.get(Direction.NORTH));
            case CEILING -> ceilingShapesByFacing.getOrDefault(facing, ceilingShapesByFacing.get(Direction.NORTH));
            case WALL -> wallShapesByFacing.getOrDefault(facing, wallShapesByFacing.get(Direction.NORTH));
        };
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return switch (state.getValue(FACE)) {
            case FLOOR -> floorCollisionByFacing.getOrDefault(facing, floorCollisionByFacing.get(Direction.NORTH));
            case CEILING -> ceilingCollisionByFacing.getOrDefault(facing, ceilingCollisionByFacing.get(Direction.NORTH));
            case WALL -> wallCollisionByFacing.getOrDefault(facing, wallCollisionByFacing.get(Direction.NORTH));
        };
    }

    private static AttachFace getPlacementFace(BlockPlaceContext context) {
        Player player = context.getPlayer();
        if (player != null) {
            int pitch = Mth.floor(player.getXRot() * 3.0F / 180.0F + 0.5D) & 3;
            pitch++;
            pitch %= 4;
            if (pitch == 0) {
                return AttachFace.CEILING;
            }
            if (pitch == 1) {
                return AttachFace.WALL;
            }
            return AttachFace.FLOOR;
        }

        Direction clickedFace = context.getClickedFace();
        if (clickedFace == Direction.DOWN) {
            return AttachFace.CEILING;
        }
        if (clickedFace == Direction.UP) {
            return AttachFace.FLOOR;
        }
        return AttachFace.WALL;
    }

    private static Map<Direction, VoxelShape> createRotations(VoxelShape northShape) {
        EnumMap<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, northShape);
        map.put(Direction.EAST, rotateShape(northShape, 1));
        map.put(Direction.SOUTH, rotateShape(northShape, 2));
        map.put(Direction.WEST, rotateShape(northShape, 3));
        return map;
    }

    private static VoxelShape rotateShape(VoxelShape shape, int quarterTurnsClockwise) {
        VoxelShape output = shape;
        for (int i = 0; i < quarterTurnsClockwise; i++) {
            output = rotateShapeClockwise(output);
        }
        return output;
    }

    private static VoxelShape rotateShapeClockwise(VoxelShape shape) {
        VoxelShape[] buffer = new VoxelShape[] { Shapes.empty() };
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                buffer[0] = Shapes.or(buffer[0], Block.box(16.0 - maxZ, minY, minX, 16.0 - minZ, maxY, maxX))
        );
        return buffer[0];
    }
}
