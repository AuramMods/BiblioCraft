package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.PaintingPressBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.PaintingPressMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import org.jetbrains.annotations.Nullable;

public final class PaintingPressBlock extends InventoryFurnitureBlock {
    public PaintingPressBlock(Properties properties) {
        super(properties, Shapes.or(Shapes.box(0, 0, 0, 1, 0.41, 1),
                Shapes.box(0, 0.41, 0.02, 0.9, 0.98, 0.98)), "painting_press");
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    public static boolean isHandle(BlockState state, BlockHitResult hit) {
        Vec3 local = northHit(state, hit);
        return hit.getDirection() == state.getValue(FACING).getClockWise() && local.y > 0.75
                || hit.getDirection() == Direction.UP && local.x > 0.82;
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enablePainting.get()
                || !(level.getBlockEntity(pos) instanceof PaintingPressBlockEntity press)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            var held = player.getMainHandItem();
            boolean occupied = !press.getItem(PaintingPressBlockEntity.CANVAS).isEmpty();
            if (!occupied && press.canPlaceItem(PaintingPressBlockEntity.CANVAS, held)) {
                press.setItem(PaintingPressBlockEntity.CANVAS, held.split(1));
            } else if (occupied && player.isShiftKeyDown()) {
                ejectStack(level, Vec3.atBottomCenterOf(pos.above()).add(0, 0.08, 0), press,
                        PaintingPressBlockEntity.CANVAS);
            } else if (occupied && isHandle(state, hit)) {
                press.startTransfer();
            } else if (player instanceof ServerPlayer serverPlayer) {
                PaintingPressMenu.open(serverPlayer, press, getName());
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.PAINTING_PRESS.get()) {
            return null;
        }
        return (world, pos, current, entity) -> ((PaintingPressBlockEntity) entity).tick(server);
    }
}
