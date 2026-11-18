package art.arcane.bibliocraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public class FloorWallFacingEntityBlock extends InventoryFurnitureBlock {
    public static final EnumProperty<AttachFace> FACE =
            EnumProperty.create("face", AttachFace.class, AttachFace.FLOOR, AttachFace.WALL);

    private final Map<Direction, VoxelShape> wallShapesByFacing;

    public FloorWallFacingEntityBlock(
            Properties properties,
            VoxelShape floorShape,
            VoxelShape wallShape,
            String blockEntityPath
    ) {
        super(properties, floorShape, blockEntityPath);
        this.wallShapesByFacing = horizontalShapes(wallShape);
        registerDefaultState(defaultBlockState().setValue(FACE, AttachFace.FLOOR));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context).setValue(FACE, getPlacementFace(context));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACE) == AttachFace.WALL ? wallShapesByFacing.get(state.getValue(FACING))
                : super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
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
