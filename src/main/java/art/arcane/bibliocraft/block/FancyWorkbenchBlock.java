package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.BookcaseBlockEntity;
import art.arcane.bibliocraft.blockentity.FancyWorkbenchBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.FancyWorkbenchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public final class FancyWorkbenchBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    public FancyWorkbenchBlock(Properties properties) {
        super(properties, box(0, 0, 0, 16, 16, 16), "fancy_workbench");
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(front(state)).getCounterClockWise());
    }

    public static Direction front(BlockState state) {
        return state.getValue(FACING).getClockWise();
    }

    public static @Nullable BookcaseBlockEntity sideBookcase(FancyWorkbenchBlockEntity bench, Player player, boolean left) {
        Level level = bench.getLevel();
        if (level == null || !BiblioConfig.COMMON.enableBookcase.get()) {
            return null;
        }
        Direction front = front(bench.getBlockState());
        BlockPos side = bench.getBlockPos().relative(left ? front.getClockWise() : front.getCounterClockWise());
        return level.hasChunkAt(side) && level.getBlockEntity(side) instanceof BookcaseBlockEntity bookcase
                && bookcase.canAccess(player) ? bookcase : null;
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enableFancyWorkbench.get()
                || !(level.getBlockEntity(pos) instanceof FancyWorkbenchBlockEntity bench)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            int slot = storageSlot(state, pos, hit);
            if (slot > 0) {
                var held = player.getItemInHand(hand);
                if (player.isShiftKeyDown() && held.isEmpty()) {
                    ejectStack(level, pos, front(state), bench, slot);
                    return InteractionResult.CONSUME;
                }
                if (bench.canPlaceItem(slot, held) && insertStack(bench, slot, held)) {
                    return InteractionResult.CONSUME;
                }
            }
            var left = sideBookcase(bench, player, true);
            var right = sideBookcase(bench, player, false);
            net.minecraftforge.network.NetworkHooks.openScreen(serverPlayer, new SimpleMenuProvider(
                    (id, inventory, viewer) -> new FancyWorkbenchMenu(id, inventory, bench, left, right), getName()),
                    buffer -> {
                        buffer.writeBoolean(left != null);
                        buffer.writeBoolean(right != null);
                    });
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static int storageSlot(BlockState state, BlockPos pos, BlockHitResult hit) {
        Direction front = front(state);
        double height = hit.getLocation().y - pos.getY();
        if (hit.getDirection() != front || height <= 0.22 || height >= 0.74) {
            return -1;
        }
        Direction right = front.getCounterClockWise();
        double offset = (hit.getLocation().x - pos.getX() - 0.5) * right.getStepX()
                + (hit.getLocation().z - pos.getZ() - 0.5) * right.getStepZ();
        return 1 + Mth.clamp(Mth.floor((offset + 0.5) * FancyWorkbenchBlockEntity.STORAGE_SLOTS), 0, 7);
    }
}
