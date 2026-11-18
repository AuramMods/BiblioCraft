package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.TypewriterBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.ColoredBlockItem;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import art.arcane.bibliocraft.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class TypewriterBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    private final Map<Direction, VoxelShape> bodyOutline;
    private final Map<Direction, VoxelShape> paperOutline;

    public TypewriterBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "typewriter");
        registerDefaultState(defaultBlockState().setValue(ColorFacingEntityBlock.COLOR, DyeColor.WHITE));
        VoxelShape carriage = Shapes.box(0.5277, 0.156, 0.0473, 0.6711, 0.2835, 0.9127);
        bodyOutline = horizontalShapes(Shapes.or(shape, carriage));
        paperOutline = horizontalShapes(Shapes.or(shape, carriage,
                Shapes.box(0.5707, 0.1851, 0.2133, 0.7053, 0.4835, 0.7467)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ColorFacingEntityBlock.COLOR);
    }

    public static Direction front(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction opening = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, opening.getClockWise());
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableTypewriter.get()
                || !(level.getBlockEntity(pos) instanceof TypewriterBlockEntity typewriter)) {
            return InteractionResult.PASS;
        }
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                ejectStack(level, pos, front(state), typewriter, TypewriterBlockEntity.PAPER);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        InteractionResult dye = tryApplyDye(state, level, pos, player, hand);
        if (dye.consumesAction()) {
            return dye;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (held.is(Items.PAPER)) {
                if (insertStack(typewriter, TypewriterBlockEntity.PAPER, held)) {
                    typewriter.playSound(ModSounds.ADDPAPER.get());
                }
            } else if (held.isEmpty()) {
                if (ejectStack(level, pos, front(state), typewriter, TypewriterBlockEntity.BOOK)) {
                    typewriter.playSound(ModSounds.REMOVEBOOK.get());
                } else if (hitKeyboard(state, hit)) {
                    typewriter.typeByHand(player);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean hitKeyboard(BlockState state, BlockHitResult hit) {
        Direction front = front(state);
        return hit.getDirection() == front || hit.getDirection() == Direction.UP
                && hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()))
                        .dot(Vec3.atLowerCornerOf(front.getNormal())) >= 0.1;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean paper = level.getBlockEntity(pos) instanceof TypewriterBlockEntity typewriter && typewriter.hasPaper();
        return (paper ? paperOutline : bodyOutline).get(state.getValue(FACING));
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return ColoredBlockItem.applyColor(super.getCloneItemStack(level, pos, state),
                state.getValue(ColorFacingEntityBlock.COLOR));
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.TYPEWRITER.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> ((TypewriterBlockEntity) entity).tick();
    }
}
