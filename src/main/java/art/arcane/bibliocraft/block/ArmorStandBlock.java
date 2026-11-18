package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.ArmorStandBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.ArmorStandMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class ArmorStandBlock extends InventoryFurnitureBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape UPRIGHT = Shapes.box(0.3, 0, 0, 0.7, 1, 1);
    private static final VoxelShape FOOT = Shapes.box(0, 0, 0, 1, 0.075, 1);
    private static final Map<Direction, VoxelShape> UPPER_SHAPES = horizontalShapes(UPRIGHT);

    public ArmorStandBlock(Properties properties) {
        super(properties, Shapes.or(FOOT, UPRIGHT), "armor_stand");
        registerDefaultState(defaultBlockState().setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(HALF);
    }

    public static BlockPos basePos(BlockState state, BlockPos pos) {
        return state.getBlock() instanceof ArmorStandBlock && state.getValue(HALF) == DoubleBlockHalf.UPPER
                ? pos.below() : pos;
    }

    public static Direction front(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? super.newBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos upper = context.getClickedPos().above();
        Direction front = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, front.getCounterClockWise());
        return hasRoom(level, upper) && level.isUnobstructed(state.setValue(HALF, DoubleBlockHalf.UPPER), upper,
                context.getPlayer() == null ? CollisionContext.empty() : CollisionContext.of(context.getPlayer())) ? state : null;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? UPPER_SHAPES.get(state.getValue(FACING))
                : super.getShape(state, level, pos, context);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            addUpperHalf(level, pos, state);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && direction == Direction.DOWN
                && !isHalf(neighbor, DoubleBlockHalf.LOWER)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER && direction == Direction.UP && neighbor.isAir()) {
            level.scheduleTick(pos, this, 1);
        }
        return super.updateShape(state, direction, neighbor, level, pos, neighborPos);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            addUpperHalf(level, pos, state);
        }
    }

    private static boolean hasRoom(Level level, BlockPos upper) {
        return !level.isOutsideBuildHeight(upper) && level.getBlockState(upper).isAir();
    }

    private void addUpperHalf(Level level, BlockPos pos, BlockState state) {
        BlockPos upper = pos.above();
        if (hasRoom(level, upper)) {
            level.setBlock(upper, state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    private boolean isHalf(BlockState state, DoubleBlockHalf half) {
        return state.is(this) && state.getValue(HALF) == half;
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableArmorstand.get()
                || !(FurnitureBlockEntity.findAt(level, pos) instanceof ArmorStandBlockEntity stand)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            BlockPos base = stand.getBlockPos();
            int slot = 3 - Mth.clamp(Mth.floor((hit.getLocation().y - base.getY()) * 2), 0, 3);
            ItemStack held = player.getMainHandItem();
            if (player.isShiftKeyDown()) {
                if (level.hasNeighborSignal(base) || level.hasNeighborSignal(base.above())) {
                    for (int index = 0; index < stand.getContainerSize(); index++) {
                        stand.swapWornArmor(player, index);
                    }
                } else {
                    stand.swapWornArmor(player, slot);
                }
            } else if (stand.getItem(slot).isEmpty() && ArmorStandBlockEntity.accepts(slot, held)) {
                stand.setItem(slot, held.split(1));
            } else {
                player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new ArmorStandMenu(id, inventory, stand), getName()));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public void rotateStand(Level level, BlockPos pos, BlockState state) {
        BlockPos base = basePos(state, pos);
        BlockState lower = level.getBlockState(base);
        if (!isHalf(lower, DoubleBlockHalf.LOWER)) {
            return;
        }
        BlockState rotated = lower.rotate(Rotation.CLOCKWISE_90);
        level.setBlock(base, rotated, Block.UPDATE_ALL);
        if (isHalf(level.getBlockState(base.above()), DoubleBlockHalf.UPPER)) {
            level.setBlock(base.above(), rotated.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER
                && isHalf(level.getBlockState(pos.below()), DoubleBlockHalf.LOWER)) {
            level.removeBlock(pos.below(), false);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        super.onRemove(state, level, pos, replacement, moving);
        if (level.isClientSide || replacement.is(this)) {
            return;
        }
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            if (isHalf(level.getBlockState(pos.below()), DoubleBlockHalf.LOWER)) {
                level.destroyBlock(pos.below(), true);
            }
        } else if (isHalf(level.getBlockState(pos.above()), DoubleBlockHalf.UPPER)) {
            level.removeBlock(pos.above(), moving);
        }
    }
}
