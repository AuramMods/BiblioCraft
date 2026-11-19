package art.arcane.bibliocraft.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Furniture that inherited the legacy wooden block's display lighting. */
public abstract class WoodenFurnitureBlock extends InventoryFurnitureBlock {
    public static final IntegerProperty DISPLAY_LIGHT = IntegerProperty.create("display_light", 0, 15);

    protected WoodenFurnitureBlock(Properties properties, VoxelShape shape, String blockEntityPath) {
        super(properties.lightLevel(WoodenFurnitureBlock::lightEmission), shape, blockEntityPath);
        registerDefaultState(defaultBlockState().setValue(DISPLAY_LIGHT, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(DISPLAY_LIGHT);
    }

    private static int lightEmission(BlockState state) {
        return state.getOptionalValue(BlockStateProperties.DOUBLE_BLOCK_HALF).orElse(DoubleBlockHalf.LOWER)
                == DoubleBlockHalf.UPPER ? 0 : state.getValue(DISPLAY_LIGHT);
    }
}
