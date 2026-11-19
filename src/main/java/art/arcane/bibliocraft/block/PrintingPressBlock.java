package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.PrintingPressBlockEntity;
import art.arcane.bibliocraft.config.BiblioConfig;
import art.arcane.bibliocraft.menu.PrintingPressMenu;
import art.arcane.bibliocraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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

public final class PrintingPressBlock extends InventoryFurnitureBlock implements DrillAdjustable {
    public PrintingPressBlock(Properties properties) {
        super(properties, Shapes.or(
                Shapes.box(0.03, 0, 0, 1, 0.55, 1),
                Shapes.box(0.03, 0.55, 0.32, 0.30, 1.13, 0.68),
                Shapes.box(0.64, 0.55, 0.02, 1, 1.0, 0.98)),
                "printing_press");
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getClockWise());
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !BiblioConfig.COMMON.enablePrintpressTypeMachine.get()
                || !(level.getBlockEntity(pos) instanceof PrintingPressBlockEntity press)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            int slot = slotAt(state, hit);
            if (slot < 0) {
                player.openMenu(new SimpleMenuProvider((id, inventory, visitor) -> new PrintingPressMenu(id, inventory, press),
                        Component.translatable("block.bibliocraft.printing_press")));
            } else {
                var held = player.getItemInHand(hand);
                if (press.canPlaceItem(slot, held)) {
                    if (slot != PrintingPressBlockEntity.PLATE || press.getItem(slot).isEmpty()) {
                        insertStack(press, slot, held);
                    }
                } else {
                    ejectStack(level, Vec3.atBottomCenterOf(pos.above()).add(0, 0.17, 0), press, slot);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static int slotAt(BlockState state, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) {
            return -1;
        }
        Vec3 point = TypesettingTableBlock.northHit(state, hit);
        if (point.x >= 0.67) {
            return point.z < 0.5 ? PrintingPressBlockEntity.RESULT : PrintingPressBlockEntity.BLANK;
        }
        return point.x < 0.30 ? PrintingPressBlockEntity.INK : PrintingPressBlockEntity.PLATE;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                          BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel server) || type != ModBlockEntities.PRINTING_PRESS.get()) {
            return null;
        }
        return (world, pos, currentState, entity) -> ((PrintingPressBlockEntity) entity).tick(server);
    }
}
