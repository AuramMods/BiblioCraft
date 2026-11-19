package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.PaintingFrameBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.item.DrillSelection;
import art.arcane.bibliocraft.menu.PaintingFrameMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class PaintingFrameBlock extends InventoryFurnitureBlock {
    private final Style style;

    public PaintingFrameBlock(Properties properties, Style style, String path) {
        super(properties, Shapes.box(0, 0, 0, style == Style.BORDERLESS ? 0.038 : 0.08, 1, 1), path);
        this.style = style;
    }

    public Style style() {
        return style;
    }

    public static Direction side(BlockState state, int side) {
        // The OBJ opens east in its north state. The viewer's right there is north.
        return switch (side) {
            case 0 -> Direction.UP;
            case 1 -> state.getValue(FACING);
            case 2 -> Direction.DOWN;
            default -> state.getValue(FACING).getOpposite();
        };
    }

    public static int visibleParts(BlockGetter world, BlockPos pos, BlockState state) {
        PaintingFrameBlockEntity frame = world.getBlockEntity(pos) instanceof PaintingFrameBlockEntity found ? found : null;
        Style style = ((PaintingFrameBlock) state.getBlock()).style;
        if (frame != null && frame.settings().hidden()) {
            return 0;
        }
        int links = frame == null ? 0 : frame.activeLinks(world);
        int visible = 1 | 1 << (style == Style.BORDERLESS || links != 0 ? 2 : 1);
        if (style == Style.BORDERLESS) {
            return visible;
        }
        // Four side groups each have double-mitered, previous-side, next-side, and straight meshes.
        for (int edge = 0; edge < 4; edge++) {
            int previous = (edge + 3) & 3;
            int next = (edge + 1) & 3;
            boolean currentLinked = (links & 1 << edge) != 0;
            boolean nextLinked = (links & 1 << next) != 0;
            if (!currentLinked) {
                int profile = (nextLinked ? 2 : 0) | ((links >> previous) & 1);
                visible |= 1 << (3 + edge * 4 + profile);
            } else if (frame != null && nextLinked) {
                var alongEdge = frame.linkedFrame(world, edge);
                var alongNext = frame.linkedFrame(world, next);
                if (alongEdge == null || alongNext == null || alongEdge.linkedFrame(world, next) == null
                        || alongNext.linkedFrame(world, edge) == null) {
                    visible |= 1 << (19 + edge);
                }
            }
        }
        return visible;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction front = context.getClickedFace();
        if (!front.getAxis().isHorizontal()) {
            front = context.getHorizontalDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, front.getCounterClockWise());
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PaintingFrameBlockEntity frame) {
            frame.connectBorderless(placer instanceof Player player ? player : null);
        }
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return level.getBlockEntity(pos) instanceof PaintingFrameBlockEntity frame && frame.settings().hidden()
                ? Shapes.empty() : super.getCollisionShape(state, level, pos, context);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!level.isClientSide && !state.is(replacement.getBlock())
                && level.getBlockEntity(pos) instanceof PaintingFrameBlockEntity frame) {
            frame.detach();
        }
        super.onRemove(state, level, pos, replacement, moving);
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enablePainting.get()
                || !(level.getBlockEntity(pos) instanceof PaintingFrameBlockEntity frame)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            ItemStack held = player.getMainHandItem();
            boolean occupied = !frame.getItem(PaintingFrameBlockEntity.CANVAS).isEmpty();
            if (!occupied && PaintingFrameBlockEntity.accepts(held)) {
                frame.setItem(PaintingFrameBlockEntity.CANVAS, held.split(1));
            } else if (occupied && player.isShiftKeyDown()) {
                ejectStack(level, Vec3.atCenterOf(pos).relative(state.getValue(FACING).getClockWise(), 0.1),
                        frame, PaintingFrameBlockEntity.CANVAS);
            } else if (player instanceof ServerPlayer serverPlayer) {
                PaintingFrameMenu.open(serverPlayer, frame, getName());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public InteractionResult adjustWithDrill(UseOnContext context, boolean sneaking) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof PaintingFrameBlockEntity frame)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (!sneaking) {
            frame.edit(frame.settings().rotate());
            return InteractionResult.CONSUME;
        }
        BlockPos first = DrillSelection.select(context, "PaintingLink", "message.bibliocraft.painting.first");
        if (first == null) {
            return InteractionResult.CONSUME;
        }
        for (int side = 0; side < 4; side++) {
            if (pos.relative(side(frame.getBlockState(), side)).equals(first) && frame.toggleLink(side, player)) {
                player.displayClientMessage(Component.translatable("message.bibliocraft.painting.link"), true);
                return InteractionResult.CONSUME;
            }
        }
        player.displayClientMessage(Component.translatable("message.bibliocraft.painting.link_failed"), true);
        return InteractionResult.CONSUME;
    }

    public enum Style {
        BORDERLESS, FLAT, SIMPLE, MIDDLE, FANCY
    }
}
