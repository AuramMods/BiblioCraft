package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.ClipboardBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class ClipboardBlock extends InventoryFurnitureBlock {
    public ClipboardBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "clipboard");
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClipboardBlockEntity(pos, state);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction front = context.getClickedFace();
        if (!BiblioConfig.COMMON.enableClipboard.get()) {
            return null;
        }
        return front.getAxis().isHorizontal() ? defaultBlockState().setValue(FACING, front.getCounterClockWise())
                : super.getStateForPlacement(context);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide && stack.is(ModItems.BIBLIO_CLIPBOARD.get())
                && level.getBlockEntity(pos) instanceof ClipboardBlockEntity clipboard) {
            clipboard.setClipboard(stack);
        }
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (!BiblioConfig.COMMON.enableClipboard.get() || hand != InteractionHand.MAIN_HAND
                || !(level.getBlockEntity(pos) instanceof ClipboardBlockEntity clipboard)) {
            return InteractionResult.PASS;
        }
        Direction front = state.getValue(FACING).getClockWise();
        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                Vec3 drop = Vec3.atCenterOf(pos).relative(front, 0.15);
                var entity = new ItemEntity(level, drop.x, drop.y, drop.z, pickup(clipboard));
                entity.setDefaultPickUpDelay();
                if (level.addFreshEntity(entity)) {
                    level.removeBlock(pos, false);
                }
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() != front) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            Vec3 point = northHit(state, hit);
            double x = 1 - point.z;
            double y = 1 - point.y;
            int task = (int) Math.floor((y - 0.21) / 0.066);
            if (x >= 0.202 && x <= 0.277 && task >= 0 && task < 9) {
                clipboard.toggleTask(task);
            } else if (y > 0.83 && y < 0.868) {
                if (x > 0.296 && x < 0.387) {
                    clipboard.turnPage(-1);
                } else if (x > 0.599 && x < 0.843) {
                    clipboard.turnPage(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private ItemStack pickup(ClipboardBlockEntity board) {
        return board.clipboard().isEmpty() ? new ItemStack(asItem()) : board.clipboard().copy();
    }

    @Override
    public ItemStack getCloneItemStack(BlockGetter level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof ClipboardBlockEntity board
                ? pickup(board) : super.getCloneItemStack(level, pos, state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        var drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof ClipboardBlockEntity board
                && !board.clipboard().isEmpty()) {
            return drops.stream().map(stack -> stack.is(asItem()) ? pickup(board) : stack).toList();
        }
        return drops;
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && level.getBlockEntity(pos) instanceof ClipboardBlockEntity board
                && !board.clipboard().isEmpty()) {
            popResource(level, pos, pickup(board));
        }
        super.playerWillDestroy(level, pos, state, player);
    }
}
