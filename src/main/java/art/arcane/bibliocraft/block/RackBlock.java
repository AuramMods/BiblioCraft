package art.arcane.bibliocraft.block;

import art.arcane.bibliocraft.blockentity.RackBlockEntity;
import art.arcane.bibliocraft.config.BiblioFeatureToggles;
import art.arcane.bibliocraft.menu.RackMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;

public class RackBlock extends ShiftableFurnitureBlock {
    private final RackType type;

    public RackBlock(Properties properties, VoxelShape shape, String id, RackType type) {
        super(properties, shape, id, type == RackType.POTIONS ? -0.25 : 0.25);
        this.type = type;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction opening = context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(FACING, type.facingForOpening(opening));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction opening = type.opening(state.getValue(FACING));
        return state.setValue(FACING, type.facingForOpening(mirror.mirror(opening)));
    }

    @Override
    protected InteractionResult useFurniture(BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND
                || !BiblioFeatureToggles.isBlockEnabled(ForgeRegistries.BLOCKS.getKey(this))
                || !(level.getBlockEntity(pos) instanceof RackBlockEntity rack)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && !interactWithSlot(level, pos, state, player, hit, rack)) {
            player.openMenu(new SimpleMenuProvider(
                    (id, inventory, viewer) -> new RackMenu(id, inventory, rack), getName()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean interactWithSlot(Level level, BlockPos pos, BlockState state, Player player,
                                     BlockHitResult hit, RackBlockEntity rack) {
        int slot = readingSlot(state, hit);
        boolean sneaking = player.isShiftKeyDown();
        if (slot < 0 || type == RackType.TOOLS && sneaking) {
            return false;
        }
        Direction side = hit.getDirection();
        var held = player.getMainHandItem();
        if (type == RackType.TOOLS) {
            if (!rack.canPlaceItem(slot, held) || !insertStack(rack, slot, held)) {
                ejectShiftedStack(state, level, pos, side, rack, slot);
            }
            return true;
        }
        return (sneaking && ejectShiftedStack(state, level, pos, side, rack, slot))
                || (rack.canPlaceItem(slot, held) && insertStack(rack, slot, held));
    }

    int readingSlot(BlockState state, BlockHitResult hit) {
        Direction opening = type.opening(state.getValue(FACING));
        Direction side = hit.getDirection();
        if (side == opening || type != RackType.TOOLS && side == opening.getOpposite()) {
            return slotAt(hit.getBlockPos(), hit.getLocation(), opening);
        }
        return -1;
    }

    private int slotAt(BlockPos pos, Vec3 hit, Direction opening) {
        Direction right = opening.getCounterClockWise();
        double offset = (hit.x - pos.getX() - 0.5) * right.getStepX()
                + (hit.z - pos.getZ() - 0.5) * right.getStepZ();
        int column = Mth.clamp(Mth.floor((offset + 0.5) * type.columns()), 0, type.columns() - 1);
        int rowFromBottom = Mth.clamp(Mth.floor((hit.y - pos.getY()) * type.rows()), 0, type.rows() - 1);
        return (type.rows() - 1 - rowFromBottom) * type.columns() + column;
    }
}
