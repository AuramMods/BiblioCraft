package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class ColorMountedFacingEntityBlock extends MountedFacingEntityBlock implements DrillAdjustable {
    private final boolean candle;

    public ColorMountedFacingEntityBlock(Properties properties, VoxelShape floorShape, VoxelShape wallShape,
                                         VoxelShape ceilingShape, boolean candle) {
        super(properties, floorShape, wallShape, ceilingShape, "bibliolight");
        this.candle = candle;
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(FACE, AttachFace.FLOOR)
                .setValue(ColorFacingEntityBlock.COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ColorFacingEntityBlock.COLOR);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (candle) {
            level.addParticle(ParticleTypes.SMALL_FLAME, pos.getX() + 0.5, pos.getY() + 0.3,
                    pos.getZ() + 0.5, 0, 0.001, 0);
        }
    }

    @Override
    public BlockState adjustWithDrill(BlockState state, boolean sneaking) {
        if (sneaking) {
            return DrillAdjustable.super.adjustWithDrill(state, true);
        }
        AttachFace next = switch (state.getValue(FACE)) {
            case FLOOR -> AttachFace.WALL;
            case WALL -> AttachFace.CEILING;
            case CEILING -> AttachFace.FLOOR;
        };
        return state.setValue(FACE, next);
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ColoredBlockItem.applyColor(super.getCloneItemStack(level, pos, state),
                state.getValue(ColorFacingEntityBlock.COLOR));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        if (hand == InteractionHand.MAIN_HAND && level.getBlockEntity(pos) instanceof FurnitureBlockEntity furniture
                && !furniture.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        return super.use(state, level, pos, player, hand, hit);
    }

    @Override
    public float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (!player.isCreative() && level.getBlockEntity(pos) instanceof FurnitureBlockEntity furniture && furniture.isLocked()) {
            return 0;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }
}
