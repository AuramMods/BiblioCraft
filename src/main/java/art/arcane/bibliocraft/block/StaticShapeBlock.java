package art.arcane.bibliocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
}
