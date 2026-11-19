package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FramedChestBlockEntity;
import art.arcane.bibliocraft.blockentity.FurnitureBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.DrillSelection;
import art.arcane.bibliocraft.menu.FramedChestMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class FramedChestBlock extends InventoryFurnitureBlock {
    public static final EnumProperty<FramedChestPart> PART = EnumProperty.create("part", FramedChestPart.class);
    private static final int JOIN_UPDATE = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    private static final List<Map<Direction, VoxelShape>> SHAPES = List.of(
            horizontalShapes(Shapes.box(0.05, 0, 0.052389, 0.965, 0.868, 0.952389)),
            horizontalShapes(Shapes.box(0.05, 0, 0, 0.965, 0.868, 0.952389)),
            horizontalShapes(Shapes.box(0.05, 0, 0.052389, 0.965, 0.868, 1)));

    public FramedChestBlock(Properties properties) {
        super(properties, Shapes.empty(), "framed_chest");
        registerDefaultState(defaultBlockState().setValue(PART, FramedChestPart.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction front = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, front.getCounterClockWise());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        if (mirror == Mirror.NONE) {
            return state;
        }
        Direction front = state.getValue(FACING).getClockWise();
        return state.setValue(FACING, mirror.mirror(front).getCounterClockWise())
                .setValue(PART, state.getValue(PART).opposite());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(PART).ordinal()).get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    public static @Nullable FramedChestBlockEntity partner(BlockGetter level, BlockPos pos, BlockState state) {
        FramedChestPart part = state.getValue(PART);
        if (part == FramedChestPart.SINGLE) {
            return null;
        }
        BlockPos otherPos = pos.relative(part.partnerSide(state.getValue(FACING)));
        return level.getBlockEntity(otherPos) instanceof FramedChestBlockEntity chest && matches(state, chest.getBlockState())
                ? chest : null;
    }

    private static boolean matches(BlockState state, BlockState neighbor) {
        return neighbor.is(state.getBlock()) && neighbor.getValue(FACING) == state.getValue(FACING)
                && neighbor.getValue(PART) == state.getValue(PART).opposite();
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        FramedChestPart part = state.getValue(PART);
        if (part != FramedChestPart.SINGLE && side == part.partnerSide(state.getValue(FACING)) && !matches(state, neighbor)) {
            return state.setValue(PART, FramedChestPart.SINGLE);
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableFramedChest.get()
                || !(level.getBlockEntity(pos) instanceof FramedChestBlockEntity chest)) {
            return InteractionResult.PASS;
        }
        FramedChestBlockEntity other = partner(level, pos, state);
        if (other != null && !other.canAccess(player)) {
            return FurnitureBlockEntity.denyAccess(player);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            boolean clickedRight = other != null && state.getValue(PART) == FramedChestPart.RIGHT;
            FramedChestBlockEntity left = clickedRight ? other : chest;
            FramedChestBlockEntity right = clickedRight ? chest : other;
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, viewer) -> new FramedChestMenu(id, inventory, left, right),
                            Component.translatable(other == null ? getDescriptionId() : "menu.bibliocraft.framed_chest.double")),
                    buffer -> buffer.writeBoolean(other != null));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult adjustWithDrill(UseOnContext context, boolean sneaking) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        FramedChestBlockEntity other = partner(level, pos, state);
        if (!sneaking) {
            if (other != null && !other.canAccess(player)) {
                return FurnitureBlockEntity.denyAccess(player);
            }
            if (other != null) {
                level.setBlock(other.getBlockPos(), other.getBlockState().setValue(PART, FramedChestPart.SINGLE), JOIN_UPDATE);
            }
            level.setBlock(pos, state.setValue(PART, FramedChestPart.SINGLE).rotate(Rotation.CLOCKWISE_90), JOIN_UPDATE);
            return InteractionResult.CONSUME;
        }
        BlockPos selected = DrillSelection.select(context, "FramedChestSelection", "message.bibliocraft.framed_chest.selected");
        if (selected == null) {
            return InteractionResult.CONSUME;
        }
        Direction facing = state.getValue(FACING);
        boolean selectedIsRight = selected.equals(pos.relative(facing));
        if ((!selectedIsRight && !selected.equals(pos.relative(facing.getOpposite())))
                || !(level.getBlockEntity(selected) instanceof FramedChestBlockEntity selectedChest)
                || selectedChest.getBlockState().getValue(FACING) != facing || !selectedChest.canAccess(player)) {
            return invalidJoin(player);
        }
        BlockState selectedState = selectedChest.getBlockState();
        if (other == selectedChest) {
            level.setBlock(pos, state.setValue(PART, FramedChestPart.SINGLE), JOIN_UPDATE);
            level.setBlock(selected, selectedState.setValue(PART, FramedChestPart.SINGLE), JOIN_UPDATE);
        } else if (state.getValue(PART) == FramedChestPart.SINGLE && selectedState.getValue(PART) == FramedChestPart.SINGLE) {
            FramedChestPart part = selectedIsRight ? FramedChestPart.LEFT : FramedChestPart.RIGHT;
            level.setBlock(pos, state.setValue(PART, part), JOIN_UPDATE);
            level.setBlock(selected, selectedState.setValue(PART, part.opposite()), JOIN_UPDATE);
        } else {
            return invalidJoin(player);
        }
        DrillSelection.reportJoin(player, other != selectedChest);
        return InteractionResult.CONSUME;
    }

    private static InteractionResult invalidJoin(Player player) {
        player.displayClientMessage(Component.translatable("message.bibliocraft.framed_chest.invalid"), true);
        return InteractionResult.CONSUME;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof FramedChestBlockEntity chest) {
            chest.recheckOpeners();
        }
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide && type == ModBlockEntities.FRAMED_CHEST.get()
                ? (world, pos, current, entity) -> ((FramedChestBlockEntity) entity).tickLid() : null;
    }

    @Override
    public boolean triggerEvent(BlockState state, Level level, BlockPos pos, int event, int value) {
        return level.getBlockEntity(pos) instanceof FramedChestBlockEntity chest && chest.triggerEvent(event, value);
    }
}
