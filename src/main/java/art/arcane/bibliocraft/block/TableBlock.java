package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.BiblioCraft;
import art.arcane.bibliocraft.blockentity.TableBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.menu.TableMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public final class TableBlock extends InventoryFurnitureBlock {
    private static final List<Direction> SIDES = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH, Direction.EAST, BlockStateProperties.EAST,
            Direction.SOUTH, BlockStateProperties.SOUTH, Direction.WEST, BlockStateProperties.WEST);
    private static final TagKey<Item> ROTATION_TOOLS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(BiblioCraft.MODID, "table_rotation_tools"));
    private static final VoxelShape TOP = Shapes.box(0, 0.88, 0, 1, 1, 1);
    private static final VoxelShape CARPET = Shapes.box(0, 0, 0, 1, 0.063, 1);
    private static final VoxelShape CENTER_SUPPORT = Shapes.or(
            Shapes.box(0.2586, 0.7283, 0.2586, 0.7414, 0.8804, 0.7414),
            Shapes.box(0.4, 0.0667, 0.4, 0.6, 0.7283, 0.6),
            Shapes.box(0.07, 0, 0.4, 0.93, 0.0667, 0.6),
            Shapes.box(0.4, 0, 0.07, 0.6, 0.0667, 0.93));
    private static final List<VoxelShape> CORNER_LEGS = List.of(
            Shapes.box(0.717, 0, 0.078, 0.922, 0.88, 0.282),
            Shapes.box(0.718, 0, 0.717, 0.922, 0.88, 0.922),
            Shapes.box(0.078, 0, 0.718, 0.283, 0.88, 0.922),
            Shapes.box(0.078, 0, 0.078, 0.282, 0.88, 0.283));
    private static final List<Map<Direction, VoxelShape>> SHAPES = createShapes();

    public TableBlock(Properties properties) {
        super(properties, TOP, "table");
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.NORTH, false)
                .setValue(BlockStateProperties.EAST, false).setValue(BlockStateProperties.SOUTH, false)
                .setValue(BlockStateProperties.WEST, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return findConnections(super.getStateForPlacement(context), context.getLevel(), context.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor, LevelAccessor level,
                                  BlockPos pos, BlockPos neighborPos) {
        BooleanProperty connection = CONNECTIONS.get(direction);
        return connection == null ? super.updateShape(state, direction, neighbor, level, pos, neighborPos)
                : state.setValue(connection, neighbor.is(this));
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockState connected = findConnections(state, level, pos);
        if (connected != state) {
            level.setBlock(pos, connected, Block.UPDATE_ALL);
        }
    }

    private BlockState findConnections(BlockState state, Level level, BlockPos pos) {
        for (Direction side : SIDES) {
            BlockPos neighbor = pos.relative(side);
            if (level.hasChunkAt(neighbor)) {
                state = state.setValue(CONNECTIONS.get(side), level.getBlockState(neighbor).is(this));
            }
        }
        return state;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return remapConnections(state, super.rotate(state, rotation), rotation::rotate);
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return remapConnections(state, super.mirror(state, mirror), mirror::mirror);
    }

    private static BlockState remapConnections(BlockState original, BlockState transformed, UnaryOperator<Direction> transform) {
        for (Direction side : SIDES) {
            transformed = transformed.setValue(CONNECTIONS.get(transform.apply(side)), original.getValue(CONNECTIONS.get(side)));
        }
        return transformed;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = SHAPES.get(localConnections(state)).get(state.getValue(FACING));
        return level.getBlockEntity(pos) instanceof TableBlockEntity table
                && TableBlockEntity.carpetColor(table.getItem(TableBlockEntity.CARPET)) != null
                ? Shapes.or(shape, CARPET) : shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    private static int localConnections(BlockState state) {
        int turns = (state.getValue(FACING).get2DDataValue() + 2) & 3;
        int mask = 0;
        for (int side = 0; side < SIDES.size(); side++) {
            if (state.getValue(CONNECTIONS.get(SIDES.get((side + turns) & 3)))) {
                mask |= 1 << side;
            }
        }
        return mask;
    }

    private static List<Map<Direction, VoxelShape>> createShapes() {
        List<Map<Direction, VoxelShape>> shapes = new ArrayList<>(16);
        shapes.add(horizontalShapes(Shapes.or(TOP, CENTER_SUPPORT)));
        for (int neighbors = 1; neighbors < 16; neighbors++) {
            VoxelShape shape = TOP;
            for (int corner = 0; corner < CORNER_LEGS.size(); corner++) {
                if ((neighbors & cornerSides(corner)) == 0) {
                    shape = Shapes.or(shape, CORNER_LEGS.get(corner));
                }
            }
            shapes.add(horizontalShapes(shape));
        }
        return shapes;
    }

    private static int cornerSides(int corner) {
        return (1 << corner) | (1 << ((corner + 1) & 3));
    }

    public static int visibleParts(BlockState state, boolean cloth, boolean carpet) {
        int neighbors = localConnections(state);
        // Bit indexes match the material tint indexes in table.mtl. Corners run NE, SE, SW, NW.
        int visible = neighbors == 0 ? 1 : 0;
        for (int corner = 0; corner < CORNER_LEGS.size(); corner++) {
            boolean outerCorner = (neighbors & cornerSides(corner)) == 0;
            visible |= 1 << ((outerCorner ? 5 : 9) + corner);
            if (neighbors != 0 && outerCorner) {
                visible |= 1 << (1 + corner);
            }
            if (cloth) {
                visible |= 1 << ((outerCorner ? 13 : 17) + corner);
                if (!outerCorner && (neighbors & (1 << ((corner + 1) & 3))) == 0) {
                    visible |= 1 << (21 + 2 * corner);
                }
                if (!outerCorner && (neighbors & (1 << corner)) == 0) {
                    visible |= 1 << (22 + 2 * corner);
                }
            }
        }
        return carpet ? visible | (1 << 29) : visible;
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableTable.get()
                || !(level.getBlockEntity(pos) instanceof TableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack held = player.getMainHandItem();
            Direction side = hit.getDirection();
            if (side == Direction.UP && player.isShiftKeyDown()) {
                player.openMenu(new SimpleMenuProvider((id, inventory, viewer) -> new TableMenu(id, inventory, table), getName()));
            } else if (TableBlockEntity.carpetColor(held) != null) {
                int slot = side == Direction.UP ? TableBlockEntity.CLOTH : TableBlockEntity.CARPET;
                if (table.getItem(slot).isEmpty()) {
                    table.setItem(slot, held.split(1));
                }
            } else if (side == Direction.UP) {
                if (table.getItem(TableBlockEntity.DISPLAY).isEmpty()) {
                    insertStack(table, TableBlockEntity.DISPLAY, held);
                } else {
                    ejectTableStack(level, pos, table, TableBlockEntity.DISPLAY);
                }
            } else if (side.getAxis().isHorizontal() && held.is(ROTATION_TOOLS)
                    && BiblioFeatureToggles.isItemEnabled(ForgeRegistries.ITEMS.getKey(held.getItem()))) {
                table.rotateDisplay(side);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult adjustWithDrill(UseOnContext context, boolean sneaking) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        if ((!sneaking && side == Direction.DOWN) || !(level.getBlockEntity(pos) instanceof TableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            if (sneaking) {
                ejectTableStack(level, pos, table, TableBlockEntity.CARPET);
            } else if (side == Direction.UP) {
                ejectTableStack(level, pos, table, TableBlockEntity.CLOTH);
            } else {
                table.rotateDisplay(side);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void ejectTableStack(Level level, BlockPos pos, TableBlockEntity table, int slot) {
        ejectStack(level, Vec3.atBottomCenterOf(pos).add(0, 1.18, 0), table, slot);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.TABLE.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> ((TableBlockEntity) entity).tickMap(server);
    }
}
