package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.FancySignBlockEntity;
import art.arcane.bibliocraft.menu.FancySignMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FancySignBlock extends SignDisplayBlock implements DrillAdjustable {
    public static final EnumProperty<AttachFace> FACE = BlockStateProperties.ATTACH_FACE;
    public static final IntegerProperty SHIFT = IntegerProperty.create("shift", 0, 2);
    private static final double[] DEPTHS = {0, 0.43, 0.93};
    private final List<Map<Direction, VoxelShape>> shapes;

    public FancySignBlock(Properties properties) {
        super(properties, Shapes.empty(), "fancy_sign");
        registerDefaultState(defaultBlockState().setValue(FACE, AttachFace.FLOOR).setValue(SHIFT, 0));
        List<Map<Direction, VoxelShape>> models = new ArrayList<>();
        for (AttachFace face : AttachFace.values()) {
            for (double depth : DEPTHS) {
                double outerEdge = Math.min(1, depth + 0.07);
                VoxelShape board = Shapes.box(depth, 0.2, 0, outerEdge, 0.8, 1);
                VoxelShape feet = switch (face) {
                    case FLOOR -> Shapes.box(depth, 0, 0.2293, outerEdge, 0.2035, 0.7707);
                    case CEILING -> Shapes.box(depth, 0.7965, 0.2293, outerEdge, 1, 0.7707);
                    case WALL -> Shapes.empty();
                };
                models.add(horizontalShapes(Shapes.or(board, feet)));
            }
        }
        shapes = List.copyOf(models);
    }

    public static double depth(BlockState state) {
        return DEPTHS[state.getValue(SHIFT)];
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACE, SHIFT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        AttachFace face;
        if (player == null) {
            face = switch (context.getClickedFace()) {
                case UP -> AttachFace.FLOOR;
                case DOWN -> AttachFace.CEILING;
                default -> AttachFace.WALL;
            };
        } else {
            int pitch = Mth.floor(player.getXRot() * 3.0F / 180.0F + 0.5D);
            face = switch (pitch) {
                case -1 -> AttachFace.CEILING;
                case 0 -> AttachFace.WALL;
                default -> AttachFace.FLOOR;
            };
        }
        return super.getStateForPlacement(context).setValue(FACE, face);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes.get(state.getValue(FACE).ordinal() * DEPTHS.length + state.getValue(SHIFT))
                .get(state.getValue(FACING));
    }

    @Override
    public BlockState adjustWithDrill(BlockState state, boolean sneaking) {
        return sneaking ? DrillAdjustable.super.adjustWithDrill(state, true) : state.cycle(SHIFT);
    }

    @Override
    protected MenuConstructor menuAt(Level level, BlockPos pos) {
        FancySignBlockEntity sign = (FancySignBlockEntity) level.getBlockEntity(pos);
        return (id, inventory, player) -> sign == null ? null : new FancySignMenu(id, inventory, sign);
    }

    @Override
    protected void openMenu(ServerPlayer player, MenuProvider provider, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof FancySignBlockEntity sign) {
            NetworkHooks.openScreen(player, provider, sign.getLayout()::write);
        }
    }

    @Override
    protected boolean dropsInventorySeparately() {
        return false;
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof FancySignBlockEntity sign) {
            drops.stream().filter(stack -> stack.is(asItem())).findFirst().ifPresent(sign::packInto);
        }
        return drops;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof FancySignBlockEntity sign
                && sign.hasContents()) {
            ItemStack stack = sign.getMaterial().applyTo(new ItemStack(this));
            popResource(level, pos, sign.packInto(stack));
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
