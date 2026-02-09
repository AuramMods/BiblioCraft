package art.arcane.bibliocraft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ColorFacingEntityBlock extends HorizontalFacingEntityBlock {
    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public ColorFacingEntityBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        this(properties, shape, shape, blockEntityPath);
    }

    public ColorFacingEntityBlock(Properties properties, VoxelShape shape, VoxelShape collisionShape, String blockEntityPath) {
        super(properties, shape, collisionShape, blockEntityPath);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }
}
