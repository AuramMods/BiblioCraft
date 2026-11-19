package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.DeskBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.DeskBookMenu;
import art.arcane.bibliocraft.menu.RecipeBookMenu;
import art.arcane.bibliocraft.registry.ModItems;
import art.arcane.bibliocraft.menu.DeskMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import art.arcane.bibliocraft.item.DrillSelection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public final class DeskBlock extends InventoryFurnitureBlock {
    public static final EnumProperty<DeskSection> SECTION = EnumProperty.create("section", DeskSection.class);
    private static final VoxelShape TOP = Shapes.box(0, 0.8734, 0, 1, 1, 1);
    private static final VoxelShape LEFT_LEG = Shapes.box(0.0311, 0, 0.8115, 0.9689, 0.8753, 0.9678);
    private static final VoxelShape RIGHT_LEG = Shapes.box(0.0311, 0, 0.032, 0.9689, 0.8753, 0.1882);
    private static final Map<DeskSection, Map<Direction, VoxelShape>> SHAPES = Map.of(
            DeskSection.SINGLE, horizontalShapes(Shapes.or(TOP, LEFT_LEG, RIGHT_LEG,
                    Shapes.box(0.0946, 0.5615, 0.156, 0.9063, 0.8741, 0.8437))),
            DeskSection.LEFT, horizontalShapes(Shapes.or(TOP, LEFT_LEG,
                    Shapes.box(0.0946, 0.1874, 0, 0.1571, 0.8751, 0.8432),
                    Shapes.box(0.1561, 0.2506, 0.3127, 0.9063, 0.8758, 0.8441))),
            DeskSection.RIGHT, horizontalShapes(Shapes.or(TOP, RIGHT_LEG,
                    Shapes.box(0.0946, 0.1874, 0.1559, 0.1571, 0.8751, 1),
                    Shapes.box(0.1561, 0.2506, 0.1558, 0.9063, 0.8758, 0.6872))),
            DeskSection.CENTER, horizontalShapes(Shapes.or(TOP, Shapes.box(0.0946, 0.1874, 0, 0.1571, 0.8751, 1))));

    public DeskBlock(Properties properties) {
        super(properties, TOP, "desk");
        registerDefaultState(defaultBlockState().setValue(SECTION, DeskSection.SINGLE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(SECTION);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction front = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, front.getCounterClockWise());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        BlockState rotated = super.mirror(state, mirror);
        return mirror == Mirror.NONE ? rotated : rotated.setValue(SECTION, state.getValue(SECTION).mirrored());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES.get(state.getValue(SECTION)).get(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableWritingdesk.get()
                || !(level.getBlockEntity(pos) instanceof DeskBlockEntity desk)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            interact(state, level, pos, player, hit, desk);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private void interact(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit, DeskBlockEntity desk) {
        int area = clickedArea(state, pos, hit);
        Direction front = state.getValue(FACING).getClockWise();
        if (player.isShiftKeyDown()) {
            int slot = area == DeskBlockEntity.LEFT_BOOKS || area == DeskBlockEntity.RIGHT_BOOKS ? desk.topOfStack(area) : area;
            if (slot >= 0 && ejectStack(level, pos, front, desk, slot)) {
                return;
            }
        } else {
            ItemStack held = player.getMainHandItem();
            if (area == DeskBlockEntity.CARPET && DeskBlockEntity.isCarpet(held) && desk.getItem(area).isEmpty()) {
                desk.setItem(area, held.split(1));
                return;
            }
            if (area >= 0 && area != DeskBlockEntity.CARPET && DeskBlockEntity.acceptsBook(held)) {
                int slot = area == DeskBlockEntity.OPEN_BOOK ? area : desk.firstEmptyInStack(area);
                if (slot >= 0 && insertStack(desk, slot, held)) {
                    return;
                }
            }
        }
        ItemStack book = desk.getItem(DeskBlockEntity.OPEN_BOOK);
        if (area == DeskBlockEntity.OPEN_BOOK && book.is(ModItems.RECIPE_BOOK.get())
                && BiblioConfig.COMMON.enableFancyWorkbench.get() && player instanceof ServerPlayer serverPlayer) {
            RecipeBookMenu.open(serverPlayer, desk, null, book);
        } else if (area == DeskBlockEntity.OPEN_BOOK && (book.is(Items.WRITABLE_BOOK) || book.is(Items.WRITTEN_BOOK))
                && player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                    new SimpleMenuProvider((id, inventory, viewer) -> new DeskBookMenu(id, desk), book.getHoverName()),
                    buffer -> {
                        buffer.writeItem(book);
                        buffer.writeVarInt(desk.getPage());
                    });
        } else {
            player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new DeskMenu(id, inventory, desk), getName()));
        }
    }

    private static int clickedArea(BlockState state, BlockPos pos, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) {
            return DeskBlockEntity.CARPET;
        }
        float inverseYaw = (state.getValue(FACING).toYRot() - 180) * Mth.DEG_TO_RAD;
        Vec3 local = hit.getLocation().subtract(Vec3.atCenterOf(pos)).yRot(inverseYaw);
        if (local.x < 0) {
            return local.z > 0 ? DeskBlockEntity.LEFT_BOOKS : DeskBlockEntity.RIGHT_BOOKS;
        }
        return Math.abs(local.z) < 0.3 ? DeskBlockEntity.OPEN_BOOK : -1;
    }

    public InteractionResult connectWithDrill(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockPos otherPos = DrillSelection.select(context, "DeskSelection", "message.bibliocraft.desk.selected");
        if (otherPos == null) {
            return InteractionResult.CONSUME;
        }
        BlockState state = level.getBlockState(pos);
        Direction right = state.getValue(FACING);
        boolean otherOnRight = otherPos.equals(pos.relative(right));
        if ((!otherOnRight && !otherPos.equals(pos.relative(right.getOpposite())))
                || !(level.getBlockEntity(otherPos) instanceof DeskBlockEntity other) || !other.canAccess(player)
                || other.getBlockState().getValue(FACING) != right) {
            player.displayClientMessage(Component.translatable("message.bibliocraft.desk.invalid"), true);
            return InteractionResult.CONSUME;
        }
        BlockState otherState = other.getBlockState();
        DeskSection current = state.getValue(SECTION);
        DeskSection adjacent = otherState.getValue(SECTION);
        boolean join = otherOnRight ? !(current.joinsRight() && adjacent.joinsLeft())
                : !(current.joinsLeft() && adjacent.joinsRight());
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pos, state.setValue(SECTION, otherOnRight ? current.withRight(join) : current.withLeft(join)), flags);
        level.setBlock(otherPos, otherState.setValue(SECTION, otherOnRight ? adjacent.withLeft(join) : adjacent.withRight(join)), flags);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction side, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        Direction right = state.getValue(FACING);
        DeskSection section = state.getValue(SECTION);
        boolean matches = neighbor.is(this) && neighbor.getValue(FACING) == right;
        if (side == right && section.joinsRight() && (!matches || !neighbor.getValue(SECTION).joinsLeft())) {
            return state.setValue(SECTION, section.withRight(false));
        }
        if (side == right.getOpposite() && section.joinsLeft() && (!matches || !neighbor.getValue(SECTION).joinsRight())) {
            return state.setValue(SECTION, section.withLeft(false));
        }
        return super.updateShape(state, side, neighbor, level, pos, neighborPos);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.DESK.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> ((DeskBlockEntity) entity).tickMaps(server);
    }
}
