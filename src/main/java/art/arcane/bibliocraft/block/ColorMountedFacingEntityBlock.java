package art.arcane.bibliocraft.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;

public class ColorMountedFacingEntityBlock extends MountedFacingEntityBlock {
    public ColorMountedFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            VoxelShape ceilingShape,
            String blockEntityPath
    ) {
        this(properties, floorShape, wallShape, ceilingShape, floorShape, wallShape, ceilingShape, blockEntityPath);
    }

    public ColorMountedFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            VoxelShape ceilingShape,
            VoxelShape floorCollisionShape,
            VoxelShape wallCollisionShape,
            VoxelShape ceilingCollisionShape,
            String blockEntityPath
    ) {
        super(
                properties,
                floorShape,
                wallShape,
                ceilingShape,
                floorCollisionShape,
                wallCollisionShape,
                ceilingCollisionShape,
                blockEntityPath
        );
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR)
                .setValue(ColorFacingEntityBlock.COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ColorFacingEntityBlock.COLOR);
    }
}
