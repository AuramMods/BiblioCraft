package art.arcane.bibliocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class HorizontalFacingEntityBlock extends PlaceholderEntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    private final Map<Direction, VoxelShape> shapesByFacing;
    private final Map<Direction, VoxelShape> collisionByFacing;

    public HorizontalFacingEntityBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        this(properties, shape, shape, blockEntityPath);
    }

    public HorizontalFacingEntityBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape, String blockEntityPath) {
        super(properties, shape, collisionShape, blockEntityPath);
        this.shapesByFacing = createRotations(shape);
        this.collisionByFacing = createRotations(collisionShape);
        registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return shapesByFacing.getOrDefault(state.getValue(FACING), shapesByFacing.get(Direction.NORTH));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionByFacing.getOrDefault(state.getValue(FACING), collisionByFacing.get(Direction.NORTH));
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
