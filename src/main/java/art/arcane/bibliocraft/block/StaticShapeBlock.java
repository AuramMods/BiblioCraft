package art.arcane.bibliocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class StaticShapeBlock extends Block {
    private final VoxelShape shape;
    private final VoxelShape collisionShape;

    public StaticShapeBlock(Properties properties, VoxelShape shape) {
        this(properties, shape, shape);
    }

    public StaticShapeBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape) {
        super(properties);
        this.shape = shape;
        this.collisionShape = collisionShape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return collisionShape;
    }

    @Override
    public VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    protected static Map<Direction, VoxelShape> horizontalShapes(VoxelShape northShape) {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        shapes.put(Direction.NORTH, northShape);
        shapes.put(Direction.EAST, rotateClockwise(northShape));
        shapes.put(Direction.SOUTH, rotateClockwise(shapes.get(Direction.EAST)));
        shapes.put(Direction.WEST, rotateClockwise(shapes.get(Direction.SOUTH)));
        return shapes;
    }

    private static VoxelShape rotateClockwise(VoxelShape shape) {
        VoxelShape[] rotated = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                rotated[0] = Shapes.or(rotated[0], Shapes.box(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
        return rotated[0];
    }
}
