package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.MarkerPoleBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class MarkerPoleBlock extends MountedFacingEntityBlock {
    public static final BooleanProperty TEMPORARY = BooleanProperty.create("temporary");
    private static final VoxelShape FLOOR = Shapes.or(Shapes.box(0.25, 0, 0.25, 0.75, 0.1, 0.75),
            Shapes.box(0.4418, 0.1, 0.4415, 0.5582, 0.9, 0.5585));
    private static final VoxelShape CEILING = Shapes.or(Shapes.box(0.25, 0.9, 0.25, 0.75, 1, 0.75),
            Shapes.box(0.4418, 0.1, 0.4415, 0.5582, 0.9, 0.5585));
    private static final VoxelShape NORTH_WALL = Shapes.or(Shapes.box(0.25, 0.25, 0.9, 0.75, 0.75, 1),
            Shapes.box(0.4418, 0.4415, 0.1, 0.5582, 0.5585, 0.9));

    public MarkerPoleBlock(Properties properties) {
        super(properties, FLOOR, NORTH_WALL, CEILING, "marker_pole");
        registerDefaultState(defaultBlockState().setValue(TEMPORARY, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(TEMPORARY);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return BiblioConfig.COMMON.enableTapemeasure.get() ? attachedTo(context.getClickedFace()) : null;
    }

    public BlockState attachedTo(Direction face) {
        return switch (face) {
            case UP -> defaultBlockState().setValue(FACE, AttachFace.FLOOR);
            case DOWN -> defaultBlockState().setValue(FACE, AttachFace.CEILING);
            default -> defaultBlockState().setValue(FACE, AttachFace.WALL).setValue(FACING, face);
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarkerPoleBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(TEMPORARY) ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        Direction support = switch (state.getValue(FACE)) {
            case FLOOR -> Direction.DOWN;
            case CEILING -> Direction.UP;
            case WALL -> state.getValue(FACING).getOpposite();
        };
        return state.getValue(TEMPORARY) && direction == support && neighbor.isAir()
                ? Blocks.AIR.defaultBlockState() : super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(TEMPORARY) && level.getBlockEntity(pos) instanceof MarkerPoleBlockEntity pole) {
            pole.checkMeasurement(level);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (level instanceof ServerLevel server && state.getValue(TEMPORARY)
                && (!replacement.is(this) || !replacement.getValue(TEMPORARY))
                && level.getBlockEntity(pos) instanceof MarkerPoleBlockEntity pole) {
            pole.unregister(server);
        }
        super.onRemove(state, level, pos, replacement, moving);
    }
}
