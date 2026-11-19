package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FurnitureInventoryBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public abstract class ShiftableFurnitureBlock extends WoodenFurnitureBlock implements DrillAdjustable {
    public static final IntegerProperty SHIFT = IntegerProperty.create("shift", 0, 2);
    private final double baseStep;
    private final List<Map<Direction, VoxelShape>> shiftedShapes;

    protected ShiftableFurnitureBlock(Properties properties, VoxelShape shape, String id, double baseStep) {
        super(properties, shape, id);
        this.baseStep = baseStep;
        shiftedShapes = List.of(horizontalShapes(shape), horizontalShapes(shape.move(baseStep, 0, 0)),
                horizontalShapes(shape.move(baseStep * 2, 0, 0)));
        registerDefaultState(defaultBlockState().setValue(SHIFT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SHIFT);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shiftedShapes.get(state.getValue(SHIFT)).get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public BlockState adjustWithDrill(BlockState state, boolean sneaking) {
        return sneaking ? DrillAdjustable.super.adjustWithDrill(state, true) : state.cycle(SHIFT);
    }

    public final double localShift(BlockState state) {
        return baseStep * state.getValue(SHIFT);
    }

    protected final boolean ejectShiftedStack(BlockState state, Level level, BlockPos pos, Direction opening,
                                              FurnitureInventoryBlockEntity furniture, int slot) {
        Vec3 drop = Vec3.atCenterOf(pos).relative(opening, 0.25)
                .relative(state.getValue(FACING).getClockWise(), localShift(state));
        return ejectStack(level, drop, furniture, slot);
    }
}
