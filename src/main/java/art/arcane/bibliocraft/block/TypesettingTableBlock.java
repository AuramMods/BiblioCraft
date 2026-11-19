package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.TypesettingTableBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.TypesettingTableMenu;
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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class TypesettingTableBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    public TypesettingTableBlock(Properties properties, VoxelShape shape) {
        super(properties, shape, "typesetting_table");
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enablePrintpressTypeMachine.get()
                || !(level.getBlockEntity(pos) instanceof TypesettingTableBlockEntity table)) {
            return InteractionResult.PASS;
        }
        int slot = slotAt(state, hit);
        if (!level.isClientSide) {
            if (slot < 0) {
                if (player instanceof ServerPlayer serverPlayer) {
                    TypesettingTableMenu.open(serverPlayer, table);
                }
            } else if (slot == TypesettingTableBlockEntity.SOURCE && player.isShiftKeyDown()) {
                table.processSource(player);
            } else {
                var held = player.getItemInHand(hand);
                if (!table.canPlaceItem(slot, held) || slot == TypesettingTableBlockEntity.SOURCE
                        && !table.getItem(slot).isEmpty() || !insertStack(table, slot, held)) {
                    ejectStack(level, Vec3.atBottomCenterOf(pos.above()).add(0, 0.06, 0), table, slot);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static int slotAt(BlockState state, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) {
            return -1;
        }
        Vec3 point = northHit(state, hit);
        if (point.x >= 0.59) {
            return point.z > 0.5 ? TypesettingTableBlockEntity.SOURCE : TypesettingTableBlockEntity.RESULT;
        }
        return point.x <= 0.43 && point.z > 0.3 && point.z < 0.7 ? TypesettingTableBlockEntity.CHASES : -1;
    }

    static Vec3 northHit(BlockState state, BlockHitResult hit) {
        var point = hit.getLocation().subtract(Vec3.atLowerCornerOf(hit.getBlockPos()));
        return switch (state.getValue(FACING)) {
            case EAST -> new Vec3(point.z, point.y, 1 - point.x);
            case SOUTH -> new Vec3(1 - point.x, point.y, 1 - point.z);
            case WEST -> new Vec3(1 - point.z, point.y, point.x);
            default -> point;
        };
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.TYPESETTING_TABLE.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> ((TypesettingTableBlockEntity) entity).tick(server);
    }
}
